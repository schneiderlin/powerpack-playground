:page/title "强类型 vs 动态类型：AI 生成代码的思考"
:page/description "强类型语言的编译器能做检查，是否意味着 AI 生成代码更有优势？本文探讨了类型表达力、类型体操成本、编译期 vs 运行期验证，以及动态类型语言在 AI 生成场景下的灵活优势。"
:page/date "2026-01-31"
:blog-post/tags [:type-system :AI :clojure :rust :haskell]
:blog-post/author {:person/id :jan}
:page/body

## 直觉的陷阱

直觉上，强类型语言（Rust、Scala、Haskell）在 AI 生成代码场景下似乎更有优势：

- 编译器能做严格检查
- 类型系统是天然的契约
- 错误在编译期就能发现

但仔细想想，这个直觉可能是错的。

## 类型表达力的限制

强类型语言的类型系统虽然强大，但表达力是有边界的。

### 常见的约束无法用类型表达

很多实际的约束，类型系统表达不出来或者表达起来很困难：

- **字符串格式**：email、phone number、正则匹配
- **数值范围**：年龄必须 ≥ 0，评分必须在 0-100 之间
- **业务规则**：用户余额不能小于转账金额，订单状态转换必须符合工作流
- **跨字段约束**：开始时间必须早于结束时间
- **外部依赖**：数据库 schema 变化、API 响应格式

这些约束如果用类型系统表达，要么做不到，要么要做大量类型体操。

### 类型体操的成本

为了让类型系统表达更复杂的约束，你会陷入 type gymnastics：

```rust
// Rust: 试图用类型表达"字符串必须是非空 email"
struct NonEmptyString(String);

struct Email(String);

impl TryFrom<NonEmptyString> for Email {
    type Error = &'static str;

    fn try_from(s: NonEmptyString) -> Result<Self, Self::Error> {
        // 这里面还是要写验证逻辑
        if is_valid_email(&s.0) {
            Ok(Email(s.0))
        } else {
            Err("invalid email")
        }
    }
}
```

问题是：
- 每个约束都要写一堆 wrapper
- 类型系统只是"外壳"，验证逻辑还是要手写
- 类型层级越深，代码越复杂
- 读懂类型比读懂业务逻辑还难

这就是 "type as theorem, program as proof" 的代价——你需要把所有约束都"证明"给类型系统看，但它并不关心你的业务逻辑，只关心类型推导。

## Clojure 的灵活验证

Clojure 原生是动态类型，但这不代表没有验证机制。相反，它的验证生态更灵活。

### 多种验证方式

| 验证方式 | 特点 | 使用场景 |
|---------|------|---------|
| **spec** | 标准库，用于数据规范和数据生成 | 数据契约、函数契约 |
| **Malli** | 灵活的 schema 库，支持多种表达方式 | API schema、运行时校验、clj-kondo 类型导出 |
| **schema** | 基于 Clojure 的 schema 库 | 数据建模 |
| **core.typed** | 可选的静态类型系统 | 需要渐进式类型时 |
| **自定义 validators** | 纯函数，想怎么写就怎么写 | 复杂业务规则 |

### 编译期 + 运行期

Clojure 的验证不局限于编译期，可以在多个阶段介入：

```clojure
;; 1. 定义 schema
(def user-schema
  [:map
   [:id :int]
   [:name [:string {:min 1}]]
   [:email [:re email-regex]]
   [:age [:int {:min 0 :max 150}]]
   [:status [:enum :active :inactive :pending]]])

 ;; 2. 编译期：导出给 clj-kondo
 (malli.dev/export-types!)  ;; 生成 clj-kondo config（详见 [malli-cljkondo-type-export](./malli-cljkondo-type-export.md)）

;; 3. 运行期：动态验证
(m/validate user-schema input)  ;; 返回 true/false 或 详细错误

;; 4. 运行期：instrument 函数
(malli.dev/instrument! `user-function)

