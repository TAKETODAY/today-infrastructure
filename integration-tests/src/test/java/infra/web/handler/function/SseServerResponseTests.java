/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.web.handler.SelectableReturnValueHandler;
import infra.web.handler.result.ResponseBodyEmitterReturnValueHandler;
import infra.web.handler.result.ServerResponseReturnValueHandler;
import infra.web.handler.result.SseEmitter;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.StandardMockWebAsyncRequest;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SseServerResponse}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class SseServerResponseTests {

  private MockRequest mockRequest;

  private MockResponse mockResponse;

  private MockHttpContext httpContext;

  @BeforeEach
  void setUp() {
    this.mockRequest = new MockRequest("GET", "https://example.com");
    this.mockRequest.setAsyncSupported(true);
    this.mockResponse = new MockResponse();
    this.httpContext = new MockHttpContext(null, this.mockRequest, this.mockResponse);
    this.httpContext.setAsyncRequest(new StandardMockWebAsyncRequest(this.mockRequest, this.mockResponse));
  }

  @Test
  void responseHeadersUseEventStreamAndChunkedFraming() throws Throwable {
    ServerResponse response = ServerResponse.sse(sse -> sse.complete());

    assertThat(response.headers().contains(HttpHeaders.CONTENT_LENGTH)).isFalse();

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM_VALUE);
    assertThat(this.mockResponse.getHeader(HttpHeaders.TRANSFER_ENCODING)).isEqualTo(HttpHeaders.CHUNKED);
    assertThat(this.mockResponse.getHeader(HttpHeaders.CACHE_CONTROL))
            .isEqualTo(CacheControl.noCache().getHeaderValue());
  }

  @Test
  void writeToReturnsSseEmitter() throws Throwable {
    ServerResponse response = ServerResponse.sse(sse -> { });

    Object result = response.writeTo(this.httpContext, Collections::emptyList);
    assertThat(result).isInstanceOf(SseEmitter.class);
  }

  @Test
  void sendString() throws Throwable {
    String body = "foo bar";
    ServerResponse response = sse(sse -> sse.send(body));

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEqualTo("data: " + body + "\n\n");
  }

  @Test
  void sendObject() throws Throwable {
    Person person = new Person("John Doe", 42);
    ServerResponse response = sse(sse -> sse.send(person));
    ServerResponse.Context context = () -> List.of(new JacksonJsonHttpMessageConverter());

    write(response, context);

    String expected = "data: {\"name\":\"John Doe\",\"age\":42}\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendObjectWithPrettyPrint() throws Throwable {
    Person person = new Person("John Doe", 42);
    ServerResponse response = sse(sse -> sse.send(person));

    JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
    ServerResponse.Context context = () -> List.of(converter);

    write(response, context);

    String expected = "data: {\ndata:   \"name\" : \"John Doe\",\ndata:   \"age\" : 42\ndata: }\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void builder() throws Throwable {
    ServerResponse response = sse(sse -> sse.id("id")
            .event("name")
            .comment("comment line 1\ncomment line 2")
            .retry(Duration.ofSeconds(1))
            .data("data"));

    write(response, Collections::emptyList);

    String expected = "id:id\nevent:name\n:comment line 1\n:comment line 2\nretry:1000\ndata: data\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendWithoutData() throws Throwable {
    ServerResponse response = sse(sse -> sse.event("custom").send());

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEqualTo("event:custom\n\n");
  }

  @Test
  void sendStringWithCarriageReturn() throws Throwable {
    String body = "line1\rline2\r\nline3";
    ServerResponse response = sse(sse -> sse.send(body));

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEqualTo("data: line1\ndata: line2\ndata: line3\n\n");
  }

  @Test
  void commentWithCarriageReturn() throws Throwable {
    ServerResponse response = sse(sse -> sse.comment("line1\rline2").send());

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEqualTo(":line1\n:line2\n\n");
  }

  @Test
  void sendHeartbeat() throws Throwable {
    ServerResponse response = sse(sse -> sse.comment("").send());

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEqualTo(":\n\n");
  }

  @Test
  void emptySendWritesNothing() throws Throwable {
    ServerResponse response = sse(sse -> sse.send());

    write(response, Collections::emptyList);

    assertThat(this.mockResponse.getContentAsString()).isEmpty();
  }

  private static ServerResponse sse(SseConsumer consumer) {
    return ServerResponse.sse(sse -> {
      try {
        consumer.accept(sse);
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });
  }

  /**
   * Write the response through the same handler chain as {@code DispatcherHandler}.
   */
  private void write(ServerResponse response, ServerResponse.Context context) throws Exception {
    List<HttpMessageConverter<?>> converters = context.messageConverters();
    if (converters.isEmpty()) {
      converters = List.of(new StringHttpMessageConverter());
    }

    var emitterHandler = new ResponseBodyEmitterReturnValueHandler(converters);
    var handler = new ServerResponseReturnValueHandler(
            new SelectableReturnValueHandler(List.of(emitterHandler)));
    this.httpContext.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE,
            ServerRequest.create(this.httpContext, converters));
    handler.handleReturnValue(this.httpContext, null, response);
  }

  @FunctionalInterface
  private interface SseConsumer {

    void accept(ServerResponse.SseBuilder builder) throws IOException;
  }

  private static final class Person {

    private final String name;

    private final int age;

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @SuppressWarnings("unused")
    public String getName() {
      return this.name;
    }

    @SuppressWarnings("unused")
    public int getAge() {
      return this.age;
    }
  }

}
