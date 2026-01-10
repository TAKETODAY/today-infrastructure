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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.ErrorResponse;
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
    converters.add(new JacksonJsonHttpMessageConverter());
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);

    @SuppressWarnings("unchecked")
    HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>) processor.resolveArgument(
            webRequest, paramSimpleBean);

    assertThat(result).isNotNull();
    assertThat(result.getBody().getName()).isEqualTo("Jad");
  }

  @Test
  public void resolveArgumentWithEmptyBody() throws Exception {
    this.mockRequest.setContent(new byte[0]);
    this.mockRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new JacksonJsonHttpMessageConverter());
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
    converters.add(new JacksonJsonHttpMessageConverter());
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
    converters.add(new JacksonJsonHttpMessageConverter());
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
    converters.add(new JacksonJsonHttpMessageConverter());
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

  @Test
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

  @Test
  public void supportsParameterWithRequestEntity() throws Exception {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new JacksonJsonHttpMessageConverter());

    ResolvableMethodParameter param = new ResolvableMethodParameter(
            new MethodParameter(getClass().getDeclaredMethod("handleRequestEntity", RequestEntity.class), 0));
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    assertThat(processor.supportsParameter(param)).isTrue();
  }

  @Test
  public void supportsParameterWithNonHttpEntity() throws Exception {
    ResolvableMethodParameter param = new ResolvableMethodParameter(
            new MethodParameter(getClass().getDeclaredMethod("handleString", String.class), 0));
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsParameter(param)).isFalse();
  }

  @Test
  public void supportsReturnValueWithHttpEntity() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(new HttpEntity<>("test"))).isTrue();
  }

  @Test
  public void supportsReturnValueWithResponseEntity() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(ResponseEntity.ok("test"))).isTrue();
  }

  @Test
  public void supportsReturnValueWithRequestEntity() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(new RequestEntity<>("test", HttpMethod.GET, URI.create("/")))).isFalse();
  }

  @Test
  public void supportsReturnValueWithNull() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(null)).isFalse();
  }

  @Test
  public void supportsReturnValueWithProblemDetail() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(ProblemDetail.forRawStatusCode(404))).isTrue();
  }

  @Test
  public void supportsReturnValueWithErrorStatus() {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsReturnValue(new TestErrorResponse())).isTrue();
  }

  @Test
  public void supportsHandlerMethodWithHttpEntityReturnType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleReturnHttpEntity");
    HandlerMethod handlerMethod = new HandlerMethod(this, method);
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsHandlerMethod(handlerMethod)).isTrue();
  }

  @Test
  public void supportsHandlerMethodWithResponseEntityReturnType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleReturnResponseEntity");
    HandlerMethod handlerMethod = new HandlerMethod(this, method);
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsHandlerMethod(handlerMethod)).isTrue();
  }

  @Test
  public void supportsHandlerMethodWithRequestEntityReturnType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleReturnRequestEntity");
    HandlerMethod handlerMethod = new HandlerMethod(this, method);
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsHandlerMethod(handlerMethod)).isFalse();
  }

  @Test
  public void supportsHandlerMethodWithStringReturnType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleString", String.class);
    HandlerMethod handlerMethod = new HandlerMethod(this, method);
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsHandlerMethod(handlerMethod)).isFalse();
  }

  @Test
  public void handleNullReturnValue() throws Exception {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, null);
    // Should not throw exception
  }

  @Test
  public void handleProblemDetailReturnValue() throws Exception {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);

    processor.handleReturnValue(webRequest, null, problemDetail);

    assertThat(webRequest.getStatus()).isEqualTo(404);
  }

  @Test
  public void handleErrorResponseReturnValue() throws Exception {
    TestErrorResponse errorResponse = new TestErrorResponse();
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);

    processor.handleReturnValue(webRequest, null, errorResponse);

    assertThat(webRequest.getStatus()).isEqualTo(500);
  }

  @Test
  public void handleResponseEntityWithHeaders() throws Exception {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Custom-Header", "custom-value");
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", headers, HttpStatusCode.valueOf(201));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, responseEntity);

    assertThat(webRequest.getStatus()).isEqualTo(201);
    assertThat(webRequest.responseHeaders().getFirst("Custom-Header")).isEqualTo("custom-value");
  }

  @Test
  public void handleResponseEntityWithRedirect() throws Exception {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add(HttpHeaders.LOCATION, "/redirect");
    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", headers, HttpStatusCode.valueOf(302));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, responseEntity);

    assertThat(webRequest.getStatus()).isEqualTo(302);
    assertThat(webRequest.responseHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/redirect");
  }

  @Test
  public void supportsParameterWithRawHttpEntity() throws Exception {
    Method method = getClass().getDeclaredMethod("handleRawHttpEntity", HttpEntity.class);
    ResolvableMethodParameter param = new ResolvableMethodParameter(new MethodParameter(method, 0));
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    assertThat(processor.supportsParameter(param)).isTrue();
  }

  @Test
  public void resolveArgumentWithRawHttpEntity() throws Exception {
    mockRequest.setContent("test content".getBytes(StandardCharsets.UTF_8));
    mockRequest.setContentType("text/plain");

    Method method = getClass().getDeclaredMethod("handleStringRawHttpEntity", HttpEntity.class);
    ResolvableMethodParameter param = new ResolvableMethodParameter(new MethodParameter(method, 0));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new StringHttpMessageConverter()), null);
    HttpEntity<?> result = (HttpEntity<?>) processor.resolveArgument(webRequest, param);

    assertThat(result).isNotNull();
    assertThat(result.getBody()).isEqualTo("test content");
  }

  @Test
  public void resolveArgumentWithRequestEntity() throws Exception {
    String content = "{\"name\":\"test\"}";
    mockRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    mockRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new JacksonJsonHttpMessageConverter());

    Method method = getClass().getDeclaredMethod("handleRequestEntity", RequestEntity.class);
    ResolvableMethodParameter param = new ResolvableMethodParameter(new MethodParameter(method, 0));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters, null);
    RequestEntity<SimpleBean> result = (RequestEntity<SimpleBean>) processor.resolveArgument(webRequest, param);

    assertThat(result).isNotNull();
    assertThat(result.getBody().getName()).isEqualTo("test");
    assertThat(result.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(result.getURI()).isEqualTo(webRequest.getURI());
  }

  @Test
  public void getHttpEntityTypeWithNonParameterizedType() throws Exception {
    Method method = getClass().getDeclaredMethod("handleRawHttpEntity", HttpEntity.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    // Access private method via reflection
    java.lang.reflect.Method getHttpEntityType = HttpEntityMethodProcessor.class.getDeclaredMethod("getHttpEntityType", MethodParameter.class);
    getHttpEntityType.setAccessible(true);

    Type type = (Type) getHttpEntityType.invoke(processor, parameter);
    assertThat(type).isEqualTo(Object.class);
  }

  @Test
  public void handleResponseEntityWithVaryHeader() throws Exception {
    HttpHeaders entityHeaders = HttpHeaders.forWritable();
    entityHeaders.setVary(List.of("Accept-Encoding"));
    HttpHeaders responseHeaders = webRequest.responseHeaders();
    responseHeaders.setVary(List.of("Accept-Language"));

    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", entityHeaders, HttpStatus.OK);

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, responseEntity);

    List<String> varyHeaders = responseHeaders.getVary();
    assertThat(varyHeaders).contains("Accept-Encoding");
  }

  @Test
  public void handleResponseEntityWithWildcardVaryHeader() throws Exception {
    HttpHeaders entityHeaders = HttpHeaders.forWritable();
    entityHeaders.setVary(List.of("Accept-Encoding"));

    HttpHeaders responseHeaders = webRequest.responseHeaders();
    responseHeaders.setVary(List.of("*"));

    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", entityHeaders, HttpStatus.OK);

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, responseEntity);

    List<String> varyHeaders = responseHeaders.getVary();
    assertThat(varyHeaders).containsExactly("*");
  }

  @Test
  public void handleResponseEntityWithNotModified() throws Exception {
    mockRequest.setMethod("GET");
    mockRequest.addHeader("If-None-Match", "\"etag1\"");

    HttpHeaders entityHeaders = HttpHeaders.forWritable();
    entityHeaders.setETag("\"etag1\"");

    ResponseEntity<String> responseEntity = new ResponseEntity<>("body", entityHeaders, HttpStatus.OK);

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, responseEntity);

    assertThat(webRequest.getStatus()).isEqualTo(304);
  }

  @Test
  public void handleProblemDetailWithInstance() throws Exception {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    problemDetail.setInstance(URI.create("/test"));

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, problemDetail);

    assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test"));
  }

  @Test
  public void handleProblemDetailWithoutInstance() throws Exception {
    ProblemDetail problemDetail = ProblemDetail.forRawStatusCode(404);
    mockRequest.setRequestURI("/test-path");

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);
    processor.handleReturnValue(webRequest, null, problemDetail);

    assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test-path"));
  }

  @Test
  public void getReturnValueTypeWithReturnValue() throws Exception {
    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);

    java.lang.reflect.Method getReturnValueType = HttpEntityMethodProcessor.class.getDeclaredMethod("getReturnValueType", Object.class, MethodParameter.class);
    getReturnValueType.setAccessible(true);

    ResponseEntity<String> returnValue = ResponseEntity.ok("test");
    Class<?> type = (Class<?>) getReturnValueType.invoke(processor, returnValue, null);

    assertThat(type).isEqualTo(ResponseEntity.class);
  }

  @Test
  public void getReturnValueTypeWithoutReturnValue() throws Exception {
    Method method = getClass().getDeclaredMethod("handleReturnResponseEntity");
    MethodParameter returnType = new MethodParameter(method, -1);

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null);

    java.lang.reflect.Method getReturnValueType = HttpEntityMethodProcessor.class.getDeclaredMethod("getReturnValueType", Object.class, MethodParameter.class);
    getReturnValueType.setAccessible(true);

    Class<?> type = (Class<?>) getReturnValueType.invoke(processor, null, returnType);

    assertThat(type).isEqualTo(String.class);
  }

  @SuppressWarnings("unused")
  private void handleStringRawHttpEntity(HttpEntity<String> entity) {
  }

  @SuppressWarnings("unused")
  private void handleRawHttpEntity(HttpEntity entity) {
  }

  @SuppressWarnings("unused")
  private void handleRequestEntity(RequestEntity<SimpleBean> requestEntity) { }

  @SuppressWarnings("unused")
  private void handleString(String string) { }

  @SuppressWarnings("unused")
  private HttpEntity<String> handleReturnHttpEntity() { return null; }

  @SuppressWarnings("unused")
  private ResponseEntity<String> handleReturnResponseEntity() { return null; }

  @SuppressWarnings("unused")
  private RequestEntity<String> handleReturnRequestEntity() { return null; }

  private static class TestErrorResponse implements ErrorResponse {

    @Override
    public HttpStatusCode getStatusCode() {
      return HttpStatusCode.valueOf(500);
    }

    @Override
    public ProblemDetail getBody() {
      return ProblemDetail.forRawStatusCode(500);
    }

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
