/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.server.ServerHttpRequest;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.http.server.ServletServerHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.handler.method.ControllerAdviceBean;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;

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
  private ServletRequestContext requestContext;

  @BeforeEach
  public void setup() {
    this.body = "body";
    this.contentType = MediaType.TEXT_PLAIN;
    this.paramType = new MethodParameter(ReflectionUtils.getMethod(this.getClass(), "handle", String.class), 0);
    this.returnType = new MethodParameter(ReflectionUtils.getMethod(this.getClass(), "handle", String.class), -1);

    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl();
    this.request = new ServletServerHttpRequest(servletRequest);

    MockHttpServletResponse servletResponse = new MockHttpServletResponse();

    this.response = new ServletServerHttpResponse(servletResponse);
    this.requestContext = new ServletRequestContext(null, servletRequest, servletResponse);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void requestBodyAdvice() throws IOException {
    RequestBodyAdvice requestAdvice = mock(RequestBodyAdvice.class);
    ResponseBodyAdvice<String> responseAdvice = mock(ResponseBodyAdvice.class);
    List<Object> advice = Arrays.asList(requestAdvice, responseAdvice);
    RequestResponseBodyAdviceChain chain = new RequestResponseBodyAdviceChain(advice);

    HttpInputMessage wrapped = new ServletServerHttpRequest(new HttpMockRequestImpl());
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
            MediaType contentType, HttpMessageConverter<?> converter, RequestContext context) {
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
            MediaType contentType, HttpMessageConverter<?> converter, RequestContext context) {
      return body + "-TargetedControllerAdvice";
    }
  }

  @SuppressWarnings("unused")
  @ResponseBody
  public String handle(String body) {
    return "";
  }

}
