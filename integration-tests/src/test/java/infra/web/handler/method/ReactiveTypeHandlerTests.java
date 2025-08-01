/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import infra.core.MethodParameter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ResolvableType;
import infra.core.task.SyncTaskExecutor;
import infra.http.MediaType;
import infra.http.codec.ServerSentEvent;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.ContentNegotiationManagerFactoryBean;
import infra.web.mock.MockRequestContext;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static infra.core.ResolvableType.forClass;
import static infra.web.ResolvableMethod.on;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/25 10:06
 */
class ReactiveTypeHandlerTests {

  private ReactiveTypeHandler handler;

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  private RequestContext webRequest;

  @BeforeEach
  public void setup() throws Exception {
    ContentNegotiationManagerFactoryBean factoryBean = new ContentNegotiationManagerFactoryBean();
    factoryBean.afterPropertiesSet();
    ContentNegotiationManager manager = factoryBean.getObject();
    ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
    this.handler = new ReactiveTypeHandler(adapterRegistry, new SyncTaskExecutor(), manager);
    resetRequest();
  }

  private void resetRequest() {
    this.mockRequest = new HttpMockRequestImpl();
    this.mockResponse = new MockHttpResponseImpl();
    this.webRequest = new MockRequestContext(null, this.mockRequest, this.mockResponse);

    this.mockRequest.setAsyncSupported(true);
  }

  @Test
  public void supportsType() throws Exception {
    assertThat(this.handler.isReactiveType(Mono.class)).isTrue();
    assertThat(this.handler.isReactiveType(Single.class)).isTrue();
  }

  @Test
  public void doesNotSupportType() throws Exception {
    assertThat(this.handler.isReactiveType(String.class)).isFalse();
  }

  @Test
  void findsConcreteStreamingMediaType() {
    final List<MediaType> accept = List.of(
            MediaType.ALL,
            MediaType.parseMediaType("application/*+x-ndjson"),
            MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

    assertThat(ReactiveTypeHandler.findConcreteStreamingMediaType(accept))
            .isEqualTo(MediaType.APPLICATION_NDJSON);
  }

  @Test
  void findsConcreteStreamingMediaType_vendorFirst() {
    final List<MediaType> accept = List.of(
            MediaType.ALL,
            MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"),
            MediaType.parseMediaType("application/*+x-ndjson"),
            MediaType.APPLICATION_NDJSON);

    assertThat(ReactiveTypeHandler.findConcreteStreamingMediaType(accept))
            .hasToString("application/vnd.myapp.v1+x-ndjson");
  }

  @Test
  void findsConcreteStreamingMediaType_plainNdJsonFirst() {
    final List<MediaType> accept = List.of(
            MediaType.ALL,
            MediaType.APPLICATION_NDJSON,
            MediaType.parseMediaType("application/*+x-ndjson"),
            MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

    assertThat(ReactiveTypeHandler.findConcreteStreamingMediaType(accept))
            .isEqualTo(MediaType.APPLICATION_NDJSON);
  }

  @Test
  void findsConcreteStreamingMediaType_plainStreamingJsonFirst() {
    final List<MediaType> accept = List.of(
            MediaType.ALL,
            MediaType.APPLICATION_STREAM_JSON,
            MediaType.parseMediaType("application/*+x-ndjson"),
            MediaType.parseMediaType("application/vnd.myapp.v1+x-ndjson"));

    assertThat(ReactiveTypeHandler.findConcreteStreamingMediaType(accept))
            .isEqualTo(MediaType.APPLICATION_STREAM_JSON);
  }

  @Test
  public void deferredResultSubscriberWithOneValue() throws Exception {

    // Mono
    Sinks.One<String> sink = Sinks.one();
    testDeferredResultSubscriber(
            sink.asMono(), Mono.class, forClass(String.class),
            () -> sink.emitValue("foo", Sinks.EmitFailureHandler.FAIL_FAST),
            "foo");

    // Mono empty
    Sinks.One<String> emptySink = Sinks.one();
    testDeferredResultSubscriber(
            emptySink.asMono(), Mono.class, forClass(String.class),
            () -> emptySink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST), null);

    // RxJava Single
    AtomicReference<SingleEmitter<String>> ref2 = new AtomicReference<>();
    Single<String> single2 = Single.create(ref2::set);
    testDeferredResultSubscriber(single2, Single.class, forClass(String.class),
            () -> ref2.get().onSuccess("foo"), "foo");
  }

  @Test
  public void deferredResultSubscriberWithNoValues() throws Exception {
    Sinks.One<String> sink = Sinks.one();
    testDeferredResultSubscriber(sink.asMono(), Mono.class, forClass(String.class),
            () -> sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST),
            null);
  }

