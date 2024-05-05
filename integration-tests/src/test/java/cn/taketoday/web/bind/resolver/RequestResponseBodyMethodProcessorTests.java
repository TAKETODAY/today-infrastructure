/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.assertj.XmlAssert;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.accept.ContentNegotiationManagerFactoryBean;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.JsonViewRequestBodyAdvice;
import cn.taketoday.web.handler.method.JsonViewResponseBodyAdvice;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.json.MappingJackson2JsonView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 22:41
 */
class RequestResponseBodyMethodProcessorTests {

  protected static final String NEWLINE_SYSTEM_PROPERTY = System.getProperty("line.separator");

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  private ServletRequestContext request;

  private ResolvableMethodParameter paramGenericList;
  private ResolvableMethodParameter paramSimpleBean;
  private ResolvableMethodParameter paramMultiValueMap;
  private ResolvableMethodParameter paramString;
  private ResolvableMethodParameter returnTypeString;

  private HandlerMethod handlerMethod;

  @BeforeEach
  public void setup() throws Throwable {
    servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("POST");
    servletResponse = new MockHttpServletResponse();
    request = new ServletRequestContext(null, servletRequest, servletResponse);

    Method method = getClass().getDeclaredMethod("handle",
            List.class, SimpleBean.class, MultiValueMap.class, String.class);
    paramGenericList = new ResolvableMethodParameter(new MethodParameter(method, 0));
    paramSimpleBean = new ResolvableMethodParameter(new MethodParameter(method, 1));
    paramMultiValueMap = new ResolvableMethodParameter(new MethodParameter(method, 2));
    paramString = new ResolvableMethodParameter(new MethodParameter(method, 3));
    returnTypeString = new ResolvableMethodParameter(new MethodParameter(method, -1));

    handlerMethod = new HandlerMethod(this, method);
  }

  @Test
  public void resolveArgumentParameterizedType() throws Throwable {
    String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    @SuppressWarnings("unchecked")
    List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
            request, paramGenericList);

