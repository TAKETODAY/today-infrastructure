# ADR-0001 响应 Trailer 的注册与发送契约

- 状态：已接受
- 日期：2026-11-04
- 关联：[#374](https://github.com/TAKETODAY/today-infrastructure/issues/374)
- 影响模块：`infra-webmvc`、`infra-web-netty-server`、`infra-webmvc-mock`

## 背景

部分响应元数据只能在正文生产结束后确定，典型例子是边流式写出边计算的 `Content-Digest`。若要求在响应头阶段给出这些值，就只能先把整个正文缓存起来，与流式输出的目标直接冲突。

HTTP/1.1 的 chunked 编码和 HTTP/2 都允许在正文之后发送尾部字段（trailer）。#374 给出了初步接口，但冻结实现前有五处需要确认：注册窗口、HTTP/1.1 协商、`Content-Length` 处理、禁止字段范围、失败语义。

本 ADR 只冻结契约与协议策略，不涉及代码实现。Trailer 是按需补齐的底层能力，不作为普通 REST、SSE 或 AI 流式输出的默认结束机制，也不等同于支持 gRPC。

## 现状核对

以下结论来自当前代码，用于替代 #374 中的推测：

1. **提交状态机的实际顺序**：`NettyHttpContext.writeHeaders()` 先执行 `committed.compareAndSet(false, true)`，再调用 `onCommitting()`，之后才装配状态码与响应头（`NettyHttpContext.java:665`）。因此在
   `COMMITTING` 回调内 `isCommitted()` 已经是 `true`。
2. **响应结束只有一条路径**：`requestCompletedInternal(notHandled)` 在 `notHandled == null` 时依次执行 `flush()` 与 `endResponse()`（`NettyHttpContext.java:630`）。`flush()` 可多次调用，`endResponse()`
   只在此处调用一次，「至多求值一次」和「失败不发」由结构天然保证。
3. **协议终止消息可替换**：HTTP/1.1 的 `endResponse()` 写 `LastHttpContent.EMPTY_LAST_CONTENT`（`infra-web-netty-server/.../HttpContext.java:146`），HTTP/2 的 `endResponse()` 写空 DATA 帧并置
   `END_STREAM`（`Http2HttpContext.java:202`）。两处都能直接换成携带尾部字段的版本，不需要新生命周期阶段。
4. **文件发送不会提前终止响应**：`sendFile()` 走 `ChunkedNioFile` 或 `DefaultFileRegion`，都不发送终止消息；`Http2DataFrameChunkedInput` 明确保持 `endStream=false`，由 `endResponse()` 补终止（
   `Http2HttpContext.java:249`）。#374 中「文件包装器可能提前产生终止消息」的担忧在当前实现下不成立，文件路径无需为此改动。
5. **`Content-Length` 自动推断是全局性的**：`writeHeaders()` 在非 SSE、非 chunked 分支里按 `responseBody.readableBytes()` 或 `0` 写入 `CONTENT_LENGTH`，该逻辑对 HTTP/1.1 与 HTTP/2 同样生效（
   `supportsTransferEncoding()` 只影响 SSE 分支）。有 Trailer 时必须显式跳过。
6. **`reset()` 是清理点**：`sendError()` 先调用 `reset()`。只要 `reset()` 清空 Trailer 配置，错误与重定向路径就不会残留成功 Trailer。

## 决策

### 接口形态：`ResponseTrailer` 绑定名称与求值

```java
void setResponseTrailers(ResponseTrailer... trailers)
```

`ResponseTrailer` 是 `infra.http` 中的不可变 record，包含字段名和 `Supplier<String>` 值供应器：

```java
context.setResponseTrailers(
    ResponseTrailer.of("Content-Digest", () -> digest.value()));
```

- 名称随注册一起声明，传输层不需要再单独接收名称集合。
- 注册多个同名字段会产生多个值。
- supplier 返回 `null` 表示本次不发送该字段；所有字段都返回 `null` 时不发送尾部字段。
- `HttpHeaders` 退回实现内部，仅用于最终写帧，不再出现在公共参数中。

### 决策 1：注册窗口截止于响应头提交之前

`setResponseTrailers` 只在 `isCommitted()` 为 `false` 时接受注册；提交后（包括 `COMMITTING` 回调内）抛 `IllegalStateException`。

- **理由**：`COMMITTING` 触发时 committed 标记已置位，状态已经脱离「可配置」区间。允许在 `COMMITTING` 内注册会让契约出现一个无法用 `isCommitted()` 解释的例外，也迫使实现额外维护一个 `headersWritten`
  标志。禁止它让规则保持单一。
- **实现落点**：`AbstractHttpContext` 保存配置前调用 `assertNotCommitted()`。

### 决策 2：HTTP/1.1 不做 `TE: trailers` 强制协商

Trailer 的发送以 chunked 定界为准，不要求请求携带 `TE: trailers`。

- **理由**：RFC 9110 允许服务器发送 trailer，`TE` 只表示客户端愿意接收。实践中主流客户端和代理即使不发 `TE` 也能读取 chunked trailer；强制协商会让多数浏览器拿不到 `Content-Digest`，使这个能力失去主要用例。
- **代价**：必须在使用文档中说明「尽力而为」——代理可能剥离尾部字段，客户端也可能忽略。注册成功只描述本端具备发送能力，不承诺端到端送达。
- **实现落点**：`writeHeaders()` 在存在 Trailer 配置时强制 `Transfer-Encoding: chunked` 并移除 `Content-Length`，与现有 SSE 分支同构。

### 决策 3：有 Trailer 时不发送 `Content-Length`

存在 Trailer 配置时，HTTP/1.1 与 HTTP/2 都不写 `Content-Length`，并同步清空 `responseContentLength` 缓存。

- **理由**：Trailer 的前提是正文长度在响应头阶段不可知。保留一个占位长度会让 `getContentLength()`（消息转换器、文件发送都会读）拿到脏值，迫使其逐个特判。统一省略比「调用方手动设置就报错」更简单，也更符合
  chunked 与 HTTP/2 的语义。
- **实现落点**：在 `writeHeaders()` 的长度推断分支前判断 Trailer 配置，命中时跳过整段推断；注册 Trailer 时若已有 `responseContentLength`，一并复位为 `-1`。
- **冲突策略**：调用方手动设置的 `Trailer` 响应头被注册的字段集合 **覆盖**，不合并。两份声明不一致时以注册信息为准，避免客户端看到未承诺的字段名。

### 决策 4：禁止字段采用黑名单 + CR/LF 校验

注册时按大小写不敏感校验并去重；发送时拒绝以下字段，注册阶段能判定的直接抛 `IllegalArgumentException`：

- 定界与连接控制：`Content-Length`、`Transfer-Encoding`、`Connection`、`Keep-Alive`、`TE`、`Upgrade`、`Proxy-Connection`、`Trailer`
- 路由与认证：`Host`、`Authorization`、`WWW-Authenticate`、`Proxy-Authenticate`、`Proxy-Authorization`
- 内容语义：`Content-Encoding`、`Content-Type`、`Content-Range`、`Content-Location`
- 响应控制：`Set-Cookie`、`Cache-Control`、`Expires`、`Vary`

值校验拒绝 `CR`、`LF`、`NUL`，其余字符集交给 Netty 的头部编码器。

- **理由**：RFC 9110 §6.5.1 把 trailer 限定为「正文处理完成后才确定的元数据」。上面这些字段要么必须在头部阶段生效（定界、认证、缓存），要么与正文语义强绑定，放进 trailer 会被中间件忽略或误用。黑名单比白名单更能容纳
  `Content-Digest`、`ETag`、`Server-Timing` 这类未来字段。
- **实现落点**：校验放在 `AbstractHttpContext` 的注册方法内，保证各适配器与 mock 行为一致。

### 决策 5：失败即终止，不重试、不伪装成功

- supplier 抛异常或产生非法值：不重试，不再生成成功 Trailer。
- supplier 返回 `null`：只跳过该字段；同一次注册中的其他字段仍可正常发送。
- HTTP/1.1：关闭连接，避免客户端等待永不到来的终止 chunk。
- HTTP/2：以 `RST_STREAM` 终止对应流，保留可诊断信息。
- 正文写入失败或连接断开：跳过 Trailer 求值（`requestCompletedInternal` 的 `notHandled != null` 分支已具备这一行为）。
- supplier 在 `requestCompletedInternal` 的调用线程上执行，必须是非阻塞的；求值时机是「终止消息排入管线之前」，不代表网络已送达客户端。

- **理由**：Trailer 的语义是「本次响应确实完整产生」。异常后补发一个部分字段会让客户端误判正文完整性，破坏 `Content-Digest` 这类校验用途。快速终止比模糊的成功更安全。

### 不支持路径

以下情况在注册时即拒绝，不静默接受：

- 适配器内部判定当前协议不具备发送能力（如 HTTP/1.0）→ `UnsupportedOperationException`
- `HEAD` 请求 → `IllegalStateException`
- 无正文状态（`1xx`、`204`、`304`）→ `IllegalStateException`
- 协议升级与成功 `CONNECT` → `IllegalStateException`

`flush()` 既不结束响应也不触发 supplier；`reset()` 清除配置。

## 不在本 ADR 范围

- 请求 Trailer
- 自动摘要计算（`Content-Digest` 只作为示例）
- gRPC、HTTP/3
- 其他服务端适配器的完整实现
- 消息转换器对 Trailer 的感知

## 后果

- `HttpContext` 新增 `setResponseTrailers(ResponseTrailer...)` 默认方法，默认抛 `UnsupportedOperationException`；是否具备发送能力由 `AbstractHttpContext` 的 protected `isResponseTrailersSupported()` 判定，已有实现无需改动即可编译。
- `AbstractHttpContext` 承担全部配置状态、校验与至多一次求值；`AbstractDelegatingHttpContext` 与 `DelegatingHttpContext` 纯委托，不另存状态。
- `MockHttpContext` 复用骨架行为，额外暴露最终字段供断言。
- 未配置 Trailer 时，`writeHeaders()`、`endResponse()` 与既有发送路径完全不变。
- 实现需要按 #374 验收项补充契约测试与 HTTP/1.1、HTTP/2 协议级测试。测试重点：注册时机、替换、防御性复制、`reset`、空结果、空值跳过、同名多值、禁止字段、至多一次求值、keep-alive 复用、尾部 HEADERS 顺序、`END_STREAM` 唯一性。
