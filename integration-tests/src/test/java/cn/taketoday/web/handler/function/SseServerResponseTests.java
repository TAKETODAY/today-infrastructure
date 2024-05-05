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

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;

import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class SseServerResponseTests {

  private MockHttpServletRequest mockRequest;

  private MockHttpServletResponse mockResponse;

  @BeforeEach
  void setUp() {
    this.mockRequest = new MockHttpServletRequest("GET", "https://example.com");
    this.mockRequest.setAsyncSupported(true);
    this.mockResponse = new MockHttpServletResponse();
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

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var requestContext = new ServletRequestContext(null, request, mockResponse);

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

    ServerResponse.Context context = () -> Collections.singletonList(new MappingJackson2HttpMessageConverter());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var requestContext = new ServletRequestContext(null, request, mockResponse);

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

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setPrettyPrint(true);
    ServerResponse.Context context = () -> Collections.singletonList(converter);

    var requestContext = new ServletRequestContext(null, this.mockRequest, mockResponse);

    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    String expected = "data:{\n" +
            "data:  \"name\" : \"John Doe\",\n" +
            "data:  \"age\" : 42\n" +
            "data:}\n" +
            "\n";
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

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var requestContext = new ServletRequestContext(null, request, mockResponse);

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
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAsyncSupported(true);
    var requestContext = new ServletRequestContext(null, request, mockResponse);

    assertThat(response.writeTo(requestContext, context)).isSameAs(ServerResponse.NONE_RETURN_VALUE);

    String expected = "event:custom\n\n";
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
