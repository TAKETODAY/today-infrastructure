/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import org.jspecify.annotations.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.validation.BindingResult;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.RequestContext;
import infra.web.annotation.RequestBody;
import infra.web.annotation.ResponseBody;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.WebDataBinder;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
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

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  private MockRequestContext webRequest;

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
    given(stringMessageConverter.getSupportedMediaTypes(ArgumentMatchers.any())).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    resourceMessageConverter = mock(HttpMessageConverter.class);
    given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
    given(resourceMessageConverter.getSupportedMediaTypes(ArgumentMatchers.any())).willReturn(Collections.singletonList(MediaType.ALL));
    resourceRegionMessageConverter = mock(HttpMessageConverter.class);
    given(resourceRegionMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
    given(resourceRegionMessageConverter.getSupportedMediaTypes(ArgumentMatchers.any())).willReturn(Collections.singletonList(MediaType.ALL));

    processor = new RequestResponseBodyMethodProcessor(
            Arrays.asList(stringMessageConverter, resourceMessageConverter, resourceRegionMessageConverter));

    mockRequest = new HttpMockRequestImpl();
    mockRequest.setMethod("POST");
    mockResponse = new MockHttpResponseImpl();
    webRequest = new MockRequestContext(null, mockRequest, mockResponse);

    Method methodHandle1 = getClass().getMethod("handle1", String.class, Integer.TYPE);
    paramRequestBodyString = new ResolvableMethodParameter(new MethodParameter(methodHandle1, 0));
    paramInt = new ResolvableMethodParameter(new MethodParameter(methodHandle1, 1));
    paramValidBean = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle2", SimpleBean.class), 0));
    paramStringNotRequired = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle3", String.class), 0));
    paramOptionalString = new ResolvableMethodParameter(new MethodParameter(getClass().getMethod("handle4", String.class), 0));

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
    mockRequest.addHeader("Content-Type", contentType.toString());

    String body = "Foo";
    mockRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

    given(stringMessageConverter.canRead(String.class, contentType)).willReturn(true);
    given(stringMessageConverter.read(ArgumentMatchers.eq(String.class), ArgumentMatchers.isA(HttpInputMessage.class))).willReturn(body);

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
    mockRequest.addHeader("Content-Type", contentType.toString());
    mockRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));

    @SuppressWarnings("unchecked")
    HttpMessageConverter<SimpleBean> beanConverter = mock(HttpMessageConverter.class);
    given(beanConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(beanConverter.canRead(SimpleBean.class, contentType)).willReturn(true);
    given(beanConverter.read(ArgumentMatchers.eq(SimpleBean.class), ArgumentMatchers.isA(HttpInputMessage.class))).willReturn(simpleBean);

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

    webRequest.setBinding(bindingContext);
  }

  @Test
  public void resolveArgumentCannotRead() throws Throwable {
    MediaType contentType = MediaType.TEXT_PLAIN;
    mockRequest.addHeader("Content-Type", contentType.toString());
    mockRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));

    given(stringMessageConverter.canRead(String.class, contentType)).willReturn(false);

    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentNoContentType() throws Throwable {
    mockRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentInvalidContentType() throws Throwable {
    this.mockRequest.setContentType("bad");
    mockRequest.setContent("payload".getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentRequiredNoContent() throws Throwable {
    mockRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(ArgumentMatchers.eq(String.class), ArgumentMatchers.isA(HttpInputMessage.class))).willReturn(null);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> processor.resolveArgument(webRequest, paramRequestBodyString));
  }

  @Test
  public void resolveArgumentNotGetRequests() throws Throwable {
    mockRequest.setMethod("GET");
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test
  public void resolveArgumentNotRequiredWithContent() throws Throwable {
    mockRequest.setContentType("text/plain");
    mockRequest.setContent("body".getBytes());
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(ArgumentMatchers.eq(String.class), ArgumentMatchers.isA(HttpInputMessage.class))).willReturn("body");
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isEqualTo("body");
  }

  @Test
  public void resolveArgumentNotRequiredNoContent() throws Throwable {
    mockRequest.setContentType("text/plain");
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test
  public void resolveArgumentNotRequiredNoContentNoContentType() throws Throwable {
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramStringNotRequired)).isNull();
  }

  @Test
  public void resolveArgumentOptionalWithContent() throws Throwable {
    mockRequest.setContentType("text/plain");
    mockRequest.setContent("body".getBytes());
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.read(ArgumentMatchers.eq(String.class), ArgumentMatchers.isA(HttpInputMessage.class))).willReturn("body");
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isEqualTo("body");
  }

  @Test
  public void resolveArgumentOptionalNoContent() throws Throwable {
    mockRequest.setContentType("text/plain");
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isNull();
  }

  @Test
  public void resolveArgumentOptionalNoContentNoContentType() throws Throwable {
    mockRequest.setContent(new byte[0]);
    given(stringMessageConverter.canRead(String.class, MediaType.TEXT_PLAIN)).willReturn(true);
    given(stringMessageConverter.canRead(String.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(false);
    assertThat(processor.resolveArgument(webRequest, paramOptionalString)).isNull();
  }

  @Test
  public void handleReturnValue() throws Throwable {
    MediaType accepted = MediaType.TEXT_PLAIN;
    mockRequest.addHeader("Accept", accepted.toString());

    String body = "Foo";
    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod1, body);

    verify(stringMessageConverter).write(ArgumentMatchers.eq(body), ArgumentMatchers.eq(accepted), ArgumentMatchers.isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnValueProduces() throws Throwable {
    String body = "Foo";

    mockRequest.addHeader("Accept", "text/*");

    HandlerMatchingMetadata metadata = mock(HandlerMatchingMetadata.class);
    given(metadata.getProducibleMediaTypes()).willReturn(List.of(MediaType.TEXT_HTML));

    webRequest.setMatchingMetadata(metadata);

    given(stringMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod6, body);

    verify(stringMessageConverter).write(ArgumentMatchers.eq(body), ArgumentMatchers.eq(MediaType.TEXT_HTML), ArgumentMatchers.isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnValueNotAcceptable() throws Throwable {
    MediaType accepted = MediaType.APPLICATION_ATOM_XML;
    mockRequest.addHeader("Accept", accepted.toString());

    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes()).willReturn(List.of(MediaType.TEXT_PLAIN));
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(false);

    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> processor.handleReturnValue(webRequest, handlerMethod1, "Foo"));
  }

  @Test
  public void handleReturnValueNotAcceptableProduces() throws Throwable {
    MediaType accepted = MediaType.TEXT_PLAIN;
    mockRequest.addHeader("Accept", accepted.toString());

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

    then(resourceMessageConverter).should(Mockito.times(1)).write(ArgumentMatchers.any(ByteArrayResource.class),
            ArgumentMatchers.eq(MediaType.APPLICATION_OCTET_STREAM), ArgumentMatchers.any(HttpOutputMessage.class));
    assertThat(mockResponse.getStatus()).isEqualTo(200);
  }

  @Test
  public void handleReturnValueMediaTypeSuffix() throws Throwable {
    String body = "Foo";
    MediaType accepted = MediaType.APPLICATION_XHTML_XML;
    List<MediaType> supported = Collections.singletonList(MediaType.valueOf("application/*+xml"));

    mockRequest.addHeader("Accept", accepted);

    given(stringMessageConverter.canWrite(String.class, null)).willReturn(true);
    given(stringMessageConverter.getSupportedMediaTypes(ArgumentMatchers.any())).willReturn(supported);
    given(stringMessageConverter.canWrite(String.class, accepted)).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod6, body);

    verify(stringMessageConverter).write(ArgumentMatchers.eq(body), ArgumentMatchers.eq(accepted), ArgumentMatchers.isA(HttpOutputMessage.class));
  }

  @Test
  public void handleReturnTypeResourceByteRange() throws Throwable {
    Resource returnValue = new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8));
    mockRequest.addHeader("Range", "bytes=0-5");

    given(resourceRegionMessageConverter.canWrite(ArgumentMatchers.any(), ArgumentMatchers.eq(null))).willReturn(true);
    given(resourceRegionMessageConverter.canWrite(ArgumentMatchers.any(), ArgumentMatchers.eq(MediaType.APPLICATION_OCTET_STREAM))).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod7, returnValue);

    then(resourceRegionMessageConverter).should(Mockito.times(1)).write(
            ArgumentMatchers.anyCollection(), ArgumentMatchers.eq(MediaType.APPLICATION_OCTET_STREAM),
            ArgumentMatchers.argThat(outputMessage -> "bytes".equals(outputMessage.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES))));
    assertThat(mockResponse.getStatus()).isEqualTo(206);
  }

  @Test
  public void handleReturnTypeResourceIllegalByteRange() throws Throwable {
    Resource returnValue = new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8));
    mockRequest.addHeader("Range", "illegal");

    given(resourceRegionMessageConverter.canWrite(ArgumentMatchers.any(), ArgumentMatchers.eq(null))).willReturn(true);
    given(resourceRegionMessageConverter.canWrite(ArgumentMatchers.any(), ArgumentMatchers.eq(MediaType.APPLICATION_OCTET_STREAM))).willReturn(true);

    processor.handleReturnValue(webRequest, handlerMethod7, returnValue);

    then(resourceRegionMessageConverter).should(Mockito.never()).write(
            ArgumentMatchers.anyCollection(), ArgumentMatchers.eq(MediaType.APPLICATION_OCTET_STREAM), ArgumentMatchers.any(HttpOutputMessage.class));
    assertThat(mockResponse.getStatus()).isEqualTo(416);
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
  public void handle4(@RequestBody @Nullable String s) {
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
