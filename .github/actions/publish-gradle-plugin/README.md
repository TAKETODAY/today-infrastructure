# Release 时发布 Gradle 插件

Release 工作流先发布 Maven 工件，再等待 Maven Central 上的框架依赖和 BOM 可用，
最后调用插件模块的 `publishPlugins` 任务，将实现工件及插件标记发布到 Gradle Plugin Portal。

## 发布坐标与插件 ID

- Maven 实现坐标：`cn.taketoday:infra-gradle-plugin:<version>`。
- Portal 应用插件 ID：`cn.taketoday.application`。
- AOT 内置于应用插件，通过 `infraApplication { enableAheadOfTimeProcessing() }` 或 Native Image 集成启用，不单独发布 AOT 插件 ID。
- GraalVM Native Image 集成通过 `infraApplication { enableNativeImage() }` 启用，Native Build Tools 版本随插件提供，无需手动应用 `org.graalvm.buildtools.native`。

插件 ID 和 Maven group 使用共同的 `cn.taketoday` 命名空间。
已有的 `infra.application` 保留为 JAR 内的兼容描述符，
通过 `buildSrc` 或 `buildscript` 加载插件实现后仍可使用；该短 ID 不发布到 Portal。
独立的 `infra.application.aot` ID 已移除，请使用 `infraApplication.enableAheadOfTimeProcessing()`。

首次发布前，在 [Plugin Portal](https://plugins.gradle.org/user/login) 注册账号并创建 API Key。
Portal 可能要求通过 `taketoday.cn` 的 DNS TXT 记录验证域名所有权，具体记录由 Portal 提供。
首次发布还需要人工审核，上传成功不等于立即可供使用。

## GitHub Secrets

在仓库 Actions Secrets 中配置：

| Secret                         | 用途                             |
|--------------------------------|----------------------------------|
| `GRADLE_PLUGIN_PUBLISH_KEY`    | Portal API Key                   |
| `GRADLE_PLUGIN_PUBLISH_SECRET` | Portal API Secret                |
| `SIGNID`                       | 已有 Maven 发布使用的签名密钥 ID |
| `SIGNPWD`                      | 签名密钥密码                     |
| `MAVEN_GPG_PRIVATE_KEY`        | ASCII-armored 私钥               |

发布 Action 使用环境变量传递凭据，并使用 JDK 25 构建。
发布版本取自 `build-and-stage-release` 的版本输出，必须是固定的非 SNAPSHOT 版本。
同一版本不能重复发布；第一次发布应检查 Portal 审核状态，失败后先核实已有上传结果。

## 使用发布后的插件

以下版本占位符应替换为已经发布并审核通过的实际版本：

```groovy
plugins {
  id 'java'
  id 'cn.taketoday.application' version '<version>'
}
```

默认插件仓库为 Gradle Plugin Portal。自定义了 `pluginManagement.repositories` 时，
应保留 `gradlePluginPortal()`，并按需加入 `mavenCentral()` 以解析框架依赖。
插件实现仍自动应用依赖管理并导入 `infra-dependencies` BOM。

## 本地验证

从仓库根目录执行以下命令，仅验证任务图和生成描述符，不上传：

```bash
./gradlew :infra-build:infra-gradle-plugin:publishPlugins --dry-run -PportalPublishing=true -PbuildSrc.skipTests=true
./gradlew :infra-build:infra-gradle-plugin:pluginDescriptors :infra-build:infra-gradle-plugin:processResources -PportalPublishing=true -PbuildSrc.skipTests=true
```

发布实现位于 `infra-build/infra-gradle-plugin/build.gradle`。
`gradlePlugin.plugins` 仅声明 `cn.taketoday.application`，正常构建和 Portal 发布使用相同的插件 ID。
`com.gradle.plugin-publish` 仅在 Portal 发布模式下应用，避免为普通 SNAPSHOT 构建创建需要密钥的 Portal 签名任务。
Release Action 传入 `-PportalPublishing=true`，启用 Portal 发布。
`src/main/resources/META-INF/gradle-plugins/` 中的短 ID 描述符保留原有入口，不生成对应的 Portal 插件标记。
配置缓存暂按未支持声明，确认兼容并测试后再修改该声明。

官方发布和审核规则：<https://plugins.gradle.org/docs/publish-plugin>。
