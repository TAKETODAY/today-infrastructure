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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.async.AsyncWebRequest;
import cn.taketoday.web.servlet.StandardServletAsyncWebRequest;
import cn.taketoday.web.handler.StreamingResponseBody;
import cn.taketoday.web.handler.result.StreamingResponseBodyReturnValueHandler;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class StreamingResponseBodyReturnValueHandlerTests {

  private StreamingResponseBodyReturnValueHandler handler;

  private RequestContext webRequest;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new StreamingResponseBodyReturnValueHandler();

    this.request = new MockHttpServletRequest("GET", "/path");
    this.response = new MockHttpServletResponse();
    this.webRequest = new ServletRequestContext(null, this.request, this.response);

    AsyncWebRequest asyncWebRequest = new StandardServletAsyncWebRequest(this.request, this.response);
    webRequest.getAsyncManager().setAsyncRequest(asyncWebRequest);
    this.request.setAsyncSupported(true);
  }

  @Test
  public void supportsReturnType() throws Exception {
//    assertThat(this.handler.supportsReturnValue(returnType(TestController.class, "handle"))).isTrue();
//    assertThat(this.handler.supportsReturnValue(returnType(TestController.class, "handleResponseEntity"))).isTrue();
//    assertThat(this.handler.supportsReturnValue(returnType(TestController.class, "handleResponseEntityString"))).isFalse();
//    assertThat(this.handler.supportsReturnValue(returnType(TestController.class, "handleResponseEntityParameterized"))).isFalse();
  }

  @Test
  public void streamingResponseBody() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    MethodParameter returnType = returnType(TestController.class, "handle");
    StreamingResponseBody streamingBody = outputStream -> {
      outputStream.write("foo".getBytes(StandardCharsets.UTF_8));
      latch.countDown();
    };
    this.handler.handleReturnValue(webRequest, returnType, streamingBody);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("foo");
  }

  @Test
  public void responseEntity() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
    ResponseEntity<StreamingResponseBody> emitter = ResponseEntity.ok().header("foo", "bar")
            .body(outputStream -> {
              outputStream.write("foo".getBytes(StandardCharsets.UTF_8));
              latch.countDown();
            });
    this.handler.handleReturnValue(this.webRequest, returnType, emitter);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(200);
    assertThat(this.response.getHeader("foo")).isEqualTo("bar");

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("foo");

  }

  @Test
  public void responseEntityNoContent() throws Exception {
    MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
    ResponseEntity<?> emitter = ResponseEntity.noContent().build();
    this.handler.handleReturnValue(webRequest, returnType, emitter);

    assertThat(this.request.isAsyncStarted()).isFalse();
    assertThat(this.response.getStatus()).isEqualTo(204);
  }

  @Test
  public void responseEntityWithHeadersAndNoContent() throws Exception {
    ResponseEntity<?> emitter = ResponseEntity.noContent().header("foo", "bar").build();
    MethodParameter returnType = returnType(TestController.class, "handleResponseEntity");
    this.handler.handleReturnValue(webRequest, returnType, emitter);
    webRequest.requestCompleted();
    assertThat(this.response.getHeaders("foo")).isEqualTo(Collections.singletonList("bar"));
  }

  private MethodParameter returnType(Class<?> clazz, String methodName) throws NoSuchMethodException {
    Method method = clazz.getDeclaredMethod(methodName);
    return new MethodParameter(method, -1);
  }

  @SuppressWarnings("unused")
  private static class TestController {

    private StreamingResponseBody handle() {
      return null;
    }

    private ResponseEntity<StreamingResponseBody> handleResponseEntity() {
      return null;
    }

    private ResponseEntity<String> handleResponseEntityString() {
      return null;
    }

    private ResponseEntity<AtomicReference<String>> handleResponseEntityParameterized() {
      return null;
    }
  }

}
