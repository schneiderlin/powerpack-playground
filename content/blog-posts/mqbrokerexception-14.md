:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: "RocketMQ MQBrokerException: CODE: 14 问题排查"
date: "2023-02-08"
description: A detailed investigation of the MQBrokerException code 14 issue in RocketMQ, discussing the causes and solutions.
lang: "zh"
tags: ["rocketmq"]
---

在本地启动 RocketMQ, 用 Producer 推消息的时候发现消息发送失败, 提示磁盘满失败
```
org.apache.rocketmq.client.exception.MQClientException: Send [3] times, still failed, cost [2148]ms, Topic: TopicTest, BrokersSent: [linzihaos-MacBook-Pro.local, linzihaos-MacBook-Pro.local, linzihaos-MacBook-Pro.local]
See http://rocketmq.apache.org/docs/faq/ for further details.
	at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.sendDefaultImpl(DefaultMQProducerImpl.java:715)
	at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1426)
	at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1369)
	at org.apache.rocketmq.client.producer.DefaultMQProducer.send(DefaultMQProducer.java:351)
	at org.apache.rocketmq.example.quickstart.Producer.main(Producer.java:79)
Caused by: org.apache.rocketmq.client.exception.MQBrokerException: CODE: 14  DESC: service not available now. It may be caused by one of the following reasons: the broker's disk is full [CL:  0.90 CQ:  0.90 INDEX:  0.90], messages are put to the slave, message store has been shut down, etc. BROKER: 10.10.1.118:10911
```

看源码发现 `DefaultMessageStore` 里面有个定时任务, `CleanCommitLogService`, 会周期性的检查硬盘状态并做清除操作

```java
 private void addScheduleTask() {

        this.scheduledExecutorService.scheduleAtFixedRate(new AbstractBrokerRunnable(this.getBrokerIdentity()) {
            @Override
            public void run2() {
                DefaultMessageStore.this.cleanFilesPeriodically();
            }
        }, 1000 * 60, this.messageStoreConfig.getCleanResourceInterval(), TimeUnit.MILLISECONDS);

        ... // 其他定时器
    }
```

跟随 `cleanFilesPeriodically` 的调用链, 调用了 `deleteExpiredFiles`, 然后 `isSpaceToDelete`. 在这个方法里面, 检查了实际硬盘容量 `minPhysicRatio`, 和配置的 warning level `getDiskSpaceWarningLevelRatio`. 如果实际使用量已经超过预设的比例, 就会把 runningFlags 设置为磁盘已满的状态.  
在 `HookUtils` `checkBeforePutMessage` 方法中, 会先检查 runningFlags 的状态.  
默认的比例是 0.9, 只要磁盘已用超过 90% 就无法再推送消息. 清理磁盘后就可以正常推送消息了.