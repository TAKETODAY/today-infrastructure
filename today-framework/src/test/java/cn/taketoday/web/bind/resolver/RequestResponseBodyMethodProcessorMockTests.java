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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link RequestResponseBodyMethodProcessor} delegating to a
 * mock HttpMessageConverter.
 *
 * <p>Also see {@link RequestResponseBodyMethodProcessorTests}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/28 14:34
 */
public class RequestResponseBodyMethodProcessorMockTests {

  private HttpMessageConverter<String> stringMessageConverter;

  private HttpMessageConverter<Resource> resourceMessageConverter;

  private HttpMessageConverter<Object> resourceRegionMessageConverter;

  private RequestResponseBodyMethodProcessor processor;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  private ServletRequestContext webRequest;

  private ResolvableMethodParameter paramRequestBodyString;
  private ResolvableMethodParameter paramInt;
  private ResolvableMethodParameter paramValidBean;
  private ResolvableMethodParameter paramStringNotRequired;
  private ResolvableMethodParameter paramOptionalString;

  private MethodParameter returnTypeString;
  private MethodParameter returnTypeInt;
  private MethodParameter returnTypeStringProduces;
  private MethodParameter returnTypeResource;

  private HandlerMethod handlerMethod1;
  private HandlerMethod handlerMethod5;
  private HandlerMethod handlerMethod6;
  private HandlerMethod handlerMethod7;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setup() throws Throwable {
    stringMessageConverter = mock(HttpMessageConverter.class);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    resourceMessageConverter = mock(HttpMessageConverter.class);
    given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
    given(resourceMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.ALL));
    resourceRegionMessageConverter = mock(HttpMessageConverter.class);
    given(resourceRegionMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
    given(resourceRegionMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.ALL));

    processor = new RequestResponseBodyMethodProcessor(
            Arrays.asList(stringMessageConverter, resourceMessageConverter, resourceRegionMessageConverter));

    servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("POST");
    servletResponse = new MockHttpServletResponse();
    webRequest = new ServletRequestContext(null, servletRequest, servletResponse);

    Method methodHandle1 = getClass().getMethod("handle1", String.class, Integer.TYPE);
    paramRequestBodyString = new ResolvableMethodParameter(new MethodParameter(methodHandle1, 0));
    paramInt = new ResolvableMethodParameter(new MethodParameter(methodHandle1, 1));
    paramValidBean = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle2", SimpleBean.class), 0));
    paramStringNotRequired = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle3", String.class), 0));
    paramOptionalString = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle4", Optional.class), 0));

    Method handle5 = getClass().getMethod("handle5");
    Method handle6 = getClass().getMethod("handle6");
    Method handle7 = getClass().getMethod("handle7");

    returnTypeString = new MethodParameter(methodHandle1, -1);
    returnTypeInt = new MethodParameter(handle5, -1);
    returnTypeStringProduces = new MethodParameter(handle6, -1);
    returnTypeResource = new MethodParameter(handle7, -1);

