:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: "RocketMQ 5.0 解锁任意时间延迟消息能力"
date: "2023-02-23"
tags: ["rocketmq"]
description: An exploration of the new delay message feature in RocketMQ 5.0, discussing its implementation and how to set the precision parameter for optimal performance.
lang: "zh"
---

过去，阿里云商用的 RocketMQ 支持任意时间延迟消息, 但是开源 4.x 版本的不支持. 只有默认 18 个 level 的延迟消息, 分别是 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h。然而，最新版本5.0中，RocketMQ引入了任意时间延迟消息的灵活性。它的实现是基于时间轮算法，并具有可调整的精度参数。在本文中，我们将深入探讨新的延迟消息功能，探索其背后的原理，并讨论如何为获得最佳效果，正确设置精度参数。

## 4.x 版本的延迟消息
RocketMQ 4.x 版本的延迟消息是通过简单的方式实现的。如果发现消息是延迟的，则会记录原始的主题，然后将消息的主题修改为 SCHEDULE_TOPIC_XXXX，并投递到延迟队列中。Broker具有定时器，将定期消费延迟队列中的消息，到期的消息将重新投递到原始主题中。

## 5.0 版本的延迟消息
采用了时间轮算法. 时间轮是一种算法, 用来管理和调度固定时间间隔内的事件。它将固定时间间隔分成若干个时间槽，并将事件映射到相应的时间槽上。在时间轮中，每个时间槽都会按顺序处理它所映射的事件，从而实现对固定时间间隔内的事件的调度.

关于时间轮算法的详细介绍可以看 <https://juejin.cn/post/7083795682313633822>

RocketMQ 中的时间轮数据是通过 mmap 技术持久化到本地文件中的。这种方法可以保证时间轮数据的完整性和高效存储。在系统重启或崩溃后，数据可以通过 mmap 快速加载到内存中，从而继续执行下一步操作。

默认时间轮的精度是 1000 毫秒，也就是一秒，时间轮的槽的数量为 TIMER_WHEEL_TTL_DAY（7）乘 DAY_SECS（24 * 3600）。因此，使用默认精度，时间轮最多可以存储 7 天的延迟消息。

如果延迟消息超过 7 天怎么处理? 将消息放入时间轮的关键逻辑在源码 TimerMessageStore 类的 doEnqueue 方法中.

该方法主要执行以下步骤：
-   先对消息的到期时间进行判断，如果消息到期时间与当前时间差值超过了时间轮定义的翻滚窗口（timerRollWindowSlots）的时间长度，则需要将该消息放入下一个时间轮；
-   然后通过时间轮的 getSlot 方法，根据消息的到期时间，确定该消息应该放入的 slot；
-   使用 ByteBuffer 对象 timerLogBuffer 记录该消息的一些信息，包括偏移量，大小等；
-   通过 timerLog.append 方法将消息的信息写入日志；
-   最后，通过 timerWheel.putSlot 将该消息放入时间轮的对应 slot 中。

```java
public boolean doEnqueue(long offsetPy, int sizePy, long delayedTime, MessageExt messageExt) {
    LOGGER.debug("Do enqueue [{}] [{}]", new Timestamp(delayedTime), messageExt);
    //copy the value first, avoid concurrent problem
    long tmpWriteTimeMs = currWriteTimeMs;
    boolean needRoll = delayedTime - tmpWriteTimeMs >= timerRollWindowSlots * precisionMs;
    int magic = MAGIC_DEFAULT;
    if (needRoll) {
        magic = magic | MAGIC_ROLL;
        if (delayedTime - tmpWriteTimeMs - timerRollWindowSlots * precisionMs < timerRollWindowSlots / 3 * precisionMs) {
            //give enough time to next roll
            delayedTime = tmpWriteTimeMs + (timerRollWindowSlots / 2) * precisionMs;
        } else {
            delayedTime = tmpWriteTimeMs + timerRollWindowSlots * precisionMs;
        }
    }
    boolean isDelete = messageExt.getProperty(TIMER_DELETE_UNIQKEY) != null;
    if (isDelete) {
        magic = magic | MAGIC_DELETE;
    }
    String realTopic = messageExt.getProperty(MessageConst.PROPERTY_REAL_TOPIC);
    Slot slot = timerWheel.getSlot(delayedTime);
    ByteBuffer tmpBuffer = timerLogBuffer;
    tmpBuffer.clear();
    tmpBuffer.putInt(TimerLog.UNIT_SIZE); //size
    tmpBuffer.putLong(slot.lastPos); //prev pos
    tmpBuffer.putInt(magic); //magic
    tmpBuffer.putLong(tmpWriteTimeMs); //currWriteTime
    tmpBuffer.putInt((int) (delayedTime - tmpWriteTimeMs)); //delayTime
    tmpBuffer.putLong(offsetPy); //offset
    tmpBuffer.putInt(sizePy); //size
    tmpBuffer.putInt(hashTopicForMetrics(realTopic)); //hashcode of real topic
    tmpBuffer.putLong(0); //reserved value, just set to 0 now
    long ret = timerLog.append(tmpBuffer.array(), 0, TimerLog.UNIT_SIZE);
    if (-1 != ret) {
        // If it's a delete message, then slot's total num -1
        // TODO: check if the delete msg is in the same slot with "the msg to be deleted".
        timerWheel.putSlot(delayedTime, slot.firstPos == -1 ? ret : slot.firstPos, ret,
            isDelete ? slot.num - 1 : slot.num + 1, slot.magic);
        addMetric(messageExt, isDelete ? -1 : 1);
    }
    return -1 != ret;
}
```

可以看到超过了 7 天, 就会触发翻滚窗口, 减少消息的延迟时间, 重新投递到队列中, 等下一次从队列中拿出时, 重复以上的逻辑. 可能会经过多轮的翻滚, 最终会少于 7 天.

根据应用场景对精度的要求, 还有是否接受多次翻滚时间轮导致的重复投递损耗, 来决定 precisionMs 参数的选择. 一般来说, 对于对时间精度要求较高的场景, 可以选择更小的 precisionMs 参数, 这样可以避免重复投递的损耗, 但会消耗更多的系统资源. 如果不在意重复投递的损耗, 可以选择更大的 precisionMs 参数, 这样可以减小系统资源的消耗.