/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.mock.web.MockAsyncContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.async.AsyncWebRequest;
import cn.taketoday.web.mock.StandardMockAsyncWebRequest;
import cn.taketoday.web.async.WebAsyncManager;
import cn.taketoday.web.mock.MockRequestContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static cn.taketoday.core.ResolvableType.forClassWithGenerics;
import static cn.taketoday.web.ResolvableMethod.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/28 15:37
 */
class ResponseBodyEmitterReturnValueHandlerTests {

  private ResponseBodyEmitterReturnValueHandler handler;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  private MockRequestContext webRequest;

  @BeforeEach
  public void setup() throws Exception {

    List<HttpMessageConverter<?>> converters =
            Collections.singletonList(new MappingJackson2HttpMessageConverter());

    this.handler = new ResponseBodyEmitterReturnValueHandler(converters);
    this.request = new HttpMockRequestImpl();
    this.response = new MockHttpResponseImpl();
    this.webRequest = new MockRequestContext(null, this.request, this.response);

    AsyncWebRequest asyncWebRequest = new StandardMockAsyncWebRequest(this.request, this.response);
    webRequest.getAsyncManager().setAsyncRequest(asyncWebRequest);
    this.request.setAsyncSupported(true);
  }

  @Test
  public void supportsHandlerMethods() throws Exception {

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(ResponseBodyEmitter.class))).isTrue();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(SseEmitter.class))).isTrue();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(ResponseEntity.class, ResponseBodyEmitter.class))).isTrue();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(Flux.class, String.class))).isTrue();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(
                    ResolvableType.forClassWithGenerics(ResponseEntity.class, forClassWithGenerics(Flux.class, String.class))))).isTrue();
  }

  @Test
  public void doesNotSupportReturnTypes() throws Exception {

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(ResponseEntity.class, String.class))).isFalse();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(ResolvableType.forClassWithGenerics(ResponseEntity.class,
                    forClassWithGenerics(AtomicReference.class, String.class))))).isFalse();

    assertThat(this.handler.supportsHandler(
            on(TestController.class).resolveHandlerMethod(ResponseEntity.class))).isFalse();
  }

  @Test
  public void responseBodyEmitter() throws Exception {
    HandlerMethod handlerMethod = on(TestController.class).resolveHandlerMethod(ResponseBodyEmitter.class);
    ResponseBodyEmitter emitter = new ResponseBodyEmitter();
    this.handler.handleReturnValue(webRequest, handlerMethod, emitter);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("");

    SimpleBean bean = new SimpleBean();
    bean.setId(1L);
    bean.setName("Joe");
    emitter.send(bean);
    emitter.send("\n");

    bean.setId(2L);
    bean.setName("John");
    emitter.send(bean);
    emitter.send("\n");

    bean.setId(3L);
    bean.setName("Jason");
    emitter.send(bean);

    assertThat(this.response.getContentAsString()).isEqualTo(("{\"id\":1,\"name\":\"Joe\"}\n" +
            "{\"id\":2,\"name\":\"John\"}\n" +
            "{\"id\":3,\"name\":\"Jason\"}"));

    MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
    assertThat(asyncContext.getDispatchedPath()).isNull();

    emitter.complete();
    assertThat(asyncContext.getDispatchedPath()).isNotNull();
  }

  @Test
  public void responseBodyEmitterWithTimeoutValue() throws Exception {
    AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
    webRequest.getAsyncManager().setAsyncRequest(asyncWebRequest);

    ResponseBodyEmitter emitter = new ResponseBodyEmitter(19000L);
    emitter.onTimeout(Mockito.mock(Runnable.class));
    emitter.onCompletion(Mockito.mock(Runnable.class));

    HandlerMethod handlerMethod = on(TestController.class).resolveHandlerMethod(ResponseBodyEmitter.class);
    this.handler.handleReturnValue(webRequest, handlerMethod, emitter);

    verify(asyncWebRequest).setTimeout(19000L);
    verify(asyncWebRequest).addTimeoutHandler(ArgumentMatchers.any(Runnable.class));
    verify(asyncWebRequest, times(1)).addCompletionHandler(ArgumentMatchers.any(Runnable.class));
    verify(asyncWebRequest).startAsync();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void responseBodyEmitterWithErrorValue() throws Exception {

    AsyncWebRequest asyncWebRequest = mock(AsyncWebRequest.class);
    webRequest.getAsyncManager().setAsyncRequest(asyncWebRequest);

    ResponseBodyEmitter emitter = new ResponseBodyEmitter(19000L);
    emitter.onError(Mockito.mock(Consumer.class));
    emitter.onCompletion(Mockito.mock(Runnable.class));

    HandlerMethod type = on(TestController.class).resolveHandlerMethod(ResponseBodyEmitter.class);
    this.handler.handleReturnValue(webRequest, type, emitter);

    verify(asyncWebRequest).addErrorHandler(ArgumentMatchers.any(Consumer.class));
    verify(asyncWebRequest, times(1)).addCompletionHandler(ArgumentMatchers.any(Runnable.class));
    verify(asyncWebRequest).startAsync();
  }

  @Test
  public void sseEmitter() throws Exception {
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(SseEmitter.class);
    SseEmitter emitter = new SseEmitter();
    this.handler.handleReturnValue(webRequest, type, emitter);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);

    SimpleBean bean1 = new SimpleBean();
    bean1.setId(1L);
    bean1.setName("Joe");

    SimpleBean bean2 = new SimpleBean();
    bean2.setId(2L);
    bean2.setName("John");

    emitter.send(SseEmitter.event().
            comment("a test").name("update").id("1").reconnectTime(5000L).data(bean1).data(bean2));

    assertThat(this.response.getContentType()).isEqualTo("text/event-stream");
    assertThat(this.response.getContentAsString()).isEqualTo((":a test\n" +
            "event:update\n" +
            "id:1\n" +
            "retry:5000\n" +
            "data:{\"id\":1,\"name\":\"Joe\"}\n" +
            "data:{\"id\":2,\"name\":\"John\"}\n" +
            "\n"));
  }

  @Test
  public void responseBodyFlux() throws Exception {

    this.request.addHeader("Accept", "text/event-stream");

    HandlerMethod type = on(TestController.class).resolveHandlerMethod(Flux.class, String.class);
    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    this.handler.handleReturnValue(webRequest, type, sink.asFlux());

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);

    sink.tryEmitNext("foo");
    sink.tryEmitNext("bar");
    sink.tryEmitNext("baz");
    sink.tryEmitComplete();

    assertThat(this.response.getContentType()).isEqualTo("text/event-stream");
    assertThat(this.response.getContentAsString()).isEqualTo("data:foo\n\ndata:bar\n\ndata:baz\n\n");
  }

  @Test // gh-21972
  public void responseBodyFluxWithError() throws Exception {
    this.request.addHeader("Accept", "text/event-stream");
    IllegalStateException ex = new IllegalStateException("wah wah");
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(Flux.class, String.class);
    this.handler.handleReturnValue(webRequest, type, Flux.error(ex));

    assertThat(this.request.isAsyncStarted()).isTrue();

    WebAsyncManager asyncManager = webRequest.getAsyncManager();
    assertThat(asyncManager.getConcurrentResult()).isSameAs(ex);
    assertThat(this.response.getContentType()).isEqualTo("text/event-stream");
  }

  @Test
  public void responseEntitySse() throws Exception {
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(ResponseEntity.class, SseEmitter.class);
    SseEmitter emitter = new SseEmitter();
    ResponseEntity<SseEmitter> entity = ResponseEntity.ok().header("foo", "bar").body(emitter);
    this.handler.handleReturnValue(webRequest, type, entity);
    emitter.complete();

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getContentType()).isEqualTo("text/event-stream");
    assertThat(this.response.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  public void responseEntitySseNoContent() throws Exception {
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(ResponseEntity.class, SseEmitter.class);
    ResponseEntity<?> entity = ResponseEntity.noContent().header("foo", "bar").build();
    this.handler.handleReturnValue(webRequest, type, entity);

    assertThat(this.request.isAsyncStarted()).isFalse();
    assertThat(this.response.getStatus()).isEqualTo(204);
    assertThat(this.response.getHeaders("foo")).isEqualTo(Collections.singletonList("bar"));
  }

  @Test
  public void responseEntityFlux() throws Exception {

    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseEntity<Flux<String>> entity = ResponseEntity.ok().body(sink.asFlux());
    ResolvableType bodyType = forClassWithGenerics(Flux.class, String.class);
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(ResponseEntity.class, bodyType);
    this.handler.handleReturnValue(webRequest, type, entity);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);

    sink.tryEmitNext("foo");
    sink.tryEmitNext("bar");
    sink.tryEmitNext("baz");
    sink.tryEmitComplete();

    assertThat(this.response.getContentType()).isEqualTo("text/plain");
    assertThat(this.response.getContentAsString()).isEqualTo("foobarbaz");
  }

  @Test // SPR-17076
  public void responseEntityFluxWithCustomHeader() throws Exception {

    Sinks.Many<SimpleBean> sink = Sinks.many().unicast().onBackpressureBuffer();
    ResponseEntity<Flux<SimpleBean>> entity = ResponseEntity.ok().header("x-foo", "bar").body(sink.asFlux());
    ResolvableType bodyType = forClassWithGenerics(Flux.class, SimpleBean.class);
    HandlerMethod type = on(TestController.class).resolveHandlerMethod(ResponseEntity.class, bodyType);
    this.handler.handleReturnValue(webRequest, type, entity);
    webRequest.flush();
    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getHeader("x-foo")).isEqualTo("bar");
    assertThat(this.response.isCommitted()).isFalse();
  }

  @SuppressWarnings("unused")
  private static class TestController {

    private ResponseBodyEmitter h1() { return null; }

    private ResponseEntity<ResponseBodyEmitter> h2() { return null; }

    private SseEmitter h3() { return null; }

    private ResponseEntity<SseEmitter> h4() { return null; }

    private ResponseEntity<String> h5() { return null; }

    private ResponseEntity<AtomicReference<String>> h6() { return null; }

    private ResponseEntity<?> h7() { return null; }

    private Flux<String> h8() { return null; }

    private ResponseEntity<Flux<String>> h9() { return null; }

    private ResponseEntity<Flux<SimpleBean>> h10() { return null; }
  }

  @SuppressWarnings("unused")
  private static class SimpleBean {

    private Long id;

    private String name;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}
