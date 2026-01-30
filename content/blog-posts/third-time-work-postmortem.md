:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: ThirdTime 工作法使用一个月后的心得体会
date: "2024-07-10"
description: A reflection on the experience of using the ThirdTime work method for one month, discussing the challenges and achievements, and providing insights into the work method.
tags: ["time management"]
lang: "zh"
---

ThirdTime 工作法可以看作是番茄工作法的升级版, 之前在 [bilibili](https://www.bilibili.com/opus/935450195019169830) 上发过.
简单来说就是以下的规则
- 每次工作完, 不管工作多长时间, 可以休息工作时间的 1/3
- 如果没有休完, 可以留到下一次休
- 休息时间不能预支
- 午饭和晚饭的时候可以 "big break", 时间任意, 只需要提前设置好时间就可以
- 休息时间延后使用不能跨 "big break" 和跨天

也用 clojure 写了一个简单的程序, 用来记录时间的使用情况 [github](https://github.com/schneiderlin/babashka-scripts/tree/master/time).

使用了大约两周后, 没有继续使用了. 因为我发现我不需要休息时间也能一直高效工作. 工作很容易进入心流状态, 往往做完一件事情, 从 "zone" 里面出来的时候, 已经过了很长时间了, 
有大把的休息时间可以用, 没用完. 

当然这个高效工作是主观感受, 实际上是否真的效率更高, 是很难评估的, 因为做的不是重复性工作, 产出没法相互比对.
[scott young 的博客](https://www.scotthyoung.com/blog/2023/03/07/sustainable-productivity/) 里面也有提到 "feeling productive" 和 "being productive" 的区别.

从 output 的角度看, 最近一段时间的产出变多了, 但更多应该不是 ThirdTime 工作法的因素, 因为前两周有用, 最近两周没用.
对于我来说, 可能提高效率更重要的因素是没有人干扰, 能自由选择时间地点工作, 在想出去玩的时候就出去玩. 和在办公室坐班相比, 减少了很多内耗.