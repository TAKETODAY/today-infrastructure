# 贡献指南

欢迎来到 **TODAY Infrastructure** 项目！感谢你愿意花费时间来帮助改进这个项目。本指南旨在帮助你高效、顺利地完成首次贡献。

## 🎯 在你开始之前

### 我们的理念
本项目是一个 **“从学习到生产”** 的实践。我们高度重视：
1.  **代码的清晰与优雅**：代码本身是最好的文档。
2.  **过程的严谨与合规**：所有贡献都必须尊重原始开源作品的版权。
3.  **产出的高质量**：我们拥有超过81%的测试覆盖率和Codacy A级评级，贡献应维护这一标准。

### 法律合规须知 (请务必阅读！)
本项目部分模块源于对 Spring Framework、ASM 等伟大开源项目的深入学习与重构。**尊重并遵守原始许可证是我们社区的底线。**

- **【铁律】绝对禁止覆盖版权声明**：任何涉及第三方开源代码（如 Spring, Spring Boot, ASM）的文件，**必须完整保留其原始版权和许可证文件头**。
- **如何正确添加声明**：如果你修改了一个已有原始声明的文件，应在原始声明**之后**追加你的修改声明，格式如下：

```
// Modifications Copyright 2017 - 2026 the TODAY authors.
```

- 如果不确定，请务必在Issue中提问。

## 🚀 贡献流程

### 1. 寻找贡献点
- **浏览现有议题**：查看带有 [`good first issue`](https://github.com/TAKETODAY/today-infrastructure/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) 标签的议题，这是专门为新贡献者准备的。
- **报告Bug或提议功能**：如果你发现了问题或有新想法，请先[搜索现有议题](https://github.com/TAKETODAY/today-infrastructure/issues) ，确认无误后创建新议题。

### 2. 开始工作
1.  **Fork 本仓库**。
2.  **创建分支**：基于主分支创建一个描述性的新分支，例如 `fix/xxx-bug` 或 `feat/add-xxx`。
3.  **设置开发环境**：请参考下面的“开发环境设置”部分。

### 3. 提交更改
- **进行代码审查**：在提交PR前，请自行审查代码，确保符合项目的代码风格和结构。
- 为行为变更增加或更新回归测试，执行受影响模块的定向测试，并说明验证结果。
- 提交信息使用 Gitmoji 短代码及中文动宾主题，例如 `:bug: 修复响应复制时的 Cookie 丢失`；正文说明动机与兼容取舍。

### 4. 发起拉取请求 (Pull Request)
1.  将你的分支推送到你Fork的仓库。
2.  打开 GitHub 的 Pull Request 界面，选择将你的分支合并到本仓库的主分支。
3.  **填写PR模板**：请清晰描述你的更改、动机以及如何测试。
4.  确保所有CI检查（Codacy、GitHub Actions）**全部通过**。

## ⚙️ 开发环境设置

本项目使用 **Gradle** 进行构建。

构建工具链使用 JDK 25，发布制品兼容 Java 17 及以上版本。使用仓库中的 Gradle Wrapper，优先构建受影响模块；常规增量开发无需运行 `clean`。

```bash
# 1. 克隆你的Fork仓库
git clone https://github.com/YOUR_USERNAME/today-infrastructure.git
cd today-infrastructure

# 2. 使用项目包装器（推荐）
./gradlew build

# 3. 导入IDE
# 项目已包含Gradle配置，可直接用IntelliJ IDEA或Eclipse导入。
```

**重要配置**：为避免自动覆盖版权头，请在IDE中检查并**禁用**“自动更新版权”

### 定向构建与测试

```bash
# 编译全部模块，不运行测试
./gradlew assemble

# 构建受影响模块
./gradlew :infra-core:build

# 运行指定测试类或方法
./gradlew :infra-core:test --tests '*StringUtilsTests'
./gradlew :infra-core:test --tests '*StringUtilsTests.hasText'

# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal
```

全量 `check` 包含测试和 Checkstyle，由 CI 执行；本地优先使用单模块任务。

## 代码风格与测试

- 遵循修改文件的现有风格，避免无关格式调整；使用 UTF-8、LF、两空格缩进，不保留行尾空白。
- 禁止通配符导入；测试类以 `Tests` 结尾，使用 JUnit Jupiter、AssertJ 和 Mockito，优先使用 BDDMockito。
- 编译期间启用 NullAway，使用 JSpecify 空值注解准确表达契约；合理的局部抑制遵循现有代码约定。
- 公共 API 提供英文 JavaDoc，每个包提供 `package-info.java`；新增 API 的 `@since` 使用项目实际版本。
- 字符串大小写转换显式指定 `Locale`。
- 自主编写且非第三方派生的文件使用 TODAY 原创版权头；派生文件完整保留原版权与许可证，并追加修改声明。
- 更多仓库约定见 [AGENTS.md](AGENTS.md)，Checkstyle 配置见 [checkstyle/checkstyle.xml](checkstyle/checkstyle.xml)。

## 参考文档

参考文档使用 AsciiDoc，位于 `infra-docs/modules/ROOT`。正文使用简体中文，示例需适配实际 infra API、配置键及章节链接。