;; 5. 测试期：用 schema 生成测试数据
(gen/generate (m/schema user-schema))
```

### 渐进式验证

你可以根据需求选择验证的严格程度：

```clojure
;; 轻量级：只是描述
(def simple-schema
  [:map [:name :string] [:age :int]])

;; 中等：加一些约束
(def medium-schema
  [:map
   [:name [:string {:min 1}]]
   [:age [:int {:min 0 :max 150}]]])

;; 严格：复杂的业务规则
(def strict-schema
  [:map
   [:name [:string {:min 1} {:max 100}]]
   [:age [:int {:min 0 :max 150} {:error-message "age must be between 0 and 150"}]]
   [:email [:re email-regex {:error/message "invalid email format"}]]
   [:status [:enum {:error/message "invalid status"} :active :inactive :pending]]
   [:created-at inst?]
   [:updated-at [:and inst? [:fn {:error/message "updated_at must be after created_at"}
                                #(t/after? % (get % "created_at"))]]]])
```

## 不需要"证明"一切

在强类型语言中，为了通过编译，你必须"证明"所有约束。

在 Clojure 中，你可以选择性地验证：

```clojure
;; 写代码时，不验证
(defn create-user [name age email]
  {:name name :age age :email email})

;; 需要时，加上验证
(defn create-user-validated [name age email]
  (let [user {:name name :age age :email email}]
    (if (m/validate user-schema user)
      user
      (throw (ex-info "invalid user" (m/explain user-schema user))))))

;; 或者用 instrument 自动验证
(malli.dev/instrument! `create-user-validated)
```

这种灵活性在 AI 生成代码场景下特别有价值：

### 为什么灵活验证更适合 AI 生成

1. **AI 生成的代码往往不完美**

   强类型语言中，如果 AI 生成的代码类型不匹配，编译不过。你需要不断调整 prompt 直到类型正确。

   Clojure 中，AI 生成基础代码后，你可以：
   - 先跑起来看效果
   - 然后逐步加上验证
   - 或者用 instrument 自动发现问题

2. **渐进式增强更符合开发流程**

   真实的开发流程是：
   - 快速原型（不管类型）
   - 加上基础验证
   - 根据实际需求加强验证

   强类型语言要求一开始就"证明"一切，这和快速迭代矛盾。

3. **运行期验证更实用**

   很多约束在编译期无法验证：
   - 数据库查询结果
   - API 响应
   - 用户输入
   - 外部系统依赖

   Clojure 的运行期验证能覆盖这些场景，而强类型语言的类型系统做不到。

4. **错误信息更友好**

   ```clojure
   ;; Clojure Malli 的错误信息
   (m/explain strict-schema {:name "" :age 200 :email "invalid"})
   ;; => [{:path [:name], :in ["name"], :message "string length must be at least 1"}
   ;;     {:path [:age], :in ["age"], :message "value must be less than or equal to 150"}
   ;;     {:path [:email], :in ["email"], :message "invalid email format"}]
   ```

   对比强类型语言的类型错误：
   ```rust
   error[E0308]: mismatched types
    --> src/main.rs:5:20
     |
   5 |     let user = User { name: "", age: 200, email: "invalid" };
     |                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expected `Email`, found `&str`
   ```

    Malli 的错误信息更具体，更容易修复。

## 关键是表达力的权衡

真正的关键不是"静态 vs 动态"，而是：

- **你想把多少约束放进类型系统？**
- **类型体操的成本 vs 验证灵活性的收益？**

强类型语言的选择是：把尽可能多的约束放进类型系统，但代价是复杂的类型定义和证明。

动态类型语言（尤其是 Clojure）的选择是：保持类型系统简单，用灵活的验证机制覆盖更多约束场景。

在 AI 生成代码的场景下，后者可能更实用：
- AI 生成的代码往往不完美
- 快速迭代比完美类型更重要
- 运行期验证能覆盖更多场景
- 渐进式增强更符合开发流程

毕竟，AI 生成代码的目标是快速得到可用的结果，而不是一开始就写出"完美证明"的代码。
