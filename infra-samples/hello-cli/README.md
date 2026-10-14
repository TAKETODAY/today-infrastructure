# 普通命令行应用

对应 `infra-docs/modules/application/pages/using/non-web-applications.adoc` 中的命令行示例。
演示普通应用启动、组件扫描、构造器注入、配置覆盖和 `ApplicationRunner`。
Runner 完成后关闭应用上下文，进程正常退出。

## 运行

在 `infra-samples` 目录执行（Windows 使用 `gradlew.bat`）：

```bash
./gradlew :hello-cli:infraRun
```

除启动日志外，输出 `Hello, World!`。

传入姓名并覆盖配置：

```bash
./gradlew :hello-cli:infraRun --args='--cli.greeting=你好 Alice Bob'
```

输出：

```text
你好, Alice!
你好, Bob!
```

`--cli.greeting=你好` 是配置选项，`Alice` 和 `Bob` 是非选项参数。
也可以从 IDE 运行 `com.example.cli.CliApplication` 的 `main` 方法。

## 打包

```bash
./gradlew :hello-cli:infraJar
java -jar hello-cli/build/libs/hello-cli-1.0.jar --cli.greeting=你好 Alice
```

版本继承自 `infra-samples/gradle.properties`；修改版本后请使用实际生成的 JAR 文件名。
示例使用 `infra-samples` 的共享构建配置，构建插件和框架版本由
`buildSrc/build.gradle` 读取仓库根目录的版本配置并从 Maven 仓库解析。
