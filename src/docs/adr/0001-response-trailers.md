# ADR-0001 响应 Trailer 的注册与发送契约

- 状态：已接受
- 日期：2026-11-04
- 关联：[#374](https://github.com/TAKETODAY/today-infrastructure/issues/374)
- 影响模块：`infra-webmvc`、`infra-web-netty-server`、`infra-webmvc-mock`

## 背景

部分响应元数据只能在正文生产结束后确定，典型例子是边流式写出边计算的 `Content-Digest`。若要求在响应头阶段给出这些值，就只能先把整个正文缓存起来，与流式输出的目标直接冲突。

HTTP/1.1 的 chunked 编码和 HTTP/2 都允许在正文之后发送尾部字段（trailer）。#374 给出了初步接口，但冻结实现前有五处需要确认：注册窗口、HTTP/1.1 协商、`Content-Length` 处理、禁止字段范围、失败语义。

本 ADR 记录契约与协议策略；“现状核对”描述已有发送结构，“决策”描述实现应遵循的约束，不代表每项修正已经落地。Trailer 是按需补齐的底层能力，不作为普通 REST、SSE 或 AI 流式输出的默认结束机制，也不等同于支持 gRPC。

## 现状核对

以下结论来自当前代码，用于替代 #374 中的推测：

1. **提交状态机的实际顺序**：`NettyHttpContext.writeHeaders()` 先执行 `committed.compareAndSet(false, true)`，再调用 `onCommitting()`，之后才装配状态码与响应头。因此在 `COMMITTING` 回调内 `isCommitted()` 已经是 `true`。
2. **正常完成入口集中，但不天然保证一次求值**：`requestCompletedInternal(notHandled)` 在 `notHandled == null` 时先执行 `flush()`，再调用 `endResponse()`；trailer supplier 在 `endResponse()` 中同步求值，不等待异步写出完成。入口集中不能代替对重复、重入或并发求值的防护。
3. **协议终止消息可替换**：HTTP/1.1 通过 `LastHttpContent` 结束响应，HTTP/2 通过带 `END_STREAM` 的 DATA 或 HEADERS 帧结束响应。Trailer 可沿用这些结束点，不需要新增生命周期阶段；HTTP/2 尾部字段名必须规范化为小写。
4. **文件终止机制无需变化，但发送与清理策略需要适配**：`ChunkedNioFile`、`DefaultFileRegion` 和 HTTP/2 文件 DATA 包装器不负责结束响应，终止消息仍由 `endResponse()` 写出。有 Trailer 的 HTTP/1.1 需要禁用现有 zero-copy `FileRegion` 路径，改走支持 chunked 的文件发送路径；由此打开的文件资源必须在发送完成或失败、尚未发送即重置、异常退出及替换待发送文件时正确回收。
5. **`Content-Length` 自动推断是全局性的**：`writeHeaders()` 的长度推断同时影响 HTTP/1.1 与 HTTP/2。有 Trailer 时 HTTP/1.1 因 chunked 定界必须移除 `Content-Length`；HTTP/2 可以保留正确的长度，本项目选择统一省略，不能将这一策略解释成 HTTP/2 的协议要求。
6. **`reset()` 是清理点**：错误处理调用 `reset()` 时必须清除 Trailer 配置和待发送资源。仅以注册时状态或正常完成入口判断，不能覆盖后续状态改变；异步发送失败由 `CLOSE_ON_FAILURE` 终止连接或流。

## 决策

### 接口形态：`Trailer` 绑定名称与求值

```java
void setTrailerFields(Trailer... trailers)
```

`Trailer` 是 `infra.web` 中的不可变 record，包含字段名和 `Supplier<String>` 值供应器：

```java
context.setTrailerFields(
    Trailer.field("Content-Digest", () -> digest.value()));
```

- 名称随注册一起声明，传输层不需要再单独接收名称集合。
- 后一次注册替换前一次注册；空数组表示清除注册，不触发 supplier。`null` 数组或 `null` 元素仍为非法参数。
- 注册多个同名字段会产生多个值。
- supplier 返回 `null` 表示本次不发送该字段；所有字段都返回 `null` 时不发送尾部字段。
- `HttpHeaders` 退回实现内部，仅用于最终写帧，不再出现在公共参数中。

### 决策 1：注册窗口截止于响应头提交之前

`setTrailerFields` 只在 `isCommitted()` 为 `false` 时接受注册或通过空数组清除注册；提交后（包括 `COMMITTING` 回调内）抛 `IllegalStateException`。

- **理由**：`COMMITTING` 触发时 committed 标记已置位，状态已经脱离「可配置」区间。允许在 `COMMITTING` 内注册会让契约出现一个无法用 `isCommitted()` 解释的例外，也迫使实现额外维护一个 `headersWritten`
  标志。禁止它让规则保持单一。
- **实现落点**：`AbstractHttpContext` 在保存或清除配置前调用 `assertNotCommitted()`。空数组清除与注册共享提交边界，清除操作无需执行 supplier。

### 决策 2：HTTP/1.1 不做 `TE: trailers` 强制协商

Trailer 的发送以 chunked 定界为准，不要求请求携带 `TE: trailers`。

- **理由**：RFC 9110 建议在客户端声明可接收 Trailer，或字段可被安全丢弃时发送。本接口采用后者的尽力而为策略，调用者只能依赖可被安全忽略的元数据；不能将 Trailer 作为请求正确完成的唯一必要信息。浏览器是否暴露 Trailer 还取决于客户端 API，省略协商不会保证浏览器脚本可读取。
- **代价**：必须在使用文档中说明「尽力而为」——代理可能剥离尾部字段，客户端也可能忽略。注册成功只描述本端具备发送能力，不承诺端到端送达。
- **实现落点**：`writeHeaders()` 在存在 Trailer 配置时强制 `Transfer-Encoding: chunked` 并移除 `Content-Length`，与现有 SSE 分支同构。

### 决策 3：有 Trailer 时不发送 `Content-Length`

存在 Trailer 配置时，HTTP/1.1 与 HTTP/2 都不写 `Content-Length`，并同步清空 `responseContentLength` 缓存。

- **协议要求**：HTTP/1.1 使用 Trailer 时必须采用 chunked 定界，因此不能同时发送 `Content-Length`。Trailer 值需要延迟计算，不代表正文长度必然未知，例如文件长度可以提前确定。
- **项目策略**：HTTP/2 使用帧定界，允许正确的 `Content-Length` 与 Trailer 共存。本项目为统一适配器行为选择省略该字段，并清空缓存；这是简化实现的策略，不是 HTTP/2 的限制。
- **实现落点**：在 `writeHeaders()` 的长度推断分支前判断 Trailer 配置，命中时跳过整段推断；注册 Trailer 时若已有 `responseContentLength`，一并复位为 `-1`。
- **冲突策略**：调用方手动设置的 `Trailer` 响应头被注册的字段集合 **覆盖**，不合并。两份声明不一致时以注册信息为准，避免客户端看到未承诺的字段名。

### 决策 4：禁止字段采用黑名单，值统一校验控制字符

注册时按大小写不敏感校验并去重；发送时拒绝以下字段，注册阶段能判定的直接抛 `IllegalArgumentException`：

- 定界与连接控制：`Content-Length`、`Transfer-Encoding`、`Connection`、`Keep-Alive`、`TE`、`Upgrade`、`Proxy-Connection`、`Trailer`
- 路由与认证：`Host`、`Authorization`、`WWW-Authenticate`、`Proxy-Authenticate`、`Proxy-Authorization`
- 内容语义：`Content-Encoding`、`Content-Type`、`Content-Range`、`Content-Location`
- 响应控制：`Set-Cookie`、`Cache-Control`、`Expires`、`Vary`

值校验拒绝除 `HTAB`（`0x09`）外的所有 C0 控制字符（`0x00`–`0x1F`），并拒绝 `DEL`（`0x7F`）。这包含 `CR`、`LF`、`NUL`，同一规则适用于真实适配器和 Mock；传输编码器还可执行自身所需的额外校验。

- **理由**：RFC 9110 §6.5.1 把 trailer 限定为「正文处理完成后才确定的元数据」。上面这些字段要么必须在头部阶段生效（定界、认证、缓存），要么与正文语义强绑定，放进 trailer 会被中间件忽略或误用。黑名单比白名单更能容纳
  `Content-Digest`、`ETag`、`Server-Timing` 这类未来字段。
- **实现落点**：名称校验放在 `AbstractHttpContext` 的注册方法内，值校验放在 supplier 求值后、写入尾部字段前，保证各适配器与 Mock 行为一致。

### 决策 5：失败即终止，不重试、不伪装成功

- supplier 抛异常或产生非法值：不重试，不发送部分 Trailer，也不补发正常结束标记；异常必须进入传输层失败处理。
- supplier 返回 `null`：只跳过该字段；同一次注册中的其他字段仍可正常发送。
- HTTP/1.1：响应已经提交后失败，应关闭连接，使客户端能够识别响应截断，不能用正常终止 chunk 伪装成功。
- HTTP/2：终止对应流并保留诊断信息；具体采用流关闭或 reset 取决于统一异常处理及 Netty 管线，本契约不保证必然发送 `RST_STREAM`。
- 正文生产失败（`notHandled != null`）时跳过 supplier。正文写出是异步的，supplier 求值之后才报告的写失败无法回滚已生成的字段；此时由 `CLOSE_ON_FAILURE` 关闭连接或流，客户端通过截断识别失败。
- supplier 在 `requestCompletedInternal()` 的完成路径中同步调用，必须是非阻塞的。它运行在请求线程，而不是 I/O 完成回调；supplier 求值不构成网络写出成功或客户端收到完整正文的证明。
- 一次求值采用原子防护，在调用 supplier 前占用求值资格；重复、重入或并发调用不能再次执行 supplier，失败后也不重试。该防护只保证求值次数，不使整个上下文或 supplier 自动具备线程安全性。
- 失败路径仍须执行约定的完成清理并释放资源；不能因为 supplier 异常跳过必要清理。supplier 在 `onRequestCompleted()` 之前执行，因为 `requestCompletedInternal()` 先于 `COMPLETED` 回调；异步写失败可能发生在 `COMPLETED` 之后，回调不得依赖写完成的确认。
- 终止消息（HTTP/1.1 的 `LastHttpContent`、HTTP/2 的终止 DATA/HEADERS）写入失败时通过 `CLOSE_ON_FAILURE` 关闭连接或流。头部和正文写入同样使用该监听；实现不再等待写入成功后才求值 supplier。

- **理由**：Trailer 可承载正文完整性元数据，但它本身不是端到端交付确认。异常后补发部分字段或正常结束标记会让客户端误判正文完整性；实现必须分别处理正文生产、管线写出和传输失败。

### 不支持路径

以下情况在非空注册时即拒绝，不静默接受；骨架实现支持在提交前传入空数组清除注册，不因当前方法、状态或适配器能力而拒绝该清除操作（接口默认实现仍报告不支持）：

- 适配器内部判定当前协议不具备发送能力（如 HTTP/1.0）→ `UnsupportedOperationException`
- `HEAD` 请求 → `IllegalStateException`
- 无正文状态（`1xx`、`204`、`304`）→ `IllegalStateException`
- 协议升级与成功 `CONNECT` → `IllegalStateException`

注册检查不代替最终状态检查。在最终响应头提交时（包括 `COMMITTING` 回调完成后），若最终状态为 `1xx`、`204`、`304` 或成功 `CONNECT`，则清除已有 Trailer 注册，不调用 supplier，也不生成 Trailer 声明或因该注册启用 chunked。`101` 协议升级属于 `1xx`，同样清除注册。调用方可以先注册、再修改状态，最终发送必须以最终状态为准。

`flush()` 既不结束响应也不触发 supplier；`reset()` 清除配置并回收尚未发送的资源；提交前传入空数组可只清除 Trailer 注册，不重置其他响应状态。`onChannelInactive()` 释放待发送的正文和文件。

`COMMITTED` 回调不得修改响应头。当前响应头适配器与 Netty 存储共享同一份数据（`Http1Headers` 为 live view），在真正 flush 之前技术上仍可修改；这是实现细节，不属于契约。

## 不在本 ADR 范围

- 请求 Trailer
- 自动摘要计算（`Content-Digest` 只作为示例）
- gRPC、HTTP/3
- 其他服务端适配器的完整实现
- 消息转换器对 Trailer 的感知

## 后果

- `HttpContext` 新增 `setTrailerFields(Trailer...)` 默认方法，默认抛 `UnsupportedOperationException`；是否发送尾部字段由各传输适配器在响应完成路径中决定，已有实现无需改动即可编译。
- `AbstractHttpContext` 承担全部配置状态、校验与原子防护的一次求值；`AbstractDelegatingHttpContext` 与 `DelegatingHttpContext` 纯委托，不另存状态。
- `MockHttpContext` 复用骨架行为，额外暴露最终字段供断言。
- 未配置 Trailer 时保持既有响应定界和终止行为；文件资源回收和必要的完成清理不能依赖是否配置 Trailer。
- 实现需要按 #374 验收项补充契约测试与 HTTP/1.1、HTTP/2 协议级测试。测试重点：注册时机、替换与空数组清除、防御性复制、最终状态变化、`reset`、空结果、空值跳过、同名多值、禁止字段与控制字符、并发及失败后不重复求值、keep-alive 复用、HTTP/2 字段名小写、尾部 HEADERS 顺序、`END_STREAM` 唯一性、文件资源回收、连接或流关闭。
