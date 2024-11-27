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

package infra.web.bind.resolver;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.HttpEntity;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 10:36
 */
class HttpEntityMethodProcessorTests {

  private ResolvableMethodParameter paramList;

  private ResolvableMethodParameter paramSimpleBean;

  private HttpMockRequestImpl mockRequest;

  private MockRequestContext webRequest;

  private MockHttpResponseImpl mockResponse;

  @BeforeEach
  public void setup() throws Exception {
    Method method = getClass().getDeclaredMethod("handle", HttpEntity.class, HttpEntity.class);
    paramList = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramSimpleBean = new ResolvableMethodParameter(new MethodParameter(method, 1));

    mockRequest = new HttpMockRequestImpl();
    mockResponse = new MockHttpResponseImpl();
    mockRequest.setMethod("POST");
    webRequest = new MockRequestContext(null, mockRequest, mockResponse);
  }

  @Test
  public void resolveArgument() throws Exception {
    String content = "{\"name\" : \"Jad\"}";
    mockRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    mockRequest.setContentType("application/json");

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
    this.mockRequest.setContent(new byte[0]);
    this.mockRequest.setContentType("application/json");

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
    this.mockRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.mockRequest.setContentType("application/json");

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
    this.mockRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.mockRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    @SuppressWarnings("unchecked")
    HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>)
            processor.resolveArgument(webRequest, new ResolvableMethodParameter(methodParam));

    assertThat(result).isNotNull();
    assertThat(result.getBody().getName()).isEqualTo("Jad");
  }

  @Test
  public void jacksonTypeInfoList() throws Exception {
    Method method = JacksonController.class.getMethod("handleList");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    Object returnValue = new JacksonController().handleList();
    processor.handleReturnValue(webRequest, handlerMethod, returnValue);

    String content = this.mockResponse.getContentAsString();
    assertThat(content.contains("\"type\":\"foo\"")).isTrue();
    assertThat(content.contains("\"type\":\"bar\"")).isTrue();
  }

  @Test
  public void handleReturnValueCharSequence() throws Exception {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    Method method = getClass().getDeclaredMethod("handle");
    ResponseEntity<StringBuilder> returnValue = ResponseEntity.ok(new StringBuilder("Foo"));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    processor.handleReturnValue(webRequest, new HandlerMethod(this, method), returnValue);

    assertThat(webRequest.responseHeaders().getFirst("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
    assertThat(mockResponse.getContentAsString()).isEqualTo("Foo");
  }

  @Test  // gh-24539
  public void handleReturnValueWithMalformedAcceptHeader() throws Exception {
    mockRequest.addHeader("Accept", "null");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    Method method = getClass().getDeclaredMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);
    ResponseEntity<String> returnValue = ResponseEntity.badRequest().body("Foo");

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    processor.handleReturnValue(webRequest, new HandlerMethod(this, method), returnValue);

    assertThat(mockResponse.getStatus()).isEqualTo(400);
    assertThat(mockResponse.getHeader("Content-Type")).isNull();
    assertThat(mockResponse.getContentAsString()).isEmpty();
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
