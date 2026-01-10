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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.MethodParameter;
import infra.http.ResponseEntity;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.async.AsyncWebRequest;
import infra.web.handler.StreamingResponseBody;
import infra.web.handler.result.StreamingResponseBodyReturnValueHandler;
import infra.web.mock.MockRequestContext;
import infra.web.mock.StandardMockAsyncWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class StreamingResponseBodyReturnValueHandlerTests {

  private StreamingResponseBodyReturnValueHandler handler;

  private MockRequestContext webRequest;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new StreamingResponseBodyReturnValueHandler();

    this.request = new HttpMockRequestImpl("GET", "/path");
    this.response = new MockHttpResponseImpl();
    this.webRequest = new MockRequestContext(null, this.request, this.response);

    AsyncWebRequest asyncWebRequest = new StandardMockAsyncWebRequest(this.request, this.response);
    webRequest.setAsyncRequest(asyncWebRequest);
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
