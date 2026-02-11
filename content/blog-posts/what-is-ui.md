:page/title 可复用的 UI 代码
:page/description "讨论如何通过引入 IR 中间表示层组织 UI 代码，实现业务逻辑可测试、渲染后端可替换、布局算法可复用，适用于 DOM、Mobile、游戏引擎等多个平台。"
:blog-post/tags [:design :ui :software-engineering]
:blog-post/author {:person/id :jan}
:page/body


本文讨论如何组织 UI 代码, 使其更易维护. 不局限于某个具体的 UI 库或框架, 而是探讨适用于 DOM、Mobile、游戏引擎等不同平台的核心思想.

## Glossary

- **State Data** - 纯业务数据, 与渲染平台无关. 例如表格数据可以是 list of rows、matrix、pandas dataframe 等任何编程语言的数据结构, 筛选条件、分页参数等. 关键是它不是 DOM 的 tr td、Widget 等与渲染平台耦合的结构

- **Sensory Data** - 用户输入数据, 抽象层次取决于 render backend 本身在哪个层次. 例如浏览器的 click event（带 target 元素信息）或游戏引擎的原始 x, y 坐标

- **IR (Intermediate Representation)** - 中间表示层, 是双向的纯数据结构. 用于解耦业务逻辑与渲染平台：
  - **渲染方向**: State data → IR → Computer output
  - **输入方向**: Sensory data + IR → User intent
  IR 包含了视图元素的结构、布局信息和业务数据

- **Computer Output** - 渲染输出, 例如浏览器页面、移动端界面、游戏画面等

- **User Intent** - 用户通过输入表达的真实意图, 例如"删除此行"、"翻到下一页". 需要结合 sensory data 和 IR 才能推断出

- **Layout** - 将 high level 的视图元素关系描述(如 flex 布局)转换为 low level 的绝对坐标(x, y, width, height)的纯函数转换, 算法本身可在不同平台间复用

什么是 UI. 处理人和计算机交互, 有两条路线, 人 -> 计算机, 计算机 -> 人.

计算机 -> 人的路线.  
从 data 到 感观(视觉, 听觉等). 

人 -> 计算机的路线.  
sensory data(鼠标, 键盘, 触控等)结合 IR 映射到 user intent.

## 计算机 -> 人
先细分 计算机 -> 人 的路线.
state data -> platform-specific representation -> computer output

其中 state data 是纯业务数据, 例如用户表格的数据可以是 list of rows、matrix 等任何数据结构.
platform-specific representation 是渲染平台特有的中间数据结构, 往往和具体平台耦合. 例如 HTML DOM tree 与浏览器耦合, 必须有浏览器才能渲染出页面. 游戏引擎的 SceneTree 与相应的游戏引擎耦合. Flutter 的 Widget tree 与 Flutter 耦合.

能不能把 platform-specific representation 和如何渲染到 output 解耦?
可以通过引入中间层(intermedia representation)的方式.
```
state data -> IR -> DOM tree (browser)
                  -> Widget tree (Flutter)
                  -> ... (other platforms)
```

需要自己根据业务情况建模, IR 表示什么数据.

引入 IR 解耦的好处是什么?  

**好处一：业务逻辑可测试性**
大量的 UI 业务逻辑在 state data -> IR 之间的转换. 并且 IR 只是普通的数据(原子数据例如 string, number, 复合数据例如 Map, List, Set, 或者他们的嵌套). 那么这些业务逻辑很容易测试.

例如 state data 包含了
- 表格数据
- 筛选, 排序条件
- 分页参数

IR 只包含某一页符合条件排序好的数据, 只是一个 List of rows. 测试时很容易 assert IR 是否符合条件. 注意 IR 可以根据业务需要设计包含不同程度的信息, 上面的例子只包含数据, 不包含布局信息.

**好处二：渲染后端可替换性**
如果 IR 包含了关于渲染的足够信息, 可以替换不同的 render backend.
```
IR -> DOM tree (browser)
   -> Widget tree (Flutter)
   -> ... (other platforms)
```
这些胶水代码可以随意增加, 替换, 都不影响业务逻辑. 这里的可替换性指的是 IR 到具体渲染平台的胶水代码可以替换, 而业务逻辑(state data -> IR)保持不变.

**好处三：布局算法可复用性**  
IR 里面一般也包含层级关系, 和 parent child sibling 之间应该怎么摆放的描述.
layout 指的是把这些 high level 的描述, 转换成简单的 x, y, width, height 之类的, 使得 render backend 可以直接使用.
例如
```
[:parent {:flexDirection "row"
          :width 100}
 [:child1 {:flexGrow 1}]
 [:child2 {:flexGrow 2}]]
```
转换成
```
[:parent {:flexDirection "row"
          :width 100}
 [:child1 {:width 33}]
 [:child2 {:width 66}]]
```
layout 算法不过是 data -> data 的纯函数转换, 完全可以在不同的平台之间复用. 这里的可复用性指的是 layout 算法本身可以在不同平台之间复用, 算法与具体渲染后端解耦. 可以自己设计不同的 high level 语言描述视图元素之间关系, 转换到 low level 的 x y width height 几乎全部 render backend 都支持, 也可以转译到例如 css 之类的其他描述语言利用已有生态.


## 人 -> 计算机
TODO
