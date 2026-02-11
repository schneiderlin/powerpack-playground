:page/title database instead of file system
:page/description "延续 static site generator 的思路，讨论 render 基于数据库而非文件系统的优点：更好的查询能力、无需 ship db、数据由 ingest 生成故无同步问题，可类比 IDE 与 hledger 的架构。"
:blog-post/tags [:clojure :software-engineering]
:blog-post/author {:person/id :jan}
:page/body

在 [static blog generator](/blog-posts/static-site-generator/) 里面引出的一个 idea, render 基于 database 比基于 file system 有一些优点. 其实严格来说, 用的是 fs 和 db 共存的方式, 但是因为不维护数据库的数据 (所有数据都是在系统启动的时候, ingest fs 中的 content 得到的 entry), 所以不需要担心 db 和 fs out of sync.  
db 提供了更好的 query, 并且最终 ship 成品的时候不需要 ship db 一起.   
各种 IDE 也可以类比成 fs 和 db, fs 里面放的是代码文本, db 可以看成是 LSP server 之类的, 会对代码文本做 parse, 然后维护他们的结构. dev time 的时候提供了很多的辅助功能. 但是最终 ship 出去的时候不需要 IDE.  

hledger 也可以类比, source of truth 是文本的 ledger. db 是在这个基础上算的各种聚合, 报表之类的东西.  