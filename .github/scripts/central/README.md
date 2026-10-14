# Central 发布脚本

`publish-central.sh` 用于正式版本的异步 Maven Central 发布；SNAPSHOT 仍使用共享构建 Action 的原有发布流程。
测试脚本使用本地 fixture 验证 524 恢复、重复执行保护和失败状态。

运行测试：

```bash
bash .github/scripts/central/test-publish-central.sh
```

恢复已有状态：

```bash
bash .github/scripts/central/publish-central.sh --resume
```
