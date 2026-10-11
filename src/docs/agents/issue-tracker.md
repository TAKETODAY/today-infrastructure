# Issue tracker: GitHub

本仓库的任务与规格存放在 `TAKETODAY/today-infrastructure` 的 GitHub Issues。
所有操作使用 `gh` CLI；下列命令在仓库目录执行，由远程配置识别仓库。
在其他目录执行时，显式指定 `--repo TAKETODAY/today-infrastructure`。

## 常用操作

- 创建：`gh issue create --title "..." --body "..."`
- 读取：`gh issue view <number> --json number,title,body,labels,comments`
- 列出：`gh issue list --state open --json number,title,body,labels,comments`，按需添加 `--label` 或调整 `--state`。
- 评论：`gh issue comment <number> --body "..."`
- 添加或移除标签：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- 关闭：`gh issue close <number> --comment "..."`

技能要求“发布到问题跟踪器”时，创建 GitHub Issue；
要求“获取相关任务”时，读取对应 Issue 及其评论。

## Pull requests as a triage surface

**PRs as a request surface: no.**

若日后改为 `yes`，外部 PR 使用相同的分诊标签与状态：
使用 `gh pr view`、`gh pr diff`、`gh pr comment`、
`gh pr edit` 和 `gh pr close` 处理。

GitHub 的 Issue 与 PR 共享编号空间；编号类型不明确时，
先通过 `gh pr view <number>` 判断，否则读取对应 Issue。

## 子任务与依赖

- `gh` 2.94+ 可通过 `gh issue create --parent <parent> ...`
  或 `gh issue edit <parent> --add-sub-issue <child>` 建立子任务。
- 旧版 CLI 使用 GitHub API：
  `gh api --method POST repos/TAKETODAY/today-infrastructure/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`。
- 子任务不可用时，在子任务正文开头写 `Part of #<parent>`，
  并在父任务正文维护任务列表。
- 阻塞关系优先使用 GitHub 原生 Issue dependencies：
  `gh api --method POST repos/TAKETODAY/today-infrastructure/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`。
- 数据库 ID 通过
  `gh api repos/TAKETODAY/today-infrastructure/issues/<number> --jq .id`
  获取，区别于 Issue 编号及 `node_id`。
- 原生依赖不可用时，在任务正文开头写 `Blocked by: #<n>, #<n>`。
  所有阻塞任务关闭后，该任务才可开始。

## Wayfinder 约定

- 地图：单个 Issue，标签为 `wayfinder:map`，
  正文维护 Notes、Decisions-so-far 与 Fog。
- 决策任务：链接到地图的子任务，使用 `wayfinder:<type>` 标签，
  type 为 `research`、`prototype`、`grilling` 或 `task`。
- 选择下一任务：按地图顺序选择尚未关闭、无人认领且无开放阻塞项的首个任务。
  原生依赖的开放阻塞数读取 `issue_dependencies_summary.blocked_by`；
  回退格式则检查 `Blocked by` 引用的任务状态。
- 认领：`gh issue edit <number> --add-assignee @me`，作为该会话的首次写操作。
- 完成：评论记录结论，关闭任务，再将结论摘要和链接追加到地图的 Decisions-so-far。
