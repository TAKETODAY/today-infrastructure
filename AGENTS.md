# TODAY Infrastructure 项目开发指南

本文档为 AI 编码助手（如 opencode）提供在 TODAY Infrastructure 代码库中工作的必要信息。包含构建命令、测试命令、代码风格指南和开发约定。

## 项目概述

TODAY Infrastructure 是一个 Java 应用程序基础设施库，基于 Spring Framework 设计思想，采用模块化架构。项目使用 Gradle 构建，支持 Java 17+，包含核心容器、AOP、Web
MVC、数据访问等模块。

## 构建命令

```bash
# 完整构建（编译、测试、检查）
./gradlew build

# 仅编译（跳过测试）
./gradlew assemble

# 清理构建产物
./gradlew clean

# 运行所有检查（编译、测试、代码风格、静态分析） , 慎用（项目很大一般执行需要半小时）
./gradlew check

# 构建特定模块（例如 infra-core）
./gradlew :infra-core:build

# 安装到本地 Maven 仓库
./gradlew publishToMavenLocal
```

## 测试命令

```bash
# 运行所有测试 , 慎用（项目很大一般执行需要半小时）
./gradlew test

# 运行特定模块的测试
./gradlew :infra-core:test

# 运行单个测试类
./gradlew :infra-core:test --tests "*ClassName"

# 运行单个测试方法
./gradlew :infra-core:test --tests "*ClassName.methodName"

# 运行集成测试 , 慎用（项目很大一般执行需要半小时）
./gradlew intTest

# 运行特定 Java 版本的测试 , 慎用（项目很大一般执行需要半小时）
./gradlew java21Test
./gradlew java24Test

# 重新运行失败的测试
./gradlew test --rerun-tasks

# 跳过测试
./gradlew build -x test

# 生成测试覆盖率报告 , 慎用（项目很大一般执行需要半小时）
./gradlew testCodeCoverageReport
```

### 在 IDE 中运行测试

- 使用 `.run/` 目录下的预配置运行配置
- 在 IntelliJ IDEA 中可以直接运行 `:module:check` 任务

## 代码风格指南

### 格式化规范

- **缩进**：2个空格（非制表符）
- **行尾**：LF（Unix 风格）
- **字符编码**：UTF-8
- **行长度**：建议不超过 120 个字符（未强制，但应保持可读性）

配置文件：`.editorconfig`

### 导入规范

- **禁止通配符导入**：不允许 `import java.util.*;`
- **移除未使用的导入**：使用 Checkstyle 自动检查
- **导入顺序**：未严格规定，但应保持一致性
- **禁止的导入**：
    - JUnit 3/4/5 的断言类（应使用 AssertJ）
    - Hamcrest 断言（应使用 AssertJ）
    - TestNG 相关类
    - JetBrains 空值注解（应使用 JSpecify）
    - 其他第三方空值注解（应使用 JSpecify）

### 命名约定

- **类名**：大驼峰式，不以 "Test" 结尾（测试类使用 "Tests" 后缀）
- **方法名**：小驼峰式
- **常量**：全大写，下划线分隔
- **包名**：全小写，使用 `infra` 作为根包

### 类型与空值安全

- **空值注解**：使用 `org.jspecify.annotations` 包中的注解
    - `@Nullable`：表示可能为 null
    - `@NonNull`：表示不为 null
- **编译时检查**：启用 NullAway 进行空值安全验证
- **避免原始类型**：优先使用泛型

### 错误处理

- **异常类**：应以 "Exception" 结尾，不可变（final 字段）
- **异常消息**：提供有意义的错误信息
- **异常捕获**：避免空的 catch 块，至少记录日志
- **自定义异常**：继承 `RuntimeException` 或适当的检查异常

### 测试规范

- **测试类命名**：使用 "Tests" 后缀（非 "Test"）
- **断言库**：使用 AssertJ（禁止使用 JUnit/Jupiter/TestNG 断言）
- **Mock 框架**：使用 Mockito，优先使用 BDDMockito 风格
- **测试方法**：应具有描述性名称，使用小驼峰式
- **测试结构**：推荐 Given-When-Then 模式

### 禁止的模式

- **字符串大小写转换**：必须指定 Locale（例如 `str.toLowerCase(Locale.ROOT)`）
- **系统输出**：禁止在核心代码中使用 `System.out`/`System.err`，应使用日志框架
- **原始类型字面量**：使用类字面量（例如 `int.class` 而非 `Integer.TYPE`）
- **过时的测试模式**：禁止使用 `@Test(expected=...)` 和 `assertThatExceptionOfType(...).isThrownBy(...)`，应使用专门的 AssertJ 异常断言

### 文档规范

- **JavaDoc**：公共 API 必须包含 JavaDoc
- **包级文档**：每个包应有 `package-info.java`
- **修改声明**：修改第三方代码时，必须在原始版权声明后添加修改声明
  ```java
  // Modifications Copyright 2017 - 2026 the TODAY authors.
  ```

## 检查与验证

### 静态代码分析

```bash
# 运行 Checkstyle 检查
./gradlew checkstyleMain

# 运行所有静态检查, 慎用（项目很大一般执行需要半小时）
./gradlew check
```

### Checkstyle 规则摘要

- 注解风格：紧凑格式
- 大括号位置：右大括号单独一行
- 禁止嵌套代码块超过 3 层
- 禁止每行多个变量声明
- 禁止尾随空格
- 强制 `@since` 使用主版本.次版本格式（非 `.0` 结尾）

### 代码覆盖率

- 目标覆盖率：81%+（当前 Codacy A 级评级）
- 使用 JaCoCo 收集覆盖率
- 报告位置：`build/reports/jacoco/testCodeCoverageReport/html/`

### 预提交检查

在提交代码前应运行：

```bash
# 慎用（项目很大一般执行需要半小时）
./gradlew check 
```

## 注意事项

### 版权与许可证

1. **严禁覆盖原始版权声明**：修改第三方代码（Spring、ASM 等）时必须保留原始版权头
2. **添加修改声明**：在原始声明后追加 `// Modifications Copyright 2017 - 2026 the TODAY authors.`
3. **许可证文件**：保留项目根目录的 LICENSE 和 NOTICE 文件

### 模块结构

- **核心模块**：`infra-` 前缀（如 `infra-core`, `infra-web`）
- **Starter 模块**：`infra-starter-` 前缀
- **构建工具**：`infra-build/` 目录
- **示例代码**：`infra-samples/` 目录

### 开发环境

- **Java 版本**：至少 JDK 17，支持到 JDK 25
- **Gradle 版本**：使用项目包装器（`gradlew`）
- **IDE 配置**：已包含 IntelliJ IDEA 运行配置（`.run/` 目录）

---

*本文档最后更新：2026年2月27日*  
*适用于 TODAY Infrastructure 5.0-Draft.6-SNAPSHOT*