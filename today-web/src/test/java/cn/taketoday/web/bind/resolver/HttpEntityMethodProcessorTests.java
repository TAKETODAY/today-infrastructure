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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.filter.ShallowEtagHeaderFilter;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 10:36
 */
class HttpEntityMethodProcessorTests {

  private ResolvableMethodParameter paramList;

  private ResolvableMethodParameter paramSimpleBean;

  private MockHttpServletRequest servletRequest;

  private ServletRequestContext webRequest;

  private MockHttpServletResponse servletResponse;

  @BeforeEach
  public void setup() throws Exception {
    Method method = getClass().getDeclaredMethod("handle", HttpEntity.class, HttpEntity.class);
    paramList = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramSimpleBean = new ResolvableMethodParameter(new MethodParameter(method, 1));

    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    servletRequest.setMethod("POST");
    webRequest = new ServletRequestContext(null, servletRequest, servletResponse);
  }

  @Test
  public void resolveArgument() throws Exception {
    String content = "{\"name\" : \"Jad\"}";
    servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    servletRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    @SuppressWarnings("unchecked")
    HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>) processor.resolveArgument(
            webRequest, paramSimpleBean);

    assertThat(result).isNotNull();
    assertThat(result.getBody().getName()).isEqualTo("Jad");
  }

  @Test  // SPR-12861
  public void resolveArgumentWithEmptyBody() throws Exception {
    this.servletRequest.setContent(new byte[0]);
    this.servletRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    HttpEntity<?> result = (HttpEntity<?>) processor.resolveArgument(webRequest, this.paramSimpleBean);

    assertThat(result).isNotNull();
    assertThat(result.getBody()).isNull();
  }

  @Test
  public void resolveGenericArgument() throws Exception {
    String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    @SuppressWarnings("unchecked")
    HttpEntity<List<SimpleBean>> result = (HttpEntity<List<SimpleBean>>)
            processor.resolveArgument(webRequest, paramList);

    assertThat(result).isNotNull();
    assertThat(result.getBody().get(0).getName()).isEqualTo("Jad");
    assertThat(result.getBody().get(1).getName()).isEqualTo("Robert");
  }

  @Test
  public void resolveArgumentTypeVariable() throws Exception {
    Method method = MySimpleParameterizedController.class.getMethod("handleDto", HttpEntity.class);
    HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
    MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

    String content = "{\"name\" : \"Jad\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    @SuppressWarnings("unchecked")
    HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>)
            processor.resolveArgument(webRequest, new ResolvableMethodParameter(methodParam));

    assertThat(result).isNotNull();
    assertThat(result.getBody().getName()).isEqualTo("Jad");
  }

  @Test  // SPR-12811
  public void jacksonTypeInfoList() throws Exception {
    Method method = JacksonController.class.getMethod("handleList");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    Object returnValue = new JacksonController().handleList();
    processor.handleReturnValue(webRequest, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"type\":\"foo\"")).isTrue();
    assertThat(content.contains("\"type\":\"bar\"")).isTrue();
  }

  @Test  // SPR-13423
  public void handleReturnValueCharSequence() throws Exception {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    Method method = getClass().getDeclaredMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);
    ResponseEntity<StringBuilder> returnValue = ResponseEntity.ok(new StringBuilder("Foo"));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    processor.handleReturnValue(webRequest, new HandlerMethod(this, method), returnValue);

    assertThat(webRequest.responseHeaders().getFirst("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
    assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
  }

  @Test  // SPR-13423
  public void handleReturnValueWithETagAndETagFilter() throws Exception {
    String eTagValue = "\"deadb33f8badf00d\"";
    String content = "body";

    Method method = getClass().getDeclaredMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);

    FilterChain chain = (req, res) -> {
      ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(eTagValue).body(content);
      try {
        ServletRequestContext requestToUse =
                new ServletRequestContext(null, (HttpServletRequest) req, (HttpServletResponse) res);

        new HttpEntityMethodProcessor(Collections.singletonList(new StringHttpMessageConverter()), null)
                .handleReturnValue(requestToUse, new HandlerMethod(this, method), returnValue);
        requestToUse.requestCompleted();
        assertThat(this.servletResponse.getContentAsString())
                .as("Response body was cached? It should be written directly to the raw response")
                .isEqualTo(content);
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    };

    this.servletRequest.setMethod("GET");
    new ShallowEtagHeaderFilter().doFilter(this.servletRequest, this.servletResponse, chain);

    assertThat(this.servletResponse.getStatus()).isEqualTo(200);
    assertThat(this.servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(eTagValue);
    assertThat(this.servletResponse.getContentAsString()).isEqualTo(content);
  }

  @Test  // gh-24539
  public void handleReturnValueWithMalformedAcceptHeader() throws Exception {
    servletRequest.addHeader("Accept", "null");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    Method method = getClass().getDeclaredMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);
    ResponseEntity<String> returnValue = ResponseEntity.badRequest().body("Foo");

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    processor.handleReturnValue(webRequest, new HandlerMethod(this, method), returnValue);

    assertThat(servletResponse.getStatus()).isEqualTo(400);
    assertThat(servletResponse.getHeader("Content-Type")).isNull();
    assertThat(servletResponse.getContentAsString()).isEmpty();
  }

  @SuppressWarnings("unused")
  private void handle(HttpEntity<List<SimpleBean>> arg1, HttpEntity<SimpleBean> arg2) {
  }

  private ResponseEntity<CharSequence> handle() {
    return null;
  }

  @SuppressWarnings("unused")
  private static abstract class MyParameterizedController<DTO extends Identifiable> {

    public void handleDto(HttpEntity<DTO> dto) {
    }
  }

  @SuppressWarnings("unused")
  private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
  }

  private interface Identifiable extends Serializable {

    Long getId();

    void setId(Long id);
  }

  @SuppressWarnings({ "serial" })
  private static class SimpleBean implements Identifiable {

    private Long id;

    private String name;

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
      this.name = name;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  private static class ParentClass {

    private String parentProperty;

    public ParentClass() {
    }

    public ParentClass(String parentProperty) {
      this.parentProperty = parentProperty;
    }

    public String getParentProperty() {
      return parentProperty;
    }

    public void setParentProperty(String parentProperty) {
      this.parentProperty = parentProperty;
    }
  }

  @JsonTypeName("foo")
  private static class Foo extends ParentClass {

    public Foo() {
    }

    public Foo(String parentProperty) {
      super(parentProperty);
    }
  }

  @JsonTypeName("bar")
  private static class Bar extends ParentClass {

    public Bar() {
    }

    public Bar(String parentProperty) {
      super(parentProperty);
    }
  }

  private static class JacksonController {

    @RequestMapping
    @ResponseBody
    public HttpEntity<List<ParentClass>> handleList() {
      List<ParentClass> list = new ArrayList<>();
      list.add(new Foo("foo"));
      list.add(new Bar("bar"));
      return new HttpEntity<>(list);
    }
  }

}
