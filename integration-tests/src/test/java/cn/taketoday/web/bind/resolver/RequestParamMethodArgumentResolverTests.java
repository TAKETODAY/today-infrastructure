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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.beans.propertyeditors.StringTrimmerEditor;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ResolvableMethod;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.support.ConfigurableWebBindingInitializer;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.testfixture.MockMultipartFile;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockMultipartHttpMockRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/12 10:43
 */
class RequestParamMethodArgumentResolverTests {

  private RequestParamMethodArgumentResolver resolver = new RequestParamMethodArgumentResolver(null, true);

  private HttpMockRequestImpl request = new HttpMockRequestImpl();

  private ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

  private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

  @Test
  public void supportsParameter() {
    resolver = new RequestParamMethodArgumentResolver(null, true);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired("bar")).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().name("name")).arg(Map.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotNotPresent().arg(MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestPart()).arg(MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam()).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired()).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    resolver = new RequestParamMethodArgumentResolver(null, false);

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annotPresent(RequestPart.class).arg(MultipartFile.class);
    assertThat(resolver.supportsParameter(param)).isFalse();
  }

  @Test
  public void resolveString() throws Throwable {
    String expected = "foo";
    request.addParameter("name", expected);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired("bar")).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof String;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveStringArray() throws Throwable {
    String[] expected = new String[] { "foo", "bar" };
    request.addParameter("name", expected);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof String[];
    assertThat(condition).isTrue();
    assertThat((String[]) result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  void resolveStringArrayWithEmptyArraySuffix() throws Throwable {
    String[] expected = new String[] { "foo", "bar" };
    request.addParameter("name[]", expected[0]);
    request.addParameter("name[]", expected[1]);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);
  }

  @Test
  public void resolveMultipartFile() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
    request.addFile(expected);
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof MultipartFile;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultipartFileList() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
    MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
    request.addFile(expected1);
    request.addFile(expected2);
    request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof List;
    assertThat(condition).isTrue();
    assertThat(result).isEqualTo(Arrays.asList(expected1, expected2));
  }

  @Test
  public void resolveMultipartFileListMissing() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
    assertThatExceptionOfType(MissingRequestPartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void resolveMultipartFileArray() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected1 = new MockMultipartFile("mfilearray", "Hello World 1".getBytes());
    MultipartFile expected2 = new MockMultipartFile("mfilearray", "Hello World 2".getBytes());
    request.addFile(expected1);
    request.addFile(expected2);
    request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
    ServletRequestContext webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof MultipartFile[];
    assertThat(condition).isTrue();
    MultipartFile[] parts = (MultipartFile[]) result;
    assertThat(parts.length).isEqualTo(2);
    assertThat(expected1).isEqualTo(parts[0]);
    assertThat(expected2).isEqualTo(parts[1]);
  }

  @Test
  public void resolveMultipartFileArrayMissing() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
    assertThatExceptionOfType(MissingRequestPartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void resolveMultipartFileNotAnnot() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
    request.addFile(expected);
    ServletRequestContext webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotNotPresent().arg(MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof MultipartFile;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultipartFileListNotannot() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
    MultipartFile expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
    request.addFile(expected1);
    request.addFile(expected2);
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod
            .annotNotPresent(RequestParam.class).arg(List.class, MultipartFile.class);

    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof List;
    assertThat(condition).isTrue();
    assertThat(result).isEqualTo(Arrays.asList(expected1, expected2));
  }

  @Test
  public void isMultipartRequest() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
    assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test  // SPR-9079
  public void isMultipartRequestHttpPut() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
    request.addFile(expected);
    request.setMethod("PUT");
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = testMethod.annotNotPresent(
            RequestParam.class).arg(List.class, MultipartFile.class);

    Object actual = resolver.resolveArgument(webRequest, param);
    boolean condition = actual instanceof List;
    assertThat(condition).isTrue();
    assertThat(((List<?>) actual).get(0)).isEqualTo(expected);
  }

  @Test
  public void noMultipartContent() throws Throwable {
    request.setMethod("POST");
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
    assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void missingMultipartFile() throws Throwable {
    request.setMethod("POST");
    request.setContentType("multipart/form-data");
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
    assertThatExceptionOfType(MissingRequestPartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void resolveDefaultValue() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired("bar")).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof String;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo("bar");
  }

  @Test
  public void missingRequestParam() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
    assertThatExceptionOfType(MissingRequestParameterException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test  // SPR-10578
  public void missingRequestParamEmptyValueConvertedToNull() throws Throwable {
    WebDataBinder binder = new WebDataBinder(null);
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = mock(BindingContext.class);
    given(binderFactory.createBinder(webRequest, null, "stringNotAnnot")).willReturn(binder);
    given(binderFactory.createBinder(webRequest, "stringNotAnnot")).willReturn(binder);
    webRequest.setBinding(binderFactory);

    request.addParameter("stringNotAnnot", "");

    ResolvableMethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
//    Object arg = resolver.resolveArgument(webRequest, param);
//    assertThat(arg).isNull();

    assertThatThrownBy(() -> resolver.resolveArgument(webRequest, param))
            .isInstanceOf(MissingRequestParameterException.class)
            .hasMessage("Required request parameter 'stringNotAnnot' for method parameter type String is present but converted to null");
  }

  @Test
  public void missingRequestParamAfterConversionWithDefaultValue() throws Throwable {

    BindingContext binderFactory = new BindingContext() {
      @Override
      protected WebDataBinder createBinderInstance(@Nullable Object target, String objectName, RequestContext request) throws Exception {
        return new WebDataBinder(null);
      }
    };

    webRequest.setBinding(binderFactory);

    request.addParameter("booleanParam", " ");

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Boolean.class);
    Object arg = resolver.resolveArgument(webRequest, param);
    assertThat(arg).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void missingRequestParamEmptyValueNotRequired() throws Throwable {
    WebDataBinder binder = new WebDataBinder(null);
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    request.addParameter("name", "");

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = mock(BindingContext.class);
    given(binderFactory.createBinder(webRequest, null, "name")).willReturn(binder);
    given(binderFactory.createBinder(webRequest, "name")).willReturn(binder);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired()).arg(String.class);
    Object arg = resolver.resolveArgument(webRequest, param);
    assertThat(arg).isNull();
  }

  @Test
  public void missingRequestParamEmptyValueNotRequiredWithDefaultValue() throws Throwable {
    WebDataBinder binder = new WebDataBinder(null);
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

    BindingContext context = mock(BindingContext.class);
    given(context.createBinder(webRequest, null, "name")).willReturn(binder);
    given(context.createBinder(webRequest, "name")).willReturn(binder);

    request.addParameter("name", "    ");

    webRequest.setBinding(context);

    ResolvableMethodParameter param = testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired("bar")).arg(String.class);
    Object arg = resolver.resolveArgument(webRequest, param);
    assertThat(arg).isEqualTo("bar");
  }

  @Test
  public void resolveSimpleTypeParam() throws Throwable {
    request.setParameter("stringNotAnnot", "plainValue");
    ResolvableMethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof String;
    assertThat(condition).isTrue();
    assertThat(result).isEqualTo("plainValue");
  }

  @Test  // SPR-8561
  public void resolveSimpleTypeParamToNull() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThatThrownBy(() -> resolver.resolveArgument(webRequest, param))
            .isInstanceOf(MissingRequestParameterException.class)
            .hasMessage("Required request parameter 'stringNotAnnot' for method parameter type String is not present");
  }

  @Test  // SPR-10180
  public void resolveEmptyValueToDefault() throws Throwable {
    request.addParameter("name", "");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired("bar")).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo("bar");
  }

  @Test
  public void resolveEmptyValueWithoutDefault() throws Throwable {
    request.addParameter("stringNotAnnot", "");
    ResolvableMethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void resolveEmptyValueRequiredWithoutDefault() throws Throwable {
    request.addParameter("name", "");
    ResolvableMethodParameter param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired()).arg(String.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo("");
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void resolveOptionalParamValue() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    request.addParameter("name", "123");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional) result).get()).isEqualTo(123);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void missingOptionalParamValue() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional) result).isPresent()).isFalse();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void resolveOptionalParamArray() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    request.addParameter("name", "123", "456");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat((Integer[]) ((Optional) result).get()).isEqualTo(new Integer[] { 123, 456 });
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void missingOptionalParamArray() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional) result).isPresent()).isFalse();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void resolveOptionalParamList() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, List.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    request.addParameter("name", "123", "456");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional) result).get()).isEqualTo(Arrays.asList("123", "456"));
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void missingOptionalParamList() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, List.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isEqualTo(Optional.empty());

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional) result).isPresent()).isFalse();
  }

  @Test
  public void resolveOptionalMultipartFile() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
    request.addFile(expected);
    webRequest = new ServletRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext(initializer));

    ResolvableMethodParameter param = testMethod.annotPresent(
            RequestParam.class).arg(Optional.class, MultipartFile.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Optional;
    assertThat(condition).isTrue();
    assertThat(((Optional<?>) result).get()).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void missingOptionalMultipartFile() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    request.setMethod("POST");
    request.setContentType("multipart/form-data");

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
    Object actual = resolver.resolveArgument(webRequest, param);

    assertThat(actual).isEqualTo(Optional.empty());
  }

  @Test
  public void optionalMultipartFileWithoutMultipartRequest() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    ServletRequestContext webRequest = new ServletRequestContext(null, request, new MockHttpServletResponse());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
    Object actual = resolver.resolveArgument(webRequest, param);

    assertThat(actual).isEqualTo(Optional.empty());
  }

  @SuppressWarnings({ "unused", "OptionalUsedAsFieldOrParameterType" })
  public void handle(
          @RequestParam(name = "name", defaultValue = "bar") String param1,
          @RequestParam("name") String[] param2,
          @RequestParam("name") Map<?, ?> param3,
          @RequestParam("mfile") MultipartFile param4,
          @RequestParam("mfilelist") List<MultipartFile> param5,
          @RequestParam("mfilearray") MultipartFile[] param6,
          @RequestParam Map<?, ?> param10,
          String stringNotAnnot,
          MultipartFile multipartFileNotAnnot,
          List<MultipartFile> multipartFileList,
          @RequestPart MultipartFile requestPartAnnot,
          @RequestParam("name") String paramRequired,
          @RequestParam(name = "name", required = false) String paramNotRequired,
          @RequestParam("name") Optional<Integer> paramOptional,
          @RequestParam("name") Optional<Integer[]> paramOptionalArray,
          @RequestParam("name") Optional<List<?>> paramOptionalList,
          @RequestParam("mfile") Optional<MultipartFile> multipartFileOptional,
          @RequestParam(defaultValue = "false") Boolean booleanParam) {
  }

}
