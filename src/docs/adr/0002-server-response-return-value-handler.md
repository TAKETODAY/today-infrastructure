# ADR-0002 函数式 ServerResponse 统一走 ReturnValueHandler

- 状态：已接受
- 日期：2026-11-15
- 关联：[#377](https://github.com/TAKETODAY/today-infrastructure/issues/377)
- 影响模块：`infra-webmvc`、`integration-tests`

## 背景

函数式 Web 的 `ServerResponse` 过去由 `HandlerFunctionAdapter` 直接调用 `writeTo` 写出，MVC 控制器返回值则由 `ReturnValueHandler` 链处理。两套路径各自实现 SSE：

- `SseServerResponse` 创建 `DeferredResult`，通过内部 `DefaultSseBuilder` 直接写输出流并立即 `flush`；
- `ResponseBodyEmitterReturnValueHandler` 初始化 `ResponseBodyEmitter` / `SseEmitter`，通过 `HttpMessageConvertingHandler` 写数据。

同时，异步函数式响应的并发结果本身就是 `ServerResponse`，没有对应的返回值处理器时只能由各个 `ServerResponse` 实现自行写出，形成特例。

## 决策

让 `ServerResponse` 成为一等返回值：

- `HandlerFunctionAdapter` 不再调用 `writeTo`，而是把 `ServerResponse` 交给 `DispatcherHandler`；
- 新增 `ServerResponseReturnValueHandler`：
  - 调用 `ServerResponse.writeTo`；
  - 非 `NONE_RETURN_VALUE` 的返回值继续交给 `ReturnValueHandler` 链（`ModelAndView`、视图名、`DeferredResult`、`ResponseBodyEmitter` 等）；
- `SseServerResponse.writeToInternal` 改为创建 `SseEmitter`，把 `SseBuilder` 适配到 `SseEmitter.SseEventBuilder`，不再自行操作输出流与 `DeferredResult`；
- `DefaultAsyncServerResponse` 只把 future 的结果（包括错误响应）设置为异步结果，由返回值链写出，不再在完成回调里直接写响应。

`ServerResponseReturnValueHandler` 注册在 `ReturnValueHandlerManager` 默认链中，位于 emitter 和 `ResponseEntity` 处理器之前。它不依赖路由绑定的 `ServerRequest`；只有函数式错误回调需要该请求对象。

### 为什么不选方案 A / C

- 方案 A（只让 `SseServerResponse` 返回 `SseEmitter`）改动更小，但异步并发结果和 `WriteFunction` 返回的 `ServerResponse` 仍然无法进入返回值链，问题只解决一半。
- 方案 C（只抽共享写逻辑）不改变返回值模型和写出时序，但会保留两套异步状态机，复用范围有限。

## 行为语义

### 请求读取与响应写出

删除 `ServerResponse.Context`；`ServerResponse.writeTo(HttpContext)` 应用状态码、响应头和 Cookie，并在条件请求命中时短路，不启动写出。`ServerRequest.messageConverters()` 只负责请求读取。标准 MVC 装配仍从同一配置注入请求侧和响应侧，但响应写出不再依赖路由映射的转换器配置。

- 普通实体把值和显式类型交给 MVC 消息写出逻辑，复用内容协商、转换器选择和 Range 处理，不经由 `ResponseEntity` 二次应用响应元数据。
- `CompletionStage` 实体完成后由异步分发写出；`Publisher` 实体继续逐项写出和刷新。
- `stream(...)` 由返回值处理侧传入转换器，再启动 `StreamBuilder`，保留 `write()` 与显式 `flush()` 的区别。
- SSE 继续由 `ResponseBodyEmitterReturnValueHandler` 写出，复用其转换器和异步生命周期。

### 写出时机

`SseBuilder.send()` 不再立即写输出流。数据先进入 `SseEmitter` 的待发送队列（`earlySendAttempts`），在同一个 dispatch 内由返回值处理器初始化后批量写出。异常发生在初始化前时，错误结果会被保留，不会写出半截事件。

### 字段与内容格式

| 项目 | 原实现 | 统一后 |
| --- | --- | --- |
| `id` / `event` / `retry` / `comment` | 直接拼接字段 | 与 `SseEmitter.SseEventBuilder` 一致 |
| 字符串数据 | 原样写出，多行拆为多行 `data:` | 相同 |
| 对象数据默认 media type | `APPLICATION_JSON` | 保持 `APPLICATION_JSON`（字符串仍按文本处理） |
| 空 `send()` | 写一个换行 | 不写任何内容 |
| `Content-Type` / charset | `text/event-stream`，无 charset，chunked | 相同；`Content-Type` 与 `Transfer-Encoding` 由 `SseEmitter.extendResponse` 设置，`Cache-Control: no-cache` 在 `writeToInternal` 中设置 |
| 超时 | `DeferredResult` 超时 | `SseEmitter` 超时，由 `ResponseBodyEmitterReturnValueHandler` 创建 `DeferredResult` |
| 完成 / 错误回调 | `DeferredResult` 回调 | `SseEmitter` 回调，最终映射到 `DeferredResult` |

空 `send()` 不写出是唯一有意的输出变化：空事件没有可发送字段，孤立换行没有协议意义。

### 异步与异常

`ServerResponse.async(...)` 的并发结果直接是 `ServerResponse`，异步分发时由 `ServerResponseReturnValueHandler` 写出，不需要在 future 回调线程里直接操作响应。`errorResponse(...)` 返回的错误响应也走同一条路径。

`sseConsumer` 执行期间抛出的异常仍由 `AbstractServerResponse.writeTo` 捕获，交给错误响应处理。`SseBuilder.error(t)` / `complete()` 映射到 `SseEmitter.completeWithError` / `complete`；`ResponseBodyEmitter` 的完成状态取代了 `SseServerResponse` 内部的 `sendFailed` 标志。

### `HandlerFunctionAdapter` 中的死分支

`asyncManager.hasConcurrentResult()` 分支不会执行：异步结果统一由 `DispatcherHandler.handleConcurrentResult` 处理，Netty 与 Mock 都直接调用该方法，不会重新进入 `HandlerAdapter.handle`。该分支一并删除。

## 后果

- `ServerResponse` 与普通控制器返回值共享 `ReturnValueHandler` 链，可被自定义处理器拦截或扩展。
- 函数式 SSE 与 MVC 的 `SseEmitter` 共享初始化、超时、完成和错误处理逻辑。
- `SseServerResponse.writeTo` 的返回值从 `NONE_RETURN_VALUE` 变为 `SseEmitter`；直接调用该方法的代码需要初始化 emitter。
- 自定义 `HandlerFunctionAdapter` 的调用方必须同时配置 `ServerResponseReturnValueHandler`，否则 `ServerResponse` 会因没有处理器而失败；标准 `WebMvcConfigurationSupport` 装配已自动注册。
- 现有测试改为通过返回值处理器链驱动，并新增异步函数式端点与 SSE 的集成测试。
