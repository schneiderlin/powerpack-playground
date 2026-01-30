:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: "Profunctor"
date: "2018-12-05"
tags: ["functional programming", "scala"]
auther: linzihao
description: An introduction to profunctors in functional programming, explaining their definition, relationship to bifunctors, and implementation in Scala. This post covers the basic structure of profunctors, their laws, and provides examples of profunctor instances like function types.
lang: "zh"
---

Produnctor是𝐂𝑜𝑝 × 𝐃 → 𝐒𝐞𝐭，类似bifunctor也是从一个product of category到category的mapping。
不过product里面的第一个category变成了Cop,相当于一个contravariant functor和一个covariant functor的组合。
```
trait Profunctor[F[_, _]] {
  def bimap[A, B, C, D]: (A => B) => (C => D) => F[B, C] => F[A, D] =
    f => g => lmap(f) compose rmap[B, C, D](g)

  def lmap[A, B, C]: (A => B) => F[B, C] => F[A, C] =
    f => bimap(f)(identity[C])

  def rmap[A, B, C]: (B => C) => F[A, B] => F[A, C] =
    bimap[A, A, B, C](identity[A])
}
```

function type就是profunctor的一个instance
一开始是B => C的函数，指定return type如何map(C => D)和argument type如何map(A => B)，然后就能得到一个A => D的函数。

profunctor在len的时候用到，并且跟end和co-end有关系