# TODAY Infrastructure

Java 基础设施库，移植/演进自 Spring Framework（group `cn.taketoday`）。Gradle 多模块，JDK 17+（编译 toolchain 用 JDK 25，`--release 17`）。

## 构建

```bash
./gradlew assemble                                          # 编译（跳过测试）
./gradlew build                                             # 编译+测试
./gradlew :infra-core:build                                 # 单个模块
./gradlew :infra-core:test --tests "*ClassName.methodName"  # 单测单个方法
./gradlew publishToMavenLocal                               # 安装到本地 Maven
./gradlew check                                             # 全量检查（Checkstyle+测试），仅 CI 使用
```

- 大项目：**避免跑全量 `check`/`test`**，优先只跑单个模块。CI 在 JDK 21/25 × macOS/Ubuntu 上跑 `check`（约 60min 超时），本地勿复刻
- Gradle 并行构建 + 2048m heap（`gradle.properties`）
- 测试在 CI 自动重试 3 次（`buildSrc/.../TestConventions.java`），可用 `-PtestGroups=...` 筛测试组
- 测试类约定 `*Tests` 后缀（勿新建 `*Test`）；构建同时匹配 `*Test`/`*Tests`

## 模块结构（以 `settings.gradle` 为准）

- 容器核心：`infra-core` `infra-beans` `infra-context` `infra-context-support` `infra-context-indexer` `infra-expression` `infra-core-test` `infra-instrument` `infra-app`
- 事务/持久化：`infra-jdbc` `infra-persistence` `infra-tx` `infra-jcache` `infra-oxm`
- Web MVC：`infra-web` `infra-webmvc` `infra-web-core` `infra-web-client` `infra-web-server` `infra-webmvc-mock` `infra-websocket`
- Reactive/Netty：`infra-web-reactive` `infra-web-reactor-server` `infra-web-netty-server` `infra-http` `infra-http-client` `infra-http-codec` `infra-http-service` `infra-http-reactive` `infra-http-converter`
- AOP/消息：`infra-aop` `infra-aspects` `infra-messaging`
- 测试支持：`infra-test` `infra-test-support` `infra-testcontainers` `integration-tests`
- 发布/BOM：`infra-build/*`（Gradle/Maven 插件、注解处理器）、`infra-bom` `infra-docs` `infra-dependencies`
- `infra-starter/*` 与 `module/*` 在 `settings.gradle` 里**按目录动态包含**，新增子模块不用改它；`module/*` 是应用型示例（`infra-app-*`、`infra-json`、`infra-mail`、`infra-flyway` 等），非 `javaProject`

## 代码风格

- 缩进 2 空格，LF，UTF-8（`.editorconfig`）
- 禁止通配符导入（如 `import java.util.*`）
- 测试用 AssertJ + Mockito（优先 BDDMockito），**禁用** JUnit 断言 / Hamcrest / TestNG / JetBrains 注解（根 `build.gradle` 已统一注入依赖）
- 字符串大小写转换必须指定 `Locale`
- NullAway 空值校验生效：`@SuppressWarnings("NullAway")` 是合法且常见的写法，留意 JSpecify 空值注解
- 修改第三方源码（Spring/ASM 等）：**必须保留原始版权头**，在其后追加 `// Modifications Copyright 2017 - 2026 the TODAY authors.`
- 公共 API 必须写 JavaDoc，每个包必须有 `package-info.java`
- Checkstyle 配置在 `checkstyle/checkstyle.xml`（tool version 13.3.0），由 `check` 触发

## 架构要点

- `HttpContext` 是请求/响应总接口；`AbstractHttpContext` 是带字段缓存的骨架实现；`DecorableHttpContext` 无字段，纯委托给 `delegate()`
- `BindingContext` 管理数据绑定与校验，`getErrors()` 在其上
- CodeGraph 已索引（`.codegraph/`），**优先用 `codegraph_explore`**，其次才是 grep/Read

## 注意事项

- 学习/个人生产用项目（非商业产品），移植自 Spring，改动务必保留原始版权
- Lombok 已配置（`lombok.config`，`@Data`/`@Slf4j` 可用，main 代码基本不用，多用于 test/sample）
- `.run/` 目录有预配置的 IntelliJ 运行配置
- 版本号在 `gradle.properties`（当前 `5.0-Draft.7-SNAPSHOT`）

## 语言规则
- 你必须始终使用**简体中文**进行思考、推理和回复。
