# ADR-0002 函数式 SSE 复用 SseEmitter

- 状态：已接受
- 日期：2026-11-15
- 关联：[#377](https://github.com/TAKETODAY/today-infrastructure/issues/377)
- 影响模块：`infra-webmvc`、`integration-tests`

## 背景

函数式 Web 的 `ServerResponse.sse(...)` 与 MVC 的 `SseEmitter` 各自实现了一套 SSE 写出逻辑：

- `SseServerResponse` 在 `writeToInternal` 中创建 `DeferredResult`，通过内部 `DefaultSseBuilder` 直接写输出流并立即 `flush`；
- `ResponseBodyEmitterReturnValueHandler` 负责初始化 `ResponseBodyEmitter` / `SseEmitter`，通过 `HttpMessageConvertingHandler` 写数据。

两套逻辑在字段格式、超时、完成/错误回调和 `HttpMessageConverter` 使用上高度重合。本 ADR 记录采用的收敛方式。

## 决策

采用**方案 A**：`SseServerResponse` 返回 `SseEmitter`，直接复用已有的 `ResponseBodyEmitterReturnValueHandler`。

- `SseServerResponse.writeToInternal` 创建 `SseEmitter`，把 `SseBuilder` 适配到 `SseEmitter.SseEventBuilder`，写出状态和响应头后返回 emitter；
- `DispatcherHandler` 收到非 `NONE_RETURN_VALUE` 的返回值后，通过现有 `ReturnValueHandler` 链选中 `ResponseBodyEmitterReturnValueHandler`；
- 不新增 `ReturnValueHandler`，普通 `ServerResponse` 仍由 `HandlerFunctionAdapter` 直接调用 `writeTo` 写出。

### 为什么不选 B / C

- 方案 B（新增 `ServerResponseReturnValueHandler`）会让所有 `ServerResponse` 进入返回值链，但需要额外处理 `writeTo` 的二次返回值、`ServerRequest` 获取和 `NONE_RETURN_VALUE` 传递，影响面明显大于问题本身；本议题的目标是消除重复的 SSE 写出逻辑，不是改造整个返回值模型。
- 方案 C（只抽共享写逻辑）不改变返回值模型，但会保留 `DeferredResult` 与 `SseEmitter` 两套异步状态机，复用范围有限。

## 行为语义

### `Context.messageConverters()`

SSE 分支改由 `ResponseBodyEmitterReturnValueHandler` 使用其构造时注入的转换器。标准 MVC 装配下，`RouterFunctionMapping` 与 `ReturnValueHandlerManager` 使用同一份 `getMessageConverters()` 列表，因此与普通控制器及原有 `ServerResponse.sse` 的转换行为一致；直接调用 `writeTo(ctx, context)` 时，`Context.messageConverters()` 不再参与 SSE 数据转换，调用方需要自行初始化返回的 `SseEmitter`。

### 写出时机

`SseBuilder.send()` 不再立即写输出流。数据先进入 `SseEmitter` 的待发送队列（`earlySendAttempts`），在同一个 dispatch 内由 `ResponseBodyEmitterReturnValueHandler` 初始化后批量写出。函数式处理器的初始化紧跟在 `writeTo` 之后，实际可见时序基本不变。

### 字段与内容格式

| 项目 | 原实现 | 复用 `SseEmitter` 后 |
| --- | --- | --- |
| `id` / `event` / `retry` / `comment` | 直接拼接字段 | 与 `SseEmitter.SseEventBuilder` 一致 |
| 字符串数据 | 原样写出，多行拆为多行 `data:` | 相同 |
| 对象数据默认 media type | `APPLICATION_JSON` | 保持 `APPLICATION_JSON`（字符串仍按文本处理） |
| 空 `send()` | 写一个换行 | 不写任何内容 |
| `Content-Type` / charset | `text/event-stream`，无 charset，chunked | 相同；`SseEmitter.extendResponse` 不覆盖已设置的响应头 |
| 超时 | `DeferredResult` 超时 | `SseEmitter` 超时，由返回值处理器创建 `DeferredResult` |
| 完成 / 错误回调 | `DeferredResult` 回调 | `SseEmitter` 回调，最终映射到 `DeferredResult` |

空 `send()` 不写出是唯一有意的输出变化：空事件没有可发送字段，孤立换行没有协议意义。

### 异常

`sseConsumer` 执行期间抛出的异常仍由 `AbstractServerResponse.writeTo` 捕获，交给错误响应处理。`SseBuilder.error(t)` / `complete()` 映射到 `SseEmitter.completeWithError` / `complete`；`ResponseBodyEmitter` 的完成状态取代了 `SseServerResponse` 内部的 `sendFailed` 标志。异常发生在 `SseEmitter` 初始化前时，错误结果会被保留，不会写出半截事件。

## 附带修正

`HandlerFunctionAdapter` 中的 `asyncManager.hasConcurrentResult()` 分支不会执行：异步结果统一由 `DispatcherHandler.handleConcurrentResult` 处理，Netty 与 Mock 都直接调用该方法，不会重新进入 `HandlerAdapter.handle`。该分支及其 `handleAsync` 方法一并删除。

## 后果

- 函数式 SSE 与 MVC 的 `SseEmitter` 共享初始化、超时、完成和错误处理逻辑，`SseServerResponse` 不再直接操作输出流和 `DeferredResult`。
- `SseServerResponse.writeTo` 的返回值从 `NONE_RETURN_VALUE` 变为 `SseEmitter`；直接调用该方法的代码需要初始化 emitter。
- 普通 `ServerResponse` 的返回值处理模型不变，不新增 `ReturnValueHandler`。
- `SseServerResponseTests` 改为先取得 emitter，再交给 `ResponseBodyEmitterReturnValueHandler` 写出；新增函数式端点集成测试覆盖完整 `DispatcherHandler` 路径。
