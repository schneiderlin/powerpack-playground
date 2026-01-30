:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: Many Java Codebases Are a Mess
author: linzihao
date: "2024-09-14"
lang: "en"
tags: ["Java"]
description: "Me rant about Java and Java devs"
---
I have seen many examples of how a Java codebase turns into a mess.
Every developer talks about decoupling, testability, and ease of refactoring. Yet, they use many features of Spring to turn their codebase into brittle spaghetti.

When you just want to call a method:

A method is inside a class, so to call a method, you first need to create an instance of the class.
That class is usually not easy to create, often doesn't provide a constructor, or its creation needs to go through a complex process.
They even invented a design pattern for this, called the `factory pattern`.

Now you have a class and call a method. BOOM! Inside that method, somewhere in the call stack, it requires some 'Bean' to be present in the 'SpringContext'.

## Hiding the Wrong Things
Abstraction is necessary, but it should only hide unimportant details, not the wrong things.

For example, can I trace what happens when a web request comes in?
Where should I put the breakpoint or log? It's just some annotations. Where can I check what the request looks like?
```java
@RestController("api")
public class XXController {
    @GetMapping("test")
    public void test() {
        System.out.println("test");
    }
}
```
It does too much magic. I can only go to `RestController` or `RequestMapping` definition, but it ends there.
It requires deep knowledge of the framework to continue reasoning.

It also hides how the request turns into custom object parameters in the controller method.
```java
@GetMapping("test")
public void test(SomeObject o) {
    System.out.println("test");
}
```
How does this SomeObject get created? Can I change the deserialization logic?
If some field is missing, how do I debug? Again, the function `request -> SomeObject` is not defined by the developer; it only exposes some configuration or annotations.

Not to mention, the return value, authentication, and authorization are also hidden by some "convention" spring-boot-starter library.
Some even use `ThreadLocal` to store context for later use, but it's not something developers can easily configure.
When and where the handler function gets called is also not clear.

Let's give another example: where do you put breakpoints in various ORM libraries to inspect the generated SQL without executing the database?
Some provide log options, but I'm not talking about that. I mean truly controlling that piece of logic, for example, adding some custom checks to the SQL or adding filters to forbid some SQL from executing.
Good luck finding cues.

## Tangled Input
Autowiring also introduces implicit input.
Input is everything that will affect the behavior of a method.
Method parameters are input. Using variables from outside of the method's scope, those variables are input. If the method depends on date and time, then time is also input. If the method gets values from ThreadLocal, then the running thread is also input.

Autowiring creates the second kind of input: variables from outside of the method's scope.
Anything that is input but not a parameter is implicit input.
It prevents local reasoning about a method.

It gives up a tremendously valuable property, making refactoring very hard.
Java developers, when refactoring, constantly need to think about the impact of changes, just because it's not local.
Even if local behavior is correct and tested, they need to always keep an eye out for effects on other parts of the system in some strange way.

## Conclusion
Think about the first principle: does it really make code easier to reason about and change?
Don't use fancy features or "design patterns" just because you think they're cool or because it's what others are doing.
50% of developers are below average BTW.