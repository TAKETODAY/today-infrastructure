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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.web.RequestContext;
import infra.web.annotation.RequestPart;
import infra.web.handler.method.NamedValueInfo;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 20:07
 */
class RequestPartMethodArgumentResolverTests {

  private RequestPartMethodArgumentResolver resolver;

  private RequestContext context;

  private List<HttpMessageConverter<?>> messageConverters;

  @BeforeEach
  void setup() {
    messageConverters = Collections.singletonList(new StringHttpMessageConverter());
    resolver = new RequestPartMethodArgumentResolver(messageConverters);
    context = mock(RequestContext.class);
  }

  @Test
  void closeStreamIfNecessaryShouldCloseInputStream() throws IOException {
    InputStream inputStream = mock(InputStream.class);
    doThrow(new IOException("Test exception")).when(inputStream).close();

    // Should not throw exception even if close throws IOException
    assertThatNoException().isThrownBy(() -> resolver.closeStreamIfNecessary(inputStream));
    verify(inputStream).close();
  }

  @Test
  void supportsParameterWithRequestPartAnnotation() throws Exception {
    MethodParameter methodParameter = mock(MethodParameter.class);
    when(methodParameter.hasParameterAnnotation(RequestPart.class)).thenReturn(true);

    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParameter);
    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  void supportsParameterWithMultipartFileAndNoRequestParam() throws Exception {
    MethodParameter methodParameter = mock(MethodParameter.class);
    when(methodParameter.hasParameterAnnotation(RequestPart.class)).thenReturn(false);
    when(methodParameter.hasParameterAnnotation(infra.web.annotation.RequestParam.class)).thenReturn(false);

    try (var delegate = mockStatic(MultipartResolutionDelegate.class)) {
      delegate.when(() -> MultipartResolutionDelegate.isMultipartArgument(methodParameter))
              .thenReturn(true);

      ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParameter);
      assertThat(resolver.supportsParameter(parameter)).isTrue();
    }
  }

  @Test
  void supportsParameterWithMultipartFileAndRequestParamShouldReturnFalse() throws Exception {
    MethodParameter methodParameter = mock(MethodParameter.class);
    when(methodParameter.hasParameterAnnotation(RequestPart.class)).thenReturn(false);
    when(methodParameter.hasParameterAnnotation(infra.web.annotation.RequestParam.class)).thenReturn(true);

    ResolvableMethodParameter parameter = new ResolvableMethodParameter(methodParameter);
    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  void resolveArgumentWithMultipartFile() throws Throwable {
    String partName = "testPart";
    Part part = mock(Part.class);

    MethodParameter methodParameter = mock(MethodParameter.class);
    when(methodParameter.hasParameterAnnotation(RequestPart.class)).thenReturn(true);

    ResolvableMethodParameter resolvable = spy(new ResolvableMethodParameter(methodParameter));
    doReturn(new NamedValueInfo(partName)).when(resolvable).getNamedValueInfo();

    try (var delegate = mockStatic(MultipartResolutionDelegate.class)) {
      delegate.when(() -> MultipartResolutionDelegate.resolveMultipartArgument(partName, methodParameter, context))
              .thenReturn(part);

      Object result = resolver.resolveArgument(context, resolvable);
      assertThat(result).isEqualTo(part);
    }
  }

}
