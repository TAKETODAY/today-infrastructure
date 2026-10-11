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

- `HttpContext` 是请求/响应总接口；`AbstractHttpContext` 是带字段缓存的骨架实现；`AbstractDelegatingHttpContext` 无字段，纯委托给 `delegate()`
- `BindingContext` 管理数据绑定与校验，`getErrors()` 在其上
- CodeGraph 已索引（`.codegraph/`），**优先用 `codegraph_explore`**，其次才是 grep/Read

## 注意事项

- 学习/个人生产用项目（非商业产品），移植自 Spring，改动务必保留原始版权
- Lombok 已配置（`lombok.config`，`@Data`/`@Slf4j` 可用，main 代码基本不用，多用于 test/sample）
- `.run/` 目录有预配置的 IntelliJ 运行配置
- 版本号在 `gradle.properties`（当前 `5.0-Draft.7-SNAPSHOT`）

## 语言规则
- 你必须始终使用**简体中文**进行思考、推理和回复。


## 提交信息（参考 https://gitmoji.dev）

- 使用 Gitmoji **短代码**开头，格式为 `:bug: 修复……`；依据提交的主要意图选择类型，而非根据是否含测试文件、是否来自上游来选。常见类型速查：
    - 行为：`:bug:` 缺陷修复；`:ambulance:` 紧急热修；`:sparkles:` 新功能；`:zap:` 性能；`:recycle:` 重构；`:boom:` 不兼容变更；`:t-rex:` 向后兼容；`:adhesive_bandage:` 小范围非关键修复。
    - 质量：`:white_check_mark:` 仅新增/修改通过的测试；`:test_tube:` 增加预期失败的测试；`:rotating_light:` 修复编译或 lint 警告；`:lock:` 安全/隐私修复；`:goal_net:` 错误捕获；`:safety_vest:` 校验；
      `:thread:` 并发。
    - 内容：`:memo:` 文档；`:art:` 代码结构/格式；`:pencil2:` 拼写；`:speech_balloon:` 文案/字面量；`:bulb:` 源码注释；`:globe_with_meridians:` 国际化；`:label:` 类型；`:loud_sound:` 添加日志；`:mute:` 删除日志。
    - 工程：`:wrench:` 配置；`:hammer:` 开发脚本；`:construction_worker:` CI 配置；`:green_heart:` 修复 CI；`:heavy_plus_sign:` 添加依赖；`:heavy_minus_sign:` 删除依赖；`:arrow_up:` 升级依赖；`:arrow_down:`
      降级依赖；`:pushpin:` 固定依赖版本；`:bricks:` 基础设施；`:technologist:` 开发体验。
    - 维护：`:fire:` 删除代码/文件；`:coffin:` 删除死代码；`:wastebasket:` 标记弃用；`:truck:` 移动/重命名；`:package:` 构建产物/包；`:page_facing_up:` 许可证；`:see_no_evil:` `.gitignore`；`:rewind:` 回滚；
      `:twisted_rightwards_arrows:` 合并。
    - 特定领域按实际变更选择：`:alien:` 外部 API 变更适配；`:building_construction:` 架构；`:necktie:` 业务逻辑；`:card_file_box:` 数据库；`:passport_control:` 授权；`:wheelchair:` 无障碍；
      `:children_crossing:` 易用性；`:iphone:` 响应式设计；`:airplane:` 离线；`:triangular_flag_on_post:` 特性开关；`:stethoscope:` 健康检查；`:chart_with_upwards_trend:` 分析/跟踪；`:mag:` SEO；`:seedling:`
      种子数据。
    - 其他场景：`:rocket:` 部署；`:bookmark:` 版本标签；`:tada:` 项目初始化；`:construction:` 进行中；`:lipstick:` UI 样式；`:bento:` 素材；`:camera_flash:` 快照；`:clown_face:` mock；`:egg:` 彩蛋；`:alembic:`
      实验；`:monocle_face:` 数据探索；`:busts_in_silhouette:` 贡献者；`:money_with_wings:` 赞助设施；`:dizzy:` 动画；`:beers:` / `:poop:` 属于非正式语义，不用于正常移植提交。
- 中文主题用一个动宾短句说清 **实际行为变化**，避免“同步上游”“迁移测试”之类过程描述。正文只在需要说明触发条件、修复机制、兼容取舍或上游溯源时补充，避免重复主题；不要写“逐项迁移上游
  BeanWrapperAutoGrowingTests 的……；infra-beans 对应测试全类通过”一类测试报告。
- 若上游原测试或文档与代码一同提交，以生产行为为主题；仅测试或仅文档的独立提交才分别用 `:white_check_mark:` 或 `:memo:`。测试映射、运行命令及结果集中写入批末归档，不塞进每个提交正文。代码、原测试及适用中文文档优先放在同一个目标提交，不人为拆分凑数。
- 提交信息可以按照上游的来，但是要保证中文

## Agent skills

### Issue tracker

任务与规格使用 `TAKETODAY/today-infrastructure` 的 GitHub Issues；读写任务前参阅 `src/docs/agents/issue-tracker.md`。

### Triage labels

使用五个默认分诊标签；执行分诊或设置分诊状态前参阅 `src/docs/agents/triage-labels.md`。

### Domain docs

采用 single-context 布局，领域文档集中在 `src/docs/`；探索领域概念与架构决策前参阅 `src/docs/agents/domain.md`。
