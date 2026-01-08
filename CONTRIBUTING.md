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
  ```java
  /*
   * 由 TODAY Infrastructure 修改。
   * 修改部分同样遵循 [原始许可证，如Apache 2.0] 进行许可。
   */
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
- todo

### 4. 发起拉取请求 (Pull Request)
1.  将你的分支推送到你Fork的仓库。
2.  打开 GitHub 的 Pull Request 界面，选择将你的分支合并到本仓库的主分支。
3.  **填写PR模板**：请清晰描述你的更改、动机以及如何测试。
4.  确保所有CI检查（Codacy、GitHub Actions）**全部通过**。

## ⚙️ 开发环境设置

本项目使用 **Gradle** 进行构建。

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