# TODAY Infrastructure

Java 应用基础设施库（基于 Spring 设计思想移植/演进）。Gradle 多模块，JDK 17+。

## 构建

```bash
./gradlew assemble                                         # 编译（跳过测试）
./gradlew build                                            # 编译+测试
./gradlew :infra-core:build                                # 单个模块
./gradlew :infra-core:test --tests "*ClassName.methodName"  # 单个方法
./gradlew publishToMavenLocal                              # 安装到本地
./gradlew check                                            # 全部检查，慎用（>30min）
```

- 大项目，避免跑全量 `check`/`test`，优先跑单个模块
- Gradle 配置了并行构建、2048m heap

## 模块结构

- `infra-core` / `infra-beans` / `infra-context` — IoC 容器核心
- `infra-web` / `infra-webmvc` — Web MVC
- `infra-web-netty-server` — Netty HTTP 服务器
- `infra-http` / `infra-http-client` — HTTP 抽象与客户端
- `infra-aop` / `infra-aspects` — AOP
- `infra-test` / `infra-test-support` / `infra-testcontainers` — 测试支持
- `infra-webmvc-mock` — Mock HttpContext 用于测试
- `integration-tests` — 集成测试
- `infra-starter/infra-starter-*` — Starter 模块
- `infra-build/infra-*` — 构建插件
- `module/infra-*` — 可选应用模块

## 代码风格

- **缩进**：2 空格，LF，UTF-8（`.editorconfig`）
- **禁止通配符导入**（如 `import java.util.*`）
- **禁止的依赖**：JUnit 断言、Hamcrest、TestNG、JetBrains 注解 → 用 AssertJ + JSpecify
- **测试类**：`*Tests` 后缀（非 `*Test`）
- **Mock**：Mockito，优先 BDDMockito 风格
- **字符串大小写转换**：必须指定 `Locale`
- **NullAway**：`@SuppressWarnings("NullAway")` 可见于多处，留意空值注解（JSpecify）
- **修改第三方代码**：在原始版权后追加 `// Modifications Copyright 2017 - 2026 the TODAY authors.`
- **公共 API 必须写 JavaDoc**，包必须有 `package-info.java`
- Checkstyle 配置在 `checkstyle/checkstyle.xml`

## 架构要点

- `HttpContext` 是请求/响应总接口，`AbstractHttpContext` 是带字段缓存的骨架实现
- `DecorableHttpContext` 直接 `implements HttpContext`，无字段，纯委托给 `delegate()`
- `BindingContext` 管理数据绑定和校验，`getErrors()` 在其上
- CodeGraph 已索引（`.codegraph/`），`codegraph_explore` 优先于 grep/Read

## 注意事项

- 项目为学习/个人生产用，非商业产品
- 移植自 Spring Framework，修改时保留原始版权
- `.run/` 目录有预配置的 IntelliJ 运行配置