    assertThat(result).isNotNull();
    assertThat(result.get(0).getName()).isEqualTo("Jad");
    assertThat(result.get(1).getName()).isEqualTo("Robert");
  }

  @Test
  public void resolveArgumentRawTypeFromParameterizedType() throws Throwable {
    String content = "fruit=apple&vegetable=kale";
    this.servletRequest.setMethod("GET");
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new AllEncompassingFormHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    @SuppressWarnings("unchecked")
    MultiValueMap<String, String> result = (MultiValueMap<String, String>)
            processor.resolveArgument(request, paramMultiValueMap);

    assertThat(result).isNotNull();
    assertThat(result.getFirst("fruit")).isEqualTo("apple");
    assertThat(result.getFirst("vegetable")).isEqualTo("kale");
  }

  @Test
  public void resolveArgumentClassJson() throws Throwable {
    String content = "{\"name\" : \"Jad\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    SimpleBean result = (SimpleBean) processor.resolveArgument(
            request, paramSimpleBean);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Jad");
  }

  @Test
  public void resolveArgumentClassString() throws Throwable {
    String content = "foobarbaz";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType("application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    String result = (String) processor.resolveArgument(
            request, paramString);

    assertThat(result).isNotNull();
    assertThat(result).isEqualTo("foobarbaz");
  }

  @Test // SPR-9942
  public void resolveArgumentRequiredNoContent() {
    this.servletRequest.setContent(new byte[0]);
    this.servletRequest.setContentType("text/plain");
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
    assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
            processor.resolveArgument(request, paramString));
  }

  @Test  // SPR-12778
  public void resolveArgumentRequiredNoContentDefaultValue() throws Throwable {
    this.servletRequest.setContent(new byte[0]);
    this.servletRequest.setContentType("text/plain");
    List<HttpMessageConverter<?>> converters = Collections.singletonList(new StringHttpMessageConverter());
    List<Object> advice = Collections.singletonList(new EmptyRequestBodyAdvice());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters, advice);
    String arg = (String) processor.resolveArgument(request, paramString);
    assertThat(arg).isNotNull();
    assertThat(arg).isEqualTo("default value for empty body");
  }

  @Test  // SPR-9964
  public void resolveArgumentTypeVariable() throws Throwable {
    Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
    HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
    MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

    String content = "{\"name\" : \"Jad\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    SimpleBean result = (SimpleBean) processor.resolveArgument(request, new ResolvableMethodParameter(methodParam));

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Jad");
  }

  @Test  // SPR-14470
  public void resolveParameterizedWithTypeVariableArgument() throws Throwable {
    Method method = MyParameterizedControllerWithList.class.getMethod("handleDto", List.class);
    HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedControllerWithList(), method);
    ResolvableMethodParameter methodParam = new ResolvableMethodParameter(handlerMethod.getMethodParameters()[0]);

    String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    @SuppressWarnings("unchecked")
    List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
            request, methodParam);

    assertThat(result).isNotNull();
    assertThat(result.get(0).getName()).isEqualTo("Jad");
    assertThat(result.get(1).getName()).isEqualTo("Robert");
  }

  @Test  // SPR-11225
  public void resolveArgumentTypeVariableWithNonGenericConverter() throws Throwable {
    Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
    HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
    MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

    String content = "{\"name\" : \"Jad\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    HttpMessageConverter<Object> target = new MappingJackson2HttpMessageConverter();
    HttpMessageConverter<?> proxy = ProxyFactory.getProxy(HttpMessageConverter.class, new SingletonTargetSource(target));
    converters.add(proxy);
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    SimpleBean result = (SimpleBean)
            processor.resolveArgument(request, new ResolvableMethodParameter(methodParam));

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Jad");
  }

  @Test  // SPR-9160
  public void handleReturnValueSortByQuality() throws Throwable {
    this.servletRequest.addHeader("Accept", "text/plain; q=0.5, application/json");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    converters.add(new StringHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    processor.writeWithMessageConverters("Foo", returnTypeString.getParameter(), request);

    assertThat(servletResponse.getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  public void handleReturnValueString() throws Throwable {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
    processor.handleReturnValue(request, handlerMethod, "Foo");

    assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
    assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
  }

  @Test  // SPR-13423
  public void handleReturnValueCharSequence() throws Throwable {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());

    Method method = ResponseBodyController.class.getMethod("handleWithCharSequence");

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
    processor.handleReturnValue(request, new HandlerMethod(this, method), new StringBuilder("Foo"));

    assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
    assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
  }

  @Test
  public void handleReturnValueStringAcceptCharset() throws Throwable {
    this.servletRequest.addHeader("Accept", "text/plain;charset=UTF-8");

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    processor.writeWithMessageConverters("Foo", returnTypeString.getParameter(), request);

    assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
  }

  // SPR-12894

  @Test
  public void handleReturnValueImage() throws Throwable {
    this.servletRequest.addHeader("Accept", "*/*");

    Method method = getClass().getDeclaredMethod("getImage");
    MethodParameter returnType = new MethodParameter(method, -1);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ResourceHttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    ClassPathResource resource = new ClassPathResource("logo.jpg", getClass());
    processor.writeWithMessageConverters(resource, returnType, this.request);

    assertThat(this.servletResponse.getHeader("Content-Type")).isEqualTo("image/jpeg");
  }

  @Test // gh-26212
  public void handleReturnValueWithObjectMapperByTypeRegistration() throws Throwable {
    MediaType halFormsMediaType = MediaType.parseMediaType("application/prs.hal-forms+json");
    MediaType halMediaType = MediaType.parseMediaType("application/hal+json");

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.registerObjectMappersForType(SimpleBean.class, map -> map.put(halMediaType, objectMapper));

    this.servletRequest.addHeader("Accept", halFormsMediaType + "," + halMediaType);

    SimpleBean simpleBean = new SimpleBean();
    simpleBean.setId(12L);
    simpleBean.setName("Jason");

    RequestResponseBodyMethodProcessor processor =
            new RequestResponseBodyMethodProcessor(Collections.singletonList(converter));
    MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("getSimpleBean"), -1);
    processor.writeWithMessageConverters(simpleBean, returnType, this.request);

    assertThat(this.servletResponse.getHeader("Content-Type")).isEqualTo(halMediaType.toString());
    assertThat(this.servletResponse.getContentAsString()).isEqualTo(
            "{" + NEWLINE_SYSTEM_PROPERTY +
                    "  \"id\" : 12," + NEWLINE_SYSTEM_PROPERTY +
                    "  \"name\" : \"Jason\"" + NEWLINE_SYSTEM_PROPERTY +
                    "}");
  }

  @Test
  void problemDetailDefaultMediaType() throws Throwable {
    testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
  void problemDetailWhenJsonRequested() throws Throwable {
    this.servletRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
    testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
    // gh-29588
  void problemDetailWhenJsonAndProblemJsonRequested() throws Throwable {
    this.servletRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE + "," + MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
  void problemDetailWhenNoMatchingMediaTypeRequested() throws Throwable {
    this.servletRequest.addHeader("Accept", MediaType.APPLICATION_PDF_VALUE);
    testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  private void testProblemDetailMediaType(String expectedContentType) throws Throwable {

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    this.servletRequest.setRequestURI("/path");

    RequestResponseBodyMethodProcessor processor =
            new RequestResponseBodyMethodProcessor(
                    Collections.singletonList(new MappingJackson2HttpMessageConverter()));

    Method method = getClass().getDeclaredMethod("handleAndReturnProblemDetail");

    processor.handleReturnValue(request, new HandlerMethod(this, method), problemDetail);

    assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(this.servletResponse.getContentType()).isEqualTo(expectedContentType);
    if (expectedContentType.equals(MediaType.APPLICATION_PROBLEM_XML_VALUE)) {
      XmlAssert.assertThat(this.servletResponse.getContentAsString()).and("""
                      <problem xmlns="urn:ietf:rfc:7807">
                      	<type>about:blank</type>
                      	<title>Bad Request</title>
                      	<status>400</status>
                      	<instance>/path</instance>
                      </problem>""")
              .ignoreWhitespace()
              .areIdentical();
    }
    else {
      JSONAssert.assertEquals("""
              {
              	"type":     "about:blank",
              	"title":    "Bad Request",
              	"status":   400,
              	"instance": "/path"
              }""", this.servletResponse.getContentAsString(), false);
    }
  }

  @Test // SPR-13135
  public void handleReturnValueWithInvalidReturnType() throws Throwable {
    Method method = getClass().getDeclaredMethod("handleAndReturnOutputStream");
    MethodParameter returnType = new MethodParameter(method, -1);
    assertThatIllegalArgumentException().isThrownBy(() -> {
      RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(new ArrayList<>());
      processor.writeWithMessageConverters(new ByteArrayOutputStream(), returnType, this.request);
    });
  }

  @Test
  public void addContentDispositionHeader() throws Throwable {
    ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
    factory.addMediaType("pdf", new MediaType("application", "pdf"));
    factory.afterPropertiesSet();

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
            Collections.singletonList(new StringHttpMessageConverter()),
            factory.getObject());

    assertContentDisposition(processor, false, "/hello.json", "safe extension");
    assertContentDisposition(processor, false, "/hello.pdf", "registered extension");
    assertContentDisposition(processor, true, "/hello.dataless", "unknown extension");

    // path parameters
    assertContentDisposition(processor, false, "/hello.json;a=b", "path param shouldn't cause issue");
    assertContentDisposition(processor, true, "/hello.json;a=b;setup.dataless", "unknown ext in path params");
    assertContentDisposition(processor, true, "/hello.dataless;a=b;setup.json", "unknown ext in filename");
    assertContentDisposition(processor, false, "/hello.json;a=b;setup.json", "safe extensions");
    assertContentDisposition(processor, true, "/hello.json;jsessionid=foo.bar", "jsessionid shouldn't cause issue");

    // encoded dot
    assertContentDisposition(processor, true, "/hello%2Edataless;a=b;setup.json", "encoded dot in filename");
    assertContentDisposition(processor, true, "/hello.json;a=b;setup%2Edataless", "encoded dot in path params");
    assertContentDisposition(processor, true, "/hello.dataless%3Bsetup.bat", "encoded dot in path params");

//    this.servletRequest.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/hello.bat");
//    assertContentDisposition(processor, true, "/bonjour", "forwarded URL");
//    this.servletRequest.removeAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
  }

  @Test
  public void addContentDispositionHeaderToErrorResponse() throws Throwable {
    ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
    factory.addMediaType("pdf", new MediaType("application", "pdf"));
    factory.afterPropertiesSet();

    var processor = new RequestResponseBodyMethodProcessor(
            Collections.singletonList(new StringHttpMessageConverter()), factory.getObject());

    this.servletRequest.setRequestURI("/hello.dataless");
    this.servletResponse.setStatus(400);

    processor.handleReturnValue(request, handlerMethod, "body");

    String header = servletResponse.getHeader("Content-Disposition");
    assertThat(header).isEqualTo("inline;filename=f.txt");
  }

  @Test
  public void supportsReturnTypeResponseBodyOnType() throws Throwable {
    Method method = ResponseBodyController.class.getMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    assertThat(processor.supportsHandlerMethod(new HandlerMethod(this, method)))
            .as("Failed to recognize type-level @ResponseBody").isTrue();
  }

  @Test
  public void supportsReturnTypeRestController() throws Throwable {
    Method method = TestRestController.class.getMethod("handle");
    MethodParameter returnType = new MethodParameter(method, -1);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    assertThat(processor.supportsHandlerMethod(new HandlerMethod(this, method)))
            .as("Failed to recognize type-level @RestController").isTrue();
  }

  @Test
  public void jacksonJsonViewWithResponseBodyAndJsonMessageConverter() throws Throwable {
    Method method = JacksonController.class.getMethod("handleResponseBody");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewResponseBodyAdvice()));

    Object returnValue = new JacksonController().handleResponseBody();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"withView1\":\"with\"")).isFalse();
    assertThat(content.contains("\"withView2\":\"with\"")).isTrue();
    assertThat(content.contains("\"withoutView\":\"without\"")).isFalse();
  }

  @Test
  public void jacksonJsonViewWithResponseEntityAndJsonMessageConverter() throws Throwable {
    Method method = JacksonController.class.getMethod("handleResponseEntity");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewResponseBodyAdvice()), null);

    Object returnValue = new JacksonController().handleResponseEntity();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"withView1\":\"with\"")).isFalse();
    assertThat(content.contains("\"withView2\":\"with\"")).isTrue();
    assertThat(content.contains("\"withoutView\":\"without\"")).isFalse();
  }

  @Test  // SPR-12149
  public void jacksonJsonViewWithResponseBodyAndXmlMessageConverter() throws Throwable {
    Method method = JacksonController.class.getMethod("handleResponseBody");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2XmlHttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewResponseBodyAdvice()));

    Object returnValue = new JacksonController().handleResponseBody();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("<withView1>with</withView1>")).isFalse();
    assertThat(content.contains("<withView2>with</withView2>")).isTrue();
    assertThat(content.contains("<withoutView>without</withoutView>")).isFalse();
  }

  @Test  // SPR-12149
  public void jacksonJsonViewWithResponseEntityAndXmlMessageConverter() throws Throwable {
    Method method = JacksonController.class.getMethod("handleResponseEntity");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2XmlHttpMessageConverter());

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewResponseBodyAdvice()), null);

    Object returnValue = new JacksonController().handleResponseEntity();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("<withView1>with</withView1>")).isFalse();
    assertThat(content.contains("<withView2>with</withView2>")).isTrue();
    assertThat(content.contains("<withoutView>without</withoutView>")).isFalse();
  }

  @Test  // SPR-12501
  public void resolveArgumentWithJacksonJsonView() throws Throwable {
    String content = "{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Method method = JacksonController.class.getMethod("handleRequestBody", JacksonViewBean.class);
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewRequestBodyAdvice()));

    JacksonViewBean result = (JacksonViewBean)
            processor.resolveArgument(request, new ResolvableMethodParameter(methodParameter));

    assertThat(result).isNotNull();
    assertThat(result.getWithView1()).isEqualTo("with");
    assertThat(result.getWithView2()).isNull();
    assertThat(result.getWithoutView()).isNull();
  }

  @Test  // SPR-12501
  public void resolveHttpEntityArgumentWithJacksonJsonView() throws Throwable {
    String content = "{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Method method = JacksonController.class.getMethod("handleHttpEntity", HttpEntity.class);
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewRequestBodyAdvice()), null);

    @SuppressWarnings("unchecked")
    HttpEntity<JacksonViewBean> result = (HttpEntity<JacksonViewBean>)
            processor.resolveArgument(request, new ResolvableMethodParameter(methodParameter));

    assertThat(result).isNotNull();
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getWithView1()).isEqualTo("with");
    assertThat(result.getBody().getWithView2()).isNull();
    assertThat(result.getBody().getWithoutView()).isNull();
  }

  @Test  // SPR-12501
  public void resolveArgumentWithJacksonJsonViewAndXmlMessageConverter() throws Throwable {
    String content = "<root>" +
            "<withView1>with</withView1>" +
            "<withView2>with</withView2>" +
            "<withoutView>without</withoutView></root>";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_XML_VALUE);

    Method method = JacksonController.class.getMethod("handleRequestBody", JacksonViewBean.class);
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2XmlHttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewRequestBodyAdvice()));

    JacksonViewBean result = (JacksonViewBean)
            processor.resolveArgument(request, new ResolvableMethodParameter(methodParameter));

    assertThat(result).isNotNull();
    assertThat(result.getWithView1()).isEqualTo("with");
    assertThat(result.getWithView2()).isNull();
    assertThat(result.getWithoutView()).isNull();
  }

  @Test  // SPR-12501
  public void resolveHttpEntityArgumentWithJacksonJsonViewAndXmlMessageConverter() throws Throwable {
    String content = "<root>" +
            "<withView1>with</withView1>" +
            "<withView2>with</withView2>" +
            "<withoutView>without</withoutView></root>";
    this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_XML_VALUE);

    Method method = JacksonController.class.getMethod("handleHttpEntity", HttpEntity.class);
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2XmlHttpMessageConverter());

    HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
            converters, null, Collections.singletonList(new JsonViewRequestBodyAdvice()), null);

    @SuppressWarnings("unchecked")
    HttpEntity<JacksonViewBean> result = (HttpEntity<JacksonViewBean>)
            processor.resolveArgument(request, new ResolvableMethodParameter(methodParameter));

    assertThat(result).isNotNull();
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getWithView1()).isEqualTo("with");
    assertThat(result.getBody().getWithView2()).isNull();
    assertThat(result.getBody().getWithoutView()).isNull();
  }

  @Test  // SPR-12811
  public void jacksonTypeInfoList() throws Throwable {
    Method method = JacksonController.class.getMethod("handleTypeInfoList");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    Object returnValue = new JacksonController().handleTypeInfoList();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"type\":\"foo\"")).isTrue();
    assertThat(content.contains("\"type\":\"bar\"")).isTrue();
  }

  @Test  // SPR-13318
  public void jacksonSubType() throws Throwable {
    Method method = JacksonController.class.getMethod("handleSubType");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
    MethodParameter methodReturnType = handlerMethod.getReturnType();

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    Object returnValue = new JacksonController().handleSubType();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"id\":123")).isTrue();
    assertThat(content.contains("\"name\":\"foo\"")).isTrue();
  }

  @Test  // SPR-13318
  public void jacksonSubTypeList() throws Throwable {
    Method method = JacksonController.class.getMethod("handleSubTypeList");
    HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());
    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    Object returnValue = new JacksonController().handleSubTypeList();
    processor.handleReturnValue(request, handlerMethod, returnValue);

    String content = this.servletResponse.getContentAsString();
    assertThat(content.contains("\"id\":123")).isTrue();
    assertThat(content.contains("\"name\":\"foo\"")).isTrue();
    assertThat(content.contains("\"id\":456")).isTrue();
    assertThat(content.contains("\"name\":\"bar\"")).isTrue();
  }

  @Test  // SPR-14520
  public void resolveArgumentTypeVariableWithGenericInterface() throws Throwable {
    this.servletRequest.setContent("\"foo\"".getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Method method = MyControllerImplementingInterface.class.getMethod("handle", Object.class);
    HandlerMethod handlerMethod = new HandlerMethod(new MyControllerImplementingInterface(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    assertThat(processor.supportsParameter(new ResolvableMethodParameter(methodParameter))).isTrue();
    String value = (String) processor.readWithMessageConverters(
            this.request, methodParameter, methodParameter.getGenericParameterType());
    assertThat(value).isEqualTo("foo");
  }

  @Test  // gh-24127
  public void resolveArgumentTypeVariableWithGenericInterfaceAndSubclass() throws Throwable {
    this.servletRequest.setContent("\"foo\"".getBytes(StandardCharsets.UTF_8));
    this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Method method = SubControllerImplementingInterface.class.getMethod("handle", Object.class);
    HandlerMethod handlerMethod = new HandlerMethod(new SubControllerImplementingInterface(), method);
    MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new MappingJackson2HttpMessageConverter());

    RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

    assertThat(processor.supportsParameter(new ResolvableMethodParameter(methodParameter))).isTrue();
    String value = (String) processor.readWithMessageConverters(
            this.request, methodParameter, methodParameter.getGenericParameterType());
    assertThat(value).isEqualTo("foo");
  }

  private void assertContentDisposition(RequestResponseBodyMethodProcessor processor,
          boolean expectContentDisposition, String requestURI, String comment) throws Throwable {

    this.servletRequest.setRequestURI(requestURI);
    processor.handleReturnValue(request, handlerMethod, "body");

    String header = servletResponse.getHeader("Content-Disposition");
    if (expectContentDisposition) {
      assertThat(header)
              .as("Expected 'Content-Disposition' header. Use case: '" + comment + "'")
              .isEqualTo("inline;filename=f.txt");
    }
    else {
      assertThat(header)
              .as("Did not expect 'Content-Disposition' header. Use case: '" + comment + "'")
              .isNull();
    }

    this.servletRequest = new MockHttpServletRequest();
    this.servletResponse = new MockHttpServletResponse();
    this.request = new ServletRequestContext(null, servletRequest, servletResponse);
  }

  String handle(
          @RequestBody List<SimpleBean> list,
          @RequestBody SimpleBean simpleBean,
          @RequestBody MultiValueMap<String, String> multiValueMap,
          @RequestBody String string) {

    return null;
  }

  Resource getImage() {
    return null;
  }

  ProblemDetail handleAndReturnProblemDetail() {
    return null;
  }

  @RequestMapping
  OutputStream handleAndReturnOutputStream() {
    return null;
  }

  SimpleBean getSimpleBean() {
    return null;
  }

  private static abstract class MyParameterizedController<DTO extends Identifiable> {

    @SuppressWarnings("unused")
    public void handleDto(@RequestBody DTO dto) { }
  }

  private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
  }

  private interface Identifiable extends Serializable {

    Long getId();

    void setId(Long id);
  }

  @SuppressWarnings("unused")
  private static abstract class MyParameterizedControllerWithList<DTO extends Identifiable> {

    public void handleDto(@RequestBody List<DTO> dto) {
    }
  }

  @SuppressWarnings("unused")
  private static class MySimpleParameterizedControllerWithList extends MyParameterizedControllerWithList<SimpleBean> {
  }

  @SuppressWarnings({ "serial", "NotNullFieldNotInitialized" })
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

    public void setName(String name) {
      this.name = name;
    }
  }

  @ResponseBody
  private static class ResponseBodyController {

    @RequestMapping
    public String handle() {
      return "hello";
    }

    @RequestMapping
    public CharSequence handleWithCharSequence() {
      return null;
    }
  }

  @RestController
  private static class TestRestController {

    @RequestMapping
    public String handle() {
      return "hello";
    }
  }

  private interface MyJacksonView1 { }

  private interface MyJacksonView2 { }

  @SuppressWarnings("NotNullFieldNotInitialized")
  private static class JacksonViewBean {

    @JsonView(MyJacksonView1.class)
    private String withView1;

    @JsonView(MyJacksonView2.class)
    private String withView2;

    private String withoutView;

    public String getWithView1() {
      return withView1;
    }

    public void setWithView1(String withView1) {
      this.withView1 = withView1;
    }

    @Nullable
    public String getWithView2() {
      return withView2;
    }

    public void setWithView2(String withView2) {
      this.withView2 = withView2;
    }

    @Nullable
    public String getWithoutView() {
      return withoutView;
    }

    public void setWithoutView(String withoutView) {
      this.withoutView = withoutView;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  public static class ParentClass {

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
  public static class Foo extends ParentClass {

    public Foo() {
    }

    public Foo(String parentProperty) {
      super(parentProperty);
    }
  }

  @JsonTypeName("bar")
  public static class Bar extends ParentClass {

    public Bar() {
    }

    public Bar(String parentProperty) {
      super(parentProperty);
    }
  }

  private static class BaseController<T> {

    @RequestMapping
    @ResponseBody
    @SuppressWarnings("unchecked")
    public List<T> handleTypeInfoList() {
      List<T> list = new ArrayList<>();
      list.add((T) new Foo("foo"));
      list.add((T) new Bar("bar"));
      return list;
    }
  }

  private static class JacksonController extends BaseController<ParentClass> {

    @RequestMapping
    @ResponseBody
    @JsonView(MyJacksonView2.class)
    public JacksonViewBean handleResponseBody() {
      JacksonViewBean bean = new JacksonViewBean();
      bean.setWithView1("with");
      bean.setWithView2("with");
      bean.setWithoutView("without");
      return bean;
    }

    @RequestMapping
    @JsonView(MyJacksonView2.class)
    public ResponseEntity<JacksonViewBean> handleResponseEntity() {
      JacksonViewBean bean = new JacksonViewBean();
      bean.setWithView1("with");
      bean.setWithView2("with");
      bean.setWithoutView("without");
      ModelAndView mav = new ModelAndView(new MappingJackson2JsonView());
      mav.addObject("bean", bean);
      return new ResponseEntity<>(bean, HttpStatus.OK);
    }

    @RequestMapping
    @ResponseBody
    public JacksonViewBean handleRequestBody(@JsonView(MyJacksonView1.class) @RequestBody JacksonViewBean bean) {
      return bean;
    }

    @RequestMapping
    @ResponseBody
    public JacksonViewBean handleHttpEntity(@JsonView(MyJacksonView1.class) HttpEntity<JacksonViewBean> entity) {
      return entity.getBody();
    }

    @RequestMapping
    @ResponseBody
    public Identifiable handleSubType() {
      SimpleBean foo = new SimpleBean();
      foo.setId(123L);
      foo.setName("foo");
      return foo;
    }

    @RequestMapping
    @ResponseBody
    public List<Identifiable> handleSubTypeList() {
      SimpleBean foo = new SimpleBean();
      foo.setId(123L);
      foo.setName("foo");
      SimpleBean bar = new SimpleBean();
      bar.setId(456L);
      bar.setName("bar");
      return Arrays.asList(foo, bar);
    }

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String defaultCharset() {
      return "foo";
    }
  }

  private static class EmptyRequestBodyAdvice implements RequestBodyAdvice {

    @Override
    public boolean supports(MethodParameter methodParameter,
            Type targetType, HttpMessageConverter<?> converterType) {
      return converterType instanceof StringHttpMessageConverter;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converterType) {

      return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converterType) {

      return body;
    }

    @Override
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
            MethodParameter parameter, Type targetType, HttpMessageConverter<?> converterType) {

      return "default value for empty body";
    }
  }

  interface MappingInterface<A> {

    default A handle(@RequestBody A arg) {
      return arg;
    }
  }

  static class MyControllerImplementingInterface implements MappingInterface<String> {
  }

  static class SubControllerImplementingInterface extends MyControllerImplementingInterface {

    @Override
    public String handle(String arg) {
      return arg;
    }
  }

}
