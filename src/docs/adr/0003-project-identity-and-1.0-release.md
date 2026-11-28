# ADR-0003 TODAY Infrastructure 品牌、发布坐标与 1.0 版本体系

- 状态：提议中
- 日期：2026-11-27
- 影响范围：项目品牌、Maven Central、Gradle Plugin Portal、版本管理、官网与文档

## 背景

TODAY Infrastructure 经过长期研发，现使用 `cn.taketoday` 发布 Maven 工件，开发版本为 `5.0-Draft.10-SNAPSHOT`。项目希望以 `1.0` 作为首个正式稳定版本，建立长期维护的独立框架品牌以及明确的兼容性承诺。

`taketoday.cn` 是维护者的个人网站，`today-tech.cn` 是 TODAY Tech 的组织域名。TODAY Tech 下设 Infra Tech 基础技术线，使用 `infra-tech.cn` 作为技术生态入口和 Maven 发布命名空间；`today-infra.cn` 作为 TODAY Infrastructure 的专属项目入口。框架及相关工具由 GitHub 组织 `today-tech` 维护。

## 目标方案

| 项目                | 目标                                                 | 说明                                    |
|---------------------|------------------------------------------------------|-----------------------------------------|
| 组织品牌            | TODAY Tech                                           | 承载技术项目，与个人品牌分离            |
| 框架名称            | TODAY Infrastructure                                 | 保留现有名称                            |
| 组织官网            | `https://today-tech.cn`                              | 技术品牌入口，待建设                    |
| 基础技术线入口      | `https://infra-tech.cn`                              | Infra Tech 生态入口                     |
| 框架项目入口        | `https://today-infra.cn`                             | TODAY Infrastructure 专属入口           |
| 官方文档            | `https://today-tech.cn/today-infrastructure`         | 保留现有地址                            |
| GitHub 组织         | `https://github.com/today-tech`                      | 推荐作为主组织，仓库迁移待确认          |
| GitHub 仓库         | `https://github.com/today-tech/today-infrastructure` | 目标地址                                |
| Maven groupId       | `cn.infra-tech`                                      | 1.0 系列目标发布命名空间                |
| Maven artifactId    | `infra-*`                                            | 保留现有模块命名                        |
| Java 包名           | `infra.*`                                            | 保留现有公共 Java 包名                  |
| 框架 Gradle 插件 ID | `cn.infra-tech.application`                          | Infra Tech 基础技术组件                 |
| IDE 插件 ID         | `cn.today-tech.infra-assistant`                      | TODAY Infra Assistant，Marketplace 分发 |
| 首个稳定版本        | `1.0`                                                | 新版本体系的起点                        |
| 个人网站            | `https://taketoday.cn`                               | 保留个人品牌用途                        |

## GitHub 组织与仓库规划

TODAY Tech 采用核心框架与周边项目分仓库维护的组织结构，规划如下：

```text
github.com/today-tech
│
├── today-infrastructure       核心框架
├── today-tech.github.io       技术品牌官网（如果使用 GitHub Pages）
├── infra-website              Infra Tech 或框架网站（未来）
├── infra-examples             独立示例项目
├── infra-initializr           项目初始化工具（未来）
├── infra-benchmarks           性能基准测试（未来）
└── .github                    组织介绍与共享配置
```

- `today-infrastructure`：继续维护框架源码、Starter、构建插件及版本化参考文档，保留现有多模块结构。
- `today-tech.github.io`：若采用 GitHub Pages 托管组织官网，则使用该仓库承载 `today-tech.cn`。
- `infra-website`：若独立建设 Infra Tech 或框架网站，则承载 `infra-tech.cn` 或 `today-infra.cn` 的项目介绍、快速开始和生态导航，链接至统一技术文档。
- `infra-examples`：面向框架使用者的独立示例应用，验证已发布版本的实际使用体验。
- `infra-initializr`：未来提供面向 TODAY Infrastructure 的项目生成与初始化服务。
- `infra-benchmarks`：未来维护跨版本、可重复执行的性能基准测试。
- `.github`：通过 `profile/README.md` 展示组织与项目生态，并维护共享社区配置。

上述结构是长期规划，优先完成核心框架仓库迁移及组织介绍，官网、示例和其他周边仓库根据实际需求逐步建立，不要求全部作为 1.0 的发布前置条件。现有框架内部示例和基准测试也无需立即拆出。

