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

import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class SseServerResponseTests {

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  @BeforeEach
  void setUp() {
    this.mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    this.mockRequest.setAsyncSupported(true);
    this.mockResponse = new MockHttpResponseImpl();
  }

  @Test
  void sendString() throws Throwable {
    String body = "foo bar";
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.send(body);
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    ServerResponse.Context context = Collections::emptyList;

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncSupported(true);
    var requestContext = new MockRequestContext(null, request, mockResponse);

    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    String expected = "data:" + body + "\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendObject() throws Throwable {
    Person person = new Person("John Doe", 42);
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.send(person);
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    ServerResponse.Context context = () -> Collections.singletonList(new JacksonJsonHttpMessageConverter());

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncSupported(true);
    var requestContext = new MockRequestContext(null, request, mockResponse);

    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    String expected = "data:{\"name\":\"John Doe\",\"age\":42}\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendObjectWithPrettyPrint() throws Throwable {
    Person person = new Person("John Doe", 42);
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.send(person);
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
    ServerResponse.Context context = () -> Collections.singletonList(converter);

    var requestContext = new MockRequestContext(null, this.mockRequest, mockResponse);

    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    String expected = """
            data:{
            data:  "name" : "John Doe",
            data:  "age" : 42
            data:}
            
            """;
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void builder() throws Throwable {
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.id("id")
                .event("name")
                .comment("comment line 1\ncomment line 2")
                .retry(Duration.ofSeconds(1))
                .data("data");
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    ServerResponse.Context context = Collections::emptyList;

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncSupported(true);
    var requestContext = new MockRequestContext(null, request, mockResponse);

    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    String expected = """
            id:id
            event:name
            :comment line 1
            :comment line 2
            retry:1000
            data:data
            
            """;
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendWithoutData() throws Throwable {
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.event("custom").send();
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    ServerResponse.Context context = Collections::emptyList;
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAsyncSupported(true);
    var requestContext = new MockRequestContext(null, request, mockResponse);

    assertThat(response.writeTo(requestContext, context)).isSameAs(ServerResponse.NONE_RETURN_VALUE);

    String expected = "event:custom\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
  }

  @Test
  void sendHeartbeat() throws Throwable {
    ServerResponse response = ServerResponse.sse(sse -> {
      try {
        sse.comment("").send();
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });

    ServerResponse.Context context = Collections::emptyList;
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    assertThat(response.writeTo(requestContext, context)).isSameAs(ServerResponse.NONE_RETURN_VALUE);

    String expected = ":\n\n";
    assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
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