  @Test
  public void deferredResultSubscriberWithMultipleValues() throws Exception {

    // JSON must be preferred for Flux<String> -> List<String> or else we stream
    this.mockRequest.addHeader("Accept", "application/json");

    Bar bar1 = new Bar("foo");
    Bar bar2 = new Bar("bar");

    Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
    testDeferredResultSubscriber(sink.asFlux(), Flux.class, forClass(Bar.class), () -> {
      sink.tryEmitNext(bar1);
      sink.tryEmitNext(bar2);
      sink.tryEmitComplete();
    }, Arrays.asList(bar1, bar2));
  }

  @Test
  public void deferredResultSubscriberWithError() throws Exception {

    IllegalStateException ex = new IllegalStateException();

    // Mono
    Sinks.One<String> sink = Sinks.one();
    testDeferredResultSubscriber(sink.asMono(), Mono.class, forClass(String.class),
            () -> sink.emitError(ex, Sinks.EmitFailureHandler.FAIL_FAST), ex);

    // RxJava Single
    AtomicReference<SingleEmitter<String>> ref2 = new AtomicReference<>();
    Single<String> single2 = Single.create(ref2::set);
    testDeferredResultSubscriber(single2, Single.class, forClass(String.class),
            () -> ref2.get().onError(ex), ex);
  }

  @Test
  public void mediaTypes() throws Exception {

    // Media type from request
    this.mockRequest.addHeader("Accept", "text/event-stream");
    testSseResponse(true, null);

    // Media type from "produces" attribute
    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(webRequest);
    matchingMetadata.setProducibleMediaTypes(List.of(MediaType.TEXT_EVENT_STREAM));
    webRequest.setMatchingMetadata(matchingMetadata);
    testSseResponse(true, null);

    // Preset media type
    testSseResponse(true, MediaType.TEXT_EVENT_STREAM);

    // No media type preferences
    testSseResponse(false, null);
  }

  private void testSseResponse(boolean expectSseEmitter, @Nullable MediaType contentType) throws Exception {
    ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class, forClass(String.class), contentType);
    Object actual = emitter instanceof SseEmitter;
    assertThat(actual).isEqualTo(expectSseEmitter);
    resetRequest();
  }

  @Test
  public void writeServerSentEvents() throws Exception {

    this.mockRequest.addHeader("Accept", "text/event-stream");
    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    SseEmitter sseEmitter = (SseEmitter) handleValue(sink.asFlux(), Flux.class, forClass(String.class));

    EmitterHandler emitterHandler = new EmitterHandler();
    sseEmitter.initialize(emitterHandler);

    sink.tryEmitNext("foo");
    sink.tryEmitNext("bar");
    sink.tryEmitNext("baz");
    sink.tryEmitComplete();

    assertThat(emitterHandler.getValuesAsText()).isEqualTo("data:foo\n\ndata:bar\n\ndata:baz\n\n");
  }

  @Test
  public void writeServerSentEventsWithBuilder() throws Exception {

    ResolvableType type = ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class);

    Sinks.Many<ServerSentEvent<?>> sink = Sinks.many().unicast().onBackpressureBuffer();
    SseEmitter sseEmitter = (SseEmitter) handleValue(sink.asFlux(), Flux.class, type);

    EmitterHandler emitterHandler = new EmitterHandler();
    sseEmitter.initialize(emitterHandler);

    sink.tryEmitNext(ServerSentEvent.builder("foo").id("1").build());
    sink.tryEmitNext(ServerSentEvent.builder("bar").id("2").build());
    sink.tryEmitNext(ServerSentEvent.builder("baz").id("3").build());
    sink.tryEmitComplete();

    assertThat(emitterHandler.getValuesAsText()).isEqualTo("id:1\ndata:foo\n\nid:2\ndata:bar\n\nid:3\ndata:baz\n\n");
  }

  @Test
  public void writeStreamJson() throws Exception {

    this.mockRequest.addHeader("Accept", "application/x-ndjson");

    Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(Bar.class));

    EmitterHandler emitterHandler = new EmitterHandler();
    emitter.initialize(emitterHandler);

    MockRequestContext requestContext = new MockRequestContext(null, null, mockResponse);
