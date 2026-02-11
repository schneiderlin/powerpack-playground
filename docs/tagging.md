博客不能随便乱打 tag. 每次创建新的 tag 都需要用户确认. 目前已经确认过可以打的 tag 在
powerblog.core.clj *allowed-blog-tags* 变量里面. 注释里面会写 tag 的含义.

clj -X:build 会检查是否用了 allowed-blog-tags 以外的 tag. 可以运行这个命令找出并修复