    handlerMethod1 = new HandlerMethod(this, methodHandle1);
    handlerMethod5 = new HandlerMethod(this, handle5);
    handlerMethod6 = new HandlerMethod(this, handle6);
    handlerMethod7 = new HandlerMethod(this, handle7);
  }

  @Test
  public void supportsParameter() {
    assertThat(processor.supportsParameter(paramRequestBodyString)).as("RequestBody parameter not supported").isTrue();
    assertThat(processor.supportsParameter(paramInt)).as("non-RequestBody parameter supported").isFalse();
  }

  @Test
  public void supportsReturnType() {
    assertThat(processor.supportsHandlerMethod(handlerMethod1)).as("ResponseBody return type not supported").isTrue();
    assertThat(processor.supportsHandlerMethod(handlerMethod5)).as("non-ResponseBody return type supported").isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    initBindingContext();

    MediaType contentType = MediaType.TEXT_PLAIN;
    servletRequest.addHeader("Content-Type", contentType.toString());

    String body = "Foo";
    servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

    given(stringMessageConverter.canRead(String.class, contentType)).willReturn(true);
    given(stringMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

    Object result = processor.resolveArgument(webRequest, paramRequestBodyString);

    assertThat(result).as("Invalid argument").isEqualTo(body);
  }

  @Test
  public void resolveArgumentNotValid() throws Throwable {
    assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
                    testResolveArgumentWithValidation(new SimpleBean(null)))
            .satisfies(ex -> {
              BindingResult bindingResult = ex.getBindingResult();
              assertThat(bindingResult.getObjectName()).isEqualTo("simpleBean");
              assertThat(bindingResult.getErrorCount()).isEqualTo(1);
              assertThat(bindingResult.getFieldError("name")).isNotNull();
            });
  }

  @Test
  public void resolveArgumentValid() throws Throwable {
    testResolveArgumentWithValidation(new SimpleBean("name"));
  }

  private void testResolveArgumentWithValidation(SimpleBean simpleBean) throws Throwable {
    initBindingContext();
    MediaType contentType = MediaType.TEXT_PLAIN;
    servletRequest.addHeader("Content-Type", contentType.toString());
    servletRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));

    @SuppressWarnings("unchecked")
    HttpMessageConverter<SimpleBean> beanConverter = mock(HttpMessageConverter.class);
    given(beanConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(beanConverter.canRead(SimpleBean.class, contentType)).willReturn(true);
    given(beanConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).willReturn(simpleBean);

    processor = new RequestResponseBodyMethodProcessor(Collections.singletonList(beanConverter));
    processor.resolveArgument(webRequest, paramValidBean);
  }

  public void initBindingContext() {
    BindingContext bindingContext = new BindingContext() {

      @Override
      public void initBinder(WebDataBinder dataBinder, RequestContext request) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        dataBinder.setValidator(validator);
      }

    };

    webRequest.setBindingContext(bindingContext);
  }

  @Test
  public void resolveArgumentCannotRead() throws Throwable {
    MediaType contentType = MediaType.TEXT_PLAIN;
    servletRequest.addHeader("Content-Type", contentType.toString());
    servletRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));

    given(stringMessageConverter.canRead(String.class, contentType)).willReturn(false);

    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentNoContentType() throws Throwable {
    servletRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentInvalidContentType() throws Throwable {
    this.servletRequest.setContentType("bad");
    servletRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test // SPR-9942
  public void resolveArgumentRequiredNoContent() throws Throwable {
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(null);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentNotGetRequests() throws Throwable {
    servletRequest.setMethod("GET");
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test
  public void resolveArgumentNotRequiredWithContent() throws Throwable {
    servletRequest.setContentType("text/plain");
    servletRequest.setContent("body".getBytes());
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn("body");
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isEqualTo("body");
  }

  @Test
  public void resolveArgumentNotRequiredNoContent() throws Throwable {
    servletRequest.setContentType("text/plain");
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test  // SPR-13417
  public void resolveArgumentNotRequiredNoContentNoContentType() throws Throwable {
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test
  public void resolveArgumentOptionalWithContent() throws Throwable {
    servletRequest.setContentType("text/plain");
    servletRequest.setContent("body".getBytes());
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn("body");
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isEqualTo(Optional.of("body"));
  }

  @Test
  public void resolveArgumentOptionalNoContent() throws Throwable {
    servletRequest.setContentType("text/plain");
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isEqualTo(Optional.empty());
  }

  @Test
  public void resolveArgumentOptionalNoContentNoContentType() throws Throwable {
    servletRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isEqualTo(Optional.empty());
  }

  @Test
  public void handleReturnValue() throws Throwable {
    MediaType accepted = MediaType.TEXT_PLAIN;
    servletRequest.addHeader("Accept", accepted.toString());

    String body = "Foo";
    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod1, body);

    verify(stringMessageConverter).write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnValueProduces() throws Throwable {
    String body = "Foo";

    servletRequest.addHeader("Accept", "text/*");

    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    given(metadata.getProducibleMediaTypes()).willReturn(new MediaType[] { MediaType.TEXT_HTML });

    webRequest.setMatchingMetadata(metadata);

    given(stringMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod6, body);

    verify(stringMessageConverter).write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnValueNotAcceptable() throws Throwable {
    MediaType accepted = MediaType.APPLICATION_ATOM_XML;
    servletRequest.addHeader("Accept", accepted.toString());

    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(Arrays.asList(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(false);

    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> processor.handleReturnValue(webRequest, handlerMethod1, "Foo"));
  }

  @Test
  public void handleReturnValueNotAcceptableProduces() throws Throwable {
    MediaType accepted = MediaType.TEXT_PLAIN;
    servletRequest.addHeader("Accept", accepted.toString());

    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(false);

    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
            processor.handleReturnValue(webRequest, handlerMethod6, "Foo"));
  }

  @Test
  public void handleReturnTypeResource() throws Throwable {
    Resource returnValue = new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8));

    given(resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
    given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
    given(resourceMessageConverter.canWrite(ByteArrayResource.class, MediaType.APPLICATION_OCTET_STREAM))
            .willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod7, returnValue);

    then(resourceMessageConverter).should(times(1)).write(any(ByteArrayResource.class),
            eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
    assertThat(servletResponse.getStatus()).isEqualTo(200);
  }

  @Test  // SPR-9841
  public void handleReturnValueMediaTypeSuffix() throws Throwable {
    String body = "Foo";
    MediaType accepted = MediaType.APPLICATION_XHTML_XML;
    List<MediaType> supported = Collections.singletonList(MediaType.valueOf("application/*+xml"));

    servletRequest.addHeader("Accept", accepted);

    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes(any())).willReturn(supported);
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod6, body);

    verify(stringMessageConverter).write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnTypeResourceByteRange() throws Throwable {
    Resource returnValue = new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8));
    servletRequest.addHeader("Range", "bytes=0-5");

    given(resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
    given(resourceRegionMessageConverter.canWrite(any(), eq(MediaType.APPLICATION_OCTET_STREAM))).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod7, returnValue);

    then(resourceRegionMessageConverter).should(times(1)).write(
            anyCollection(), eq(MediaType.APPLICATION_OCTET_STREAM),
            argThat(outputMessage -> "bytes".equals(outputMessage.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES))));
    assertThat(servletResponse.getStatus()).isEqualTo(206);
  }

  @Test
  public void handleReturnTypeResourceIllegalByteRange() throws Throwable {
    Resource returnValue = new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8));
    servletRequest.addHeader("Range", "illegal");

    given(resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
    given(resourceRegionMessageConverter.canWrite(any(), eq(MediaType.APPLICATION_OCTET_STREAM))).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod7, returnValue);

    then(resourceRegionMessageConverter).should(never()).write(
            anyCollection(), eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
    assertThat(servletResponse.getStatus()).isEqualTo(416);
  }

  @SuppressWarnings("unused")
  @ResponseBody
  public String handle1(@RequestBody String s, int i) {
    return s;
  }

  @SuppressWarnings("unused")
  public void handle2(@Valid @RequestBody SimpleBean b) {
  }

  @SuppressWarnings("unused")
  public void handle3(@RequestBody(required = false) String s) {
  }

  @SuppressWarnings("unused")
  public void handle4(@RequestBody Optional<String> s) {
  }

  @SuppressWarnings("unused")
  public int handle5() {
    return 42;
  }

  @SuppressWarnings("unused")
  @ResponseBody
  public String handle6() {
    return null;
  }

  @SuppressWarnings("unused")
  @ResponseBody
  public Resource handle7() {
    return null;
  }

  @SuppressWarnings("unused")
  private static class SimpleBean {

    @NotNull
    private final String name;

    public SimpleBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

}