//    ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
    emitter.extendResponse(requestContext);

    Bar bar1 = new Bar("foo");
    Bar bar2 = new Bar("bar");

    sink.tryEmitNext(bar1);
    sink.tryEmitNext(bar2);
    sink.tryEmitComplete();

    assertThat(requestContext.responseHeaders().getContentType().toString()).isEqualTo("application/x-ndjson");
    assertThat(emitterHandler.getValues()).isEqualTo(Arrays.asList(bar1, "\n", bar2, "\n"));
  }

  @Test
  public void writeStreamJsonWithVendorSubtype() throws Exception {
    this.mockRequest.addHeader("Accept", "application/vnd.myapp.v1+x-ndjson");

    Sinks.Many<Bar> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(Bar.class));

    assertThat(emitter).as("emitter").isNotNull();

    EmitterHandler emitterHandler = new EmitterHandler();
    emitter.initialize(emitterHandler);

    MockRequestContext requestContext = new MockRequestContext(null, null, mockResponse);
//    ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
    emitter.extendResponse(requestContext);

    Bar bar1 = new Bar("foo");
    Bar bar2 = new Bar("bar");

    sink.tryEmitNext(bar1);
    sink.tryEmitNext(bar2);
    sink.tryEmitComplete();

    assertThat(requestContext.getResponseContentType()).isEqualTo("application/vnd.myapp.v1+x-ndjson");
    assertThat(emitterHandler.getValues()).isEqualTo(Arrays.asList(bar1, "\n", bar2, "\n"));
  }

  @Test
  public void writeText() throws Exception {

    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(String.class));

    EmitterHandler emitterHandler = new EmitterHandler();
    emitter.initialize(emitterHandler);

    sink.tryEmitNext("The quick");
    sink.tryEmitNext(" brown fox jumps over ");
    sink.tryEmitNext("the lazy dog");
    sink.tryEmitComplete();

    assertThat(emitterHandler.getValuesAsText()).isEqualTo("The quick brown fox jumps over the lazy dog");
  }

  @Test
  void failOnWriteShouldCompleteEmitter() throws Exception {

    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseBodyEmitter emitter = handleValue(sink.asFlux(), Flux.class, forClass(String.class));

    ErroringEmitterHandler emitterHandler = new ErroringEmitterHandler();
    emitter.initialize(emitterHandler);

    sink.tryEmitNext("The quick");
    sink.tryEmitNext(" brown fox jumps over ");
    sink.tryEmitNext("the lazy dog");
    sink.tryEmitComplete();

    assertThat(emitterHandler.getHandlingStatus()).isEqualTo(HandlingStatus.ERROR);
    assertThat(emitterHandler.getFailure()).isInstanceOf(IOException.class);
  }

  @Test
  public void writeFluxOfString() throws Exception {

    // Default to "text/plain"
    testEmitterContentType("text/plain");

    // Same if no concrete media type
    this.mockRequest.addHeader("Accept", "text/*");
    testEmitterContentType("text/plain");

    // Otherwise pick concrete media type
    this.mockRequest.addHeader("Accept", "*/*, text/*, text/markdown");
    testEmitterContentType("text/markdown");

    // Any concrete media type
    this.mockRequest.addHeader("Accept", "*/*, text/*, foo/bar");
    testEmitterContentType("foo/bar");

    // Including json
    this.mockRequest.addHeader("Accept", "*/*, text/*, application/json");
    testEmitterContentType("application/json");
  }

  private void testEmitterContentType(String expected) throws Exception {
//    ServletServerHttpResponse message = new ServletServerHttpResponse(this.servletResponse);
    ResponseBodyEmitter emitter = handleValue(Flux.empty(), Flux.class, forClass(String.class));
    emitter.extendResponse(webRequest);
    assertThat(webRequest.responseHeaders().getContentType().toString()).isEqualTo(expected);
    resetRequest();
  }

  private void testDeferredResultSubscriber(Object returnValue, Class<?> asyncType,
          ResolvableType elementType, Runnable produceTask, Object expected) throws Exception {

    ResponseBodyEmitter emitter = handleValue(returnValue, asyncType, elementType);
    assertThat(emitter).isNull();

    assertThat(this.mockRequest.isAsyncStarted()).isTrue();
    assertThat(webRequest.getAsyncManager().hasConcurrentResult()).isFalse();

    produceTask.run();

    assertThat(webRequest.getAsyncManager().hasConcurrentResult()).isTrue();
    assertThat(webRequest.getAsyncManager().getConcurrentResult()).isEqualTo(expected);

    resetRequest();
  }

  private ResponseBodyEmitter handleValue(Object returnValue, Class<?> asyncType,
          ResolvableType genericType) throws Exception {
    return handleValue(returnValue, asyncType, genericType, null);
  }

  private ResponseBodyEmitter handleValue(Object returnValue, Class<?> asyncType,
          ResolvableType genericType, @Nullable MediaType contentType) throws Exception {

    BindingContext mavContainer = new BindingContext();
    MethodParameter returnType = on(TestController.class).resolveReturnType(asyncType, genericType);
    webRequest.setBinding(mavContainer);
    return this.handler.handleValue(returnValue, returnType, contentType, this.webRequest);
  }

  @SuppressWarnings("unused")
  static class TestController {

    String handleString() { return null; }

    Mono<String> handleMono() { return null; }

    Single<String> handleSingle() { return null; }

    Flux<Bar> handleFlux() { return null; }

    Flux<String> handleFluxString() { return null; }

    Flux<ServerSentEvent<String>> handleFluxSseEventBuilder() { return null; }
  }

  private static class EmitterHandler implements ResponseBodyEmitter.Handler {

    private final List<Object> values = new ArrayList<>();

    private HandlingStatus handlingStatus;

    private Throwable failure;

    public List<?> getValues() {
      return this.values;
    }

    public String getValuesAsText() {
      return this.values.stream().map(Object::toString).collect(Collectors.joining());
    }

    public HandlingStatus getHandlingStatus() {
      return this.handlingStatus;
    }

    public Throwable getFailure() {
      return this.failure;
    }

    @Override
    public void send(Object data, MediaType mediaType) throws IOException {
      this.values.add(data);
    }

    @Override
    public void send(Collection<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
      items.forEach(item -> this.values.add(item.data));
    }

    @Override
    public void complete() {
      this.handlingStatus = HandlingStatus.SUCCESS;
    }

    @Override
    public void completeWithError(Throwable failure) {
      this.handlingStatus = HandlingStatus.ERROR;
      this.failure = failure;
    }

    @Override
    public void onTimeout(Runnable callback) {
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
    }

    @Override
    public void onCompletion(Runnable callback) {
    }
  }

  private enum HandlingStatus {
    SUCCESS, ERROR
  }

  private static class ErroringEmitterHandler extends EmitterHandler {
    @Override
    public void send(Object data, MediaType mediaType) throws IOException {
      throw new IOException();
    }

    @Override
    public void send(Collection<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
      throw new IOException();
    }
  }

  private static class Bar {

    private final String value;

    public Bar(String value) {
      this.value = value;
    }

    @SuppressWarnings("unused")
    public String getValue() {
      return this.value;
    }
  }

}
