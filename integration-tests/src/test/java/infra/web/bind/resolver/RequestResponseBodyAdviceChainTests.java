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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.HttpInputMessage;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.server.MockServerHttpRequest;
import infra.http.server.MockServerHttpResponse;
import infra.http.server.ServerHttpRequest;
import infra.http.server.ServerHttpResponse;
import org.jspecify.annotations.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Controller;
import infra.util.ReflectionUtils;
import infra.web.RequestContext;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ResponseBody;
import infra.web.handler.method.ControllerAdviceBean;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResponseBodyAdvice;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 22:33
 */
class RequestResponseBodyAdviceChainTests {

  private String body;

  private MediaType contentType;

  private final StringHttpMessageConverter converterType = new StringHttpMessageConverter();

  private MethodParameter paramType;
  private MethodParameter returnType;

  private ServerHttpRequest request;
  private ServerHttpResponse response;
  private MockRequestContext requestContext;

  @BeforeEach
  public void setup() {
    this.body = "body";
    this.contentType = MediaType.TEXT_PLAIN;
    this.paramType = new MethodParameter(ReflectionUtils.getMethod(this.getClass(), "handle", String.class), 0);
    this.returnType = new MethodParameter(ReflectionUtils.getMethod(this.getClass(), "handle", String.class), -1);

    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl();
    this.request = new MockServerHttpRequest(servletRequest);

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();

    this.response = new MockServerHttpResponse(servletResponse);
    this.requestContext = new MockRequestContext(null, servletRequest, servletResponse);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void requestBodyAdvice() throws IOException {
    RequestBodyAdvice requestAdvice = mock(RequestBodyAdvice.class);
    ResponseBodyAdvice<String> responseAdvice = mock(ResponseBodyAdvice.class);
    List<Object> advice = Arrays.asList(requestAdvice, responseAdvice);
    RequestResponseBodyAdviceChain chain = new RequestResponseBodyAdviceChain(advice);

    HttpInputMessage wrapped = new MockServerHttpRequest(new HttpMockRequestImpl());
    given(requestAdvice.supports(this.paramType, String.class, this.converterType)).willReturn(true);
    given(requestAdvice.beforeBodyRead(ArgumentMatchers.eq(this.request), ArgumentMatchers.eq(this.paramType), ArgumentMatchers.eq(String.class),
            ArgumentMatchers.eq(this.converterType))).willReturn(wrapped);

    assertThat(chain.beforeBodyRead(this.request, this.paramType, String.class, this.converterType)).isSameAs(wrapped);

    String modified = "body++";
    given(requestAdvice.afterBodyRead(ArgumentMatchers.eq(this.body), ArgumentMatchers.eq(this.request), ArgumentMatchers.eq(this.paramType),
            ArgumentMatchers.eq(String.class), ArgumentMatchers.eq(this.converterType))).willReturn(modified);

    assertThat(chain.afterBodyRead(this.body, this.request, this.paramType,
            String.class, this.converterType)).isEqualTo(modified);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void responseBodyAdvice() {
    RequestBodyAdvice requestAdvice = mock(RequestBodyAdvice.class);
    ResponseBodyAdvice<String> responseAdvice = mock(ResponseBodyAdvice.class);
    List<Object> advice = Arrays.asList(requestAdvice, responseAdvice);
    RequestResponseBodyAdviceChain chain = new RequestResponseBodyAdviceChain(advice);

    String expected = "body++";
    given(responseAdvice.supports(body, this.returnType, this.converterType)).willReturn(true);
    given(responseAdvice.beforeBodyWrite(ArgumentMatchers.eq(this.body), ArgumentMatchers.eq(this.returnType), ArgumentMatchers.eq(this.contentType),
            ArgumentMatchers.eq(this.converterType), ArgumentMatchers.same(requestContext))).willReturn(expected);

    String actual = (String) chain.beforeBodyWrite(this.body, this.returnType, this.contentType,
            this.converterType, requestContext);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void controllerAdvice() {
    Object adviceBean = new ControllerAdviceBean(new MyControllerAdvice());
    RequestResponseBodyAdviceChain chain = new RequestResponseBodyAdviceChain(Collections.singletonList(adviceBean));

    String actual = (String) chain.beforeBodyWrite(this.body, this.returnType, this.contentType,
            this.converterType, requestContext);

    assertThat(actual).isEqualTo("body-MyControllerAdvice");
  }

  @Test
  public void controllerAdviceNotApplicable() {
    Object adviceBean = new ControllerAdviceBean(new TargetedControllerAdvice());
    RequestResponseBodyAdviceChain chain = new RequestResponseBodyAdviceChain(Collections.singletonList(adviceBean));

    String actual = (String) chain.beforeBodyWrite(this.body, this.returnType, this.contentType,
            this.converterType, requestContext);

    assertThat(actual).isEqualTo(this.body);
  }

  @ControllerAdvice
  private static class MyControllerAdvice implements ResponseBodyAdvice<String> {

    @Override
    public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
      return true;
    }

    @Nullable
    @Override
    public String beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
            MediaType contentType, HttpMessageConverter<?> selected, RequestContext context) {
      return body + "-MyControllerAdvice";
    }
  }

  @ControllerAdvice(annotations = Controller.class)
  private static class TargetedControllerAdvice implements ResponseBodyAdvice<String> {

    @Override
    public boolean supports(@Nullable Object body,
            @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
      return true;
    }

    @Nullable
    @Override
    public String beforeBodyWrite(
            @Nullable Object body, MethodParameter returnType,
            MediaType contentType, HttpMessageConverter<?> selected, RequestContext context) {
      return body + "-TargetedControllerAdvice";
    }
  }

  @SuppressWarnings("unused")
  @ResponseBody
  public String handle(String body) {
    return "";
  }

}
