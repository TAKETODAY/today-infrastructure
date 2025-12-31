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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import infra.beans.propertyeditors.StringTrimmerEditor;
import infra.core.conversion.support.DefaultConversionService;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.web.BindingContext;
import infra.web.RequestContext;
import infra.web.ResolvableMethod;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.WebDataBinder;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.MultipartException;
import infra.web.multipart.Part;
import infra.web.testfixture.MockMultipartFile;

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

  private MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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

    param = this.testMethod.annotPresent(RequestParam.class).isNotNullable().arg(Part.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().noName()).arg(Map.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotNotPresent().arg(Part.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(List.class, Part.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestPart()).arg(Part.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam()).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annot(MvcAnnotationPredicates.requestParam().notRequired()).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Integer.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Part.class);
    assertThat(resolver.supportsParameter(param)).isTrue();

    resolver = new RequestParamMethodArgumentResolver(null, false);

    param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThat(resolver.supportsParameter(param)).isFalse();

    param = this.testMethod.annotPresent(RequestPart.class).arg(Part.class);
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
    Part expected = new MockMultipartFile("mfile", "Hello World".getBytes());
    request.addPart(expected);
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class)
            .isNotNullable().arg(Part.class);
    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof Part;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultipartFileList() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
    Part expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
    request.addPart(expected1);
    request.addPart(expected2);
    request.addPart(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof List;
    assertThat(condition).isTrue();
    assertThat(result).isEqualTo(Arrays.asList(expected1, expected2));
  }

  @Test
  public void resolveMultipartFileListMissing() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addPart(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
    assertThatExceptionOfType(MissingRequestPartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void resolveMultipartFileArray() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected1 = new MockMultipartFile("mfilearray", "Hello World 1".getBytes());
    Part expected2 = new MockMultipartFile("mfilearray", "Hello World 2".getBytes());
    request.addPart(expected1);
    request.addPart(expected2);
    request.addPart(new MockMultipartFile("other", "Hello World 3".getBytes()));
    MockRequestContext webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Part[];
    assertThat(condition).isTrue();
    Part[] parts = (Part[]) result;
    assertThat(parts.length).isEqualTo(2);
    assertThat(expected1).isEqualTo(parts[0]);
    assertThat(expected2).isEqualTo(parts[1]);
  }

  @Test
  public void resolveMultipartFileArrayMissing() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addPart(new MockMultipartFile("other", "Hello World 3".getBytes()));
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
    assertThatExceptionOfType(MissingRequestPartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void resolveMultipartFileNotAnnot() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
    request.addPart(expected);
    MockRequestContext webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod.annotNotPresent().arg(Part.class);
    Object result = resolver.resolveArgument(webRequest, param);

    boolean condition = result instanceof Part;
    assertThat(condition).isTrue();
    assertThat(result).as("Invalid result").isEqualTo(expected);
  }

  @Test
  public void resolveMultipartFileListNotannot() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
    Part expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
    request.addPart(expected1);
    request.addPart(expected2);
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = this.testMethod
            .annotNotPresent(RequestParam.class).arg(List.class, Part.class);

    Object result = resolver.resolveArgument(webRequest, param);
    boolean condition = result instanceof List;
    assertThat(condition).isTrue();
    assertThat(result).isEqualTo(Arrays.asList(expected1, expected2));
  }

  @Test
  public void isMultipartRequest() {
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class)
            .isNotNullable()
            .arg(Part.class);
    assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void isMultipartRequestHttpPut() throws Throwable {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
    request.addPart(expected);
    request.setMethod("PUT");
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext());

    ResolvableMethodParameter param = testMethod.annotNotPresent(
            RequestParam.class).arg(List.class, Part.class);

    Object actual = resolver.resolveArgument(webRequest, param);
    boolean condition = actual instanceof List;
    assertThat(condition).isTrue();
    assertThat(((List<?>) actual).get(0)).isEqualTo(expected);
  }

  @Test
  public void noMultipartContent() throws Throwable {
    request.setMethod("POST");
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class)
            .isNotNullable().arg(Part.class);
    assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
            resolver.resolveArgument(webRequest, param));
  }

  @Test
  public void missingMultipartFile() throws Throwable {
    request.setMethod("POST");
    request.setContentType("multipart/form-data");
    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class)
            .isNotNullable().arg(Part.class);
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

  @Test
  public void missingRequestParamEmptyValueConvertedToNull() throws Throwable {
    WebDataBinder binder = new WebDataBinder(null);
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

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

  @Test
  public void resolveSimpleTypeParamToNull() throws Throwable {
    ResolvableMethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
    assertThatThrownBy(() -> resolver.resolveArgument(webRequest, param))
            .isInstanceOf(MissingRequestParameterException.class)
            .hasMessage("Required request parameter 'stringNotAnnot' for method parameter type String is not present");
  }

  @Test
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

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Integer.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    webRequest.addParameter("name", "123");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNotNull().isEqualTo(123);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void missingOptionalParamValue() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Integer.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void resolveOptionalParamArray() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request);

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Integer[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    webRequest.setParameter("name", "123", "456");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNotNull()
            .isEqualTo(new Integer[] { 123, 456 });
  }

  @Test
  public void missingOptionalParamArray() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Integer[].class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();
  }

  @Test
  public void resolveOptionalParamList() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(List.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    webRequest.setParameter("name", "123", "456");
    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNotNull().isEqualTo(Arrays.asList("123", "456"));
  }

  @Test
  public void missingOptionalParamList() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(List.class);
    Object result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();

    result = resolver.resolveArgument(webRequest, param);
    assertThat(result).isNull();
  }

  @Test
  public void resolveOptionalMultipartFile() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    Part expected = new MockMultipartFile("mfile", "Hello World".getBytes());
    request.addPart(expected);
    webRequest = new MockRequestContext(null, request, null);
    webRequest.setBinding(new BindingContext(initializer));

    ResolvableMethodParameter param = testMethod.annotPresent(
            RequestParam.class).isNullable().arg(Part.class);
    Object result = resolver.resolveArgument(webRequest, param);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void missingOptionalMultipartFile() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    request.setMethod("POST");
    request.setContentType("multipart/form-data");

    var param = this.testMethod.annotPresent(RequestParam.class).isNullable().arg(Part.class);
    Object actual = resolver.resolveArgument(webRequest, param);

    assertThat(actual).isEqualTo(null);
  }

  @Test
  public void optionalMultipartFileWithoutMultipartRequest() throws Throwable {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setConversionService(new DefaultConversionService());

    MockRequestContext webRequest = new MockRequestContext(null, request, new MockHttpResponseImpl());

    BindingContext binderFactory = new BindingContext(initializer);
    webRequest.setBinding(binderFactory);

    ResolvableMethodParameter param = this.testMethod.annotPresent(RequestParam.class).isNullable()
            .arg(Part.class);
    Object actual = resolver.resolveArgument(webRequest, param);

    assertThat(actual).isNull();
  }

  @SuppressWarnings({ "unused" })
  public void handle(
          @RequestParam(name = "name", defaultValue = "bar") String param1,
          @RequestParam("name") String[] param2,
          @RequestParam("name") Map<?, ?> param3,
          @RequestParam("mfile") Part param4,
          @RequestParam("mfilelist") List<Part> param5,
          @RequestParam("mfilearray") Part[] param6,
          @RequestParam Map<?, ?> param10,
          String stringNotAnnot,
          Part multipartFileNotAnnot,
          List<Part> multipartFileList,
          @RequestPart Part requestPartAnnot,
          @RequestParam("name") String paramRequired,
          @RequestParam(name = "name", required = false) String paramNotRequired,
          @RequestParam("name") @Nullable Integer paramOptional,
          @RequestParam("name") @Nullable Integer @Nullable [] paramOptionalArray,
          @RequestParam("name") @Nullable List<?> paramOptionalList,
          @RequestParam("mfile") @Nullable Part multipartFileOptional,
          @RequestParam(defaultValue = "false") Boolean booleanParam) {
  }

}