核心框架建议在 `1.0-Alpha.1` 发布前通过 GitHub 原生 [Transfer repository](https://docs.github.com/en/repositories/creating-and-managing-repositories/transferring-a-repository) 功能从 `TAKETODAY/today-infrastructure` 转移至目标组织，继承现有 Stars、Forks、提交历史、Issues、Pull Requests 和 Releases。保留旧仓库地址的自动重定向，不在原位置重新创建同名仓库；迁移前后检查组织权限、CI/CD、文档部署和发布流水线，并更新仓库引用。

## Maven 命名空间选择

采用 `cn.infra-tech` 作为 Infra Tech 基础技术线的 Maven groupId，反转域名 `infra-tech.cn`。`infra-tech.cn` 和 `today-tech.cn` 是独立域名；Infra Tech 拥有独立的发布命名空间，仍是 TODAY Tech 旗下技术线。`today-infra.cn` 是框架产品入口，不单独决定 Maven 命名空间；框架官网和文档托管方式可以独立演进。

Maven groupId 与 Java 包名承担不同职责。[Maven Central 命名空间注册规范](https://central.sonatype.org/register/namespace/#for-dns) 明确允许 groupId 中存在连字符，并且不要求 Java 包名与之匹配。因此无需为了迁移 Maven 坐标而重命名 `infra.*` Java 包。

考虑过的替代方案：

- `cn.taketoday`：延续已有发布坐标，迁移成本最低，但仍与个人域名绑定，并且在同一坐标下从 `5.0-Draft` 切换到 `1.0` 会产生版本排序问题。
- `cn.today-tech`：组织级命名空间，适用于 TODAY Tech 的应用级产品或组织通用产品。
- `cn.today-tech.infra`：在组织命名空间下划分框架子组；选择独立的 `cn.infra-tech` 可以统一承载 Infra Tech 的框架、构建工具和其他库。

目标依赖坐标示例：

```groovy
dependencies {
  implementation platform('cn.infra-tech:infra-bom:1.0')
  implementation 'cn.infra-tech:infra-starter-webmvc'
}
```

### 项目归类与命名空间

项目按照“库级基础技术”和“应用级产品”区分发布命名空间：

| 项目                                   | 归属                      | 发布标识                                                                                                   | Java 包名                |
|----------------------------------------|---------------------------|------------------------------------------------------------------------------------------------------------|--------------------------|
| TODAY Infrastructure                   | Infra Tech 核心框架       | `cn.infra-tech:infra-*`                                                                                    | `infra.*`                |
| Maven Central Publishing Gradle Plugin | Infra Tech 构建与发布工具 | `cn.infra-tech:maven-central-publishing-gradle-plugin`，Gradle ID `cn.infra-tech.maven-central-publishing` | `infra.publishing`       |
| TODAY Infra Assistant                  | Infra Tech IDE 应用产品   | JetBrains ID `cn.today-tech.infra-assistant`，不以 Maven 库形式发布                                        | `cn.todaytech.assistant` |

库级组件面向其他工程集成，使用 `cn.infra-tech` 和 `infra.*`；应用级产品面向最终用户直接安装或使用，使用 `cn.today-tech` 体系。应用级产品仍可以归属于 Infra Tech 生态，生态归属、发布方和技术命名空间分别承担不同职责。

## 版本与兼容性

将 `5.0-Draft.x` 系列作为历史研发版本保留。新版本体系建议继续支持框架已有的 `Draft`、`Alpha`、`Beta` 阶段，并保留 `RC`（Release Candidate）作为可选的最终发布验证阶段，无需采用 `M1`、`M2` 等里程碑命名。各阶段可以按实际准备情况跳过或重复：

```text
Draft → Alpha → Beta → RC（可选）→ Release
```

- `SNAPSHOT`：持续开发构建，可重复发布，不代表固定的成熟度阶段。
- `Draft.N`：设计草案阶段，用于探索架构、模块边界和 API，不承诺功能完整性或兼容性。
- `Alpha.N`：早期预览，功能及公共 API 仍可调整。
- `Beta.N`：主要功能基本完成，重点验证实际使用、兼容性与稳定性。
- `RC.N`：可选的发布候选，公共契约已基本冻结，原则上只修复发布阻塞问题。
- 无后缀：正式稳定版本，如 `1.0`。

已有的 `5.0-Draft.x` 研发成果可作为 1.0 的草案阶段基础，不需要仅因切换坐标而重新经历 Draft；达到 Alpha 准入条件后即可发布 `1.0-Alpha.1`。首个稳定版本及重大版本建议按需发布 RC，Beta 已完成最终验收时也可以直接进入稳定版本。补丁版本通常直接发布。

预发布序号从 1 开始，按数值递增，固定使用上述大小写及点号格式。已发布的 Draft、Alpha、Beta、RC 工件与稳定版一样不可覆盖。`SNAPSHOT` 是构建状态，可与阶段组合，例如 `1.0-Alpha.1-SNAPSHOT`。不能把上述成熟度顺序直接当作 Maven 或 Gradle 的版本比较顺序；例如 Maven 对同一基础版本的标准限定词排序是 `alpha < beta < milestone < rc < snapshot < release`，而非标准限定词 `Draft` 可能排在稳定版本之后。因此 Draft 主要用于早期研发，若发布 Draft 工件，应使用明确的版本号，并验证依赖解析行为，不依赖动态版本选择来推断成熟度。

现有 `infra.lang.Version` 已支持 `Draft.N`、`Alpha.N`、`Beta.N`，但没有显式的 `RC` 类型，并且当前将 `SNAPSHOT` 排在 `Alpha` 之前。1.0 前需明确并验证版本解析、阶段判断、比较顺序以及带 `-SNAPSHOT` 后缀的版本语义，区分框架的成熟度顺序与构建工具的依赖版本排序，同时保留历史 `Draft` 格式的解析能力。构建中的非 SNAPSHOT 发布与正式稳定版本也应明确区分。

稳定版本采用语义化版本规则：补丁版本用于向后兼容的缺陷修复，次版本用于向后兼容的功能增强，主版本用于不兼容的公共契约变更。Git 发布标签继续采用 `v<版本号>`，例如 `v1.0`。

1.0 发布前应明确公共 API、SPI、配置属性、Starter、插件接口、JDK 支持范围及兼容性策略。公共契约一经发布，应按照版本规则演进；内部实现与实验性接口的稳定性范围需在发布前说明。

## 迁移边界与待办

本文记录 1.0 的目标方案。已发布的 `5.0-Draft.x` 工件继续使用 `cn.taketoday`。以下事项需在实施过程中完成：

- [ ] 在 Maven Central 单独注册并验证 `cn.infra-tech`，确认框架发布账号有权限；已验证的组织级 `cn.today-tech` 不覆盖这个命名空间。
- [ ] 确认 Infra Tech Gradle 插件 ID 的发布权限、迁移时机及旧 ID 的兼容策略。
- [ ] 将新版本的 Maven groupId、BOM、Starter、插件实现坐标及生成的发布元数据统一迁移，开启 `1.0-SNAPSHOT` 开发线。
- [ ] 核对构建脚本、`buildSrc`、依赖管理、集成测试、示例、README 和文档中使用的旧坐标，区分新版本引用与仍需解析的历史工件。
- [ ] 将 Infra Tech 构建工具迁移到 `cn.infra-tech` 命名空间及 `infra.*` 包名；TODAY Infra Assistant 保持应用级 `cn.today-tech` 插件标识和 `cn.todaytech.assistant` 包名。
- [ ] 验证新坐标下的 BOM、Starter、Maven/Gradle 插件及示例应用能正常解析和运行。
- [ ] 确认 GitHub 仓库归属、官网入口与发布元数据中的项目链接。
- [ ] 发布 1.0 迁移指南，说明旧坐标、版本及插件 ID 的变更；根据实际用户需求决定是否提供 Maven relocation POM。避免在同一应用中同时引入新旧坐标下包含相同 Java 类的工件。
- [ ] 明确 1.0 的稳定性范围、兼容性承诺和发布验收条件。
- [ ] 完善 `infra.lang.Version` 对新版本体系的支持，并核对 Maven、Gradle、发布脚本及文档的预发布版本处理规则。

历史版本和 Git 标签保留原样，已经发布的 Maven 工件不重新发布或覆盖。各项实施任务可在 GitHub Issues 中跟踪，并引用本 ADR。
