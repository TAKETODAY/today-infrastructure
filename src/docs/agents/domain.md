# Domain Docs

本仓库采用 single-context 领域文档布局：
`src/docs/GLOSSARY.md` 保存共享术语，`src/docs/adr/` 保存架构决策记录。
以下路径均相对于仓库根目录。

## 探索前读取

- 读取 `src/docs/GLOSSARY.md`。
- 读取 `src/docs/adr/` 中与当前工作领域相关的 ADR。
- 若日后存在 `src/docs/GLOSSARY-MAP.md`，先按映射读取相关上下文的词汇表，
  并检查对应上下文的 ADR。

文件尚不存在时，静默继续。
`domain-modeling` 在实际澄清术语或确定决策时按需创建这些文档；
`grill-with-docs` 和 `improve-codebase-architecture` 可调用该技能。

## 文件布局

- `src/docs/GLOSSARY.md`：全仓库共享术语。
- `src/docs/adr/0001-<decision>.md`：按编号记录架构决策。

## 使用规范术语

Issue 标题、重构提案、假设与测试名称涉及领域概念时，
使用词汇表定义的名称，避免使用词汇表明确排除的同义词。

所需概念尚未收录时，先判断它是否符合项目现有语言；
确有缺口则记录供 `domain-modeling` 后续澄清。

## 显式说明 ADR 冲突

提案与现有 ADR 冲突时，指出对应 ADR，并说明重新讨论的理由，
例如：“与 ADR-0007 冲突，但建议重新讨论，因为……”
