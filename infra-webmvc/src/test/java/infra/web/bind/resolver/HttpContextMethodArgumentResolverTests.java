/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import infra.context.ApplicationContext;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.core.io.InputStreamSource;
import infra.core.io.OutputStreamSource;
import infra.http.HttpMethod;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockHttpContext;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 17:08
 */
class HttpContextMethodArgumentResolverTests {

  @Test
  void supportsParameterWithSupportedTypes() throws Exception {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();

    Method method = TestController.class.getDeclaredMethod("handleRequest",
            HttpContext.class, MultipartRequest.class, InputStream.class, OutputStream.class,
            Reader.class, Writer.class, HttpMethod.class, Locale.class, TimeZone.class,
            InputStreamSource.class, OutputStreamSource.class, ZoneId.class);

    for (int i = 0; i < 12; i++) {
      ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, i));
      assertThat(resolver.supportsParameter(parameter))
              .as("Parameter type %s should be supported", parameter.getParameterType())
              .isTrue();
    }
  }

  @Test
  void supportsParameterWithUnsupportedType() throws Exception {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();

    Method method = TestController.class.getDeclaredMethod("handleUnsupported", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  void resolveArgumentWithHttpContext() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleHttpContext", HttpContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(httpContext);
  }

  @Test
  void resolveArgumentWithHttpMethod() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();
    httpContext.setHttpMethod(HttpMethod.POST);

    Method method = TestController.class.getDeclaredMethod("handleMethod", HttpMethod.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isEqualTo(HttpMethod.POST);
  }

  @Test
  void resolveArgumentWithLocale() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleLocale", Locale.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void resolveArgumentWithTimeZone() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleTimeZone", TimeZone.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isEqualTo(TimeZone.getDefault());
  }

  @Test
  void resolveArgumentWithZoneId() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleZoneId", ZoneId.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void resolveArgumentWithInputStreamSource() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleInputStreamSource", InputStreamSource.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(httpContext);
  }

  @Test
  void resolveArgumentWithOutputStreamSource() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleOutputStreamSource", OutputStreamSource.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(httpContext);
  }

  @Test
  void resolveArgumentWithMultipartRequest() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    MultipartRequest multipartRequest = mock(MultipartRequest.class);
    when(http.asMultipartRequest()).thenReturn(multipartRequest);

    Method method = TestController.class.getDeclaredMethod("handleRequest", MultipartRequest.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(http, parameter);

    assertThat(result).isSameAs(multipartRequest);
  }

  @Test
  void resolveArgumentWithInputStream() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    InputStream inputStream = mock(InputStream.class);
    when(http.getInputStream()).thenReturn(inputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", InputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(http, parameter);

    assertThat(result).isSameAs(inputStream);
  }

  @Test
  void resolveArgumentWithOutputStream() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    OutputStream outputStream = mock(OutputStream.class);
    when(http.getOutputStream()).thenReturn(outputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", OutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(http, parameter);

    assertThat(result).isSameAs(outputStream);
  }

  @Test
  void resolveArgumentWithReader() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    BufferedReader reader = mock(BufferedReader.class);
    when(http.getReader()).thenReturn(reader);

    Method method = TestController.class.getDeclaredMethod("handleRequest", Reader.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(http, parameter);

    assertThat(result).isSameAs(reader);
  }

  @Test
  void resolveArgumentWithWriter() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    PrintWriter writer = mock(PrintWriter.class);
    when(http.getWriter()).thenReturn(writer);

    Method method = TestController.class.getDeclaredMethod("handleRequest", Writer.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(http, parameter);

    assertThat(result).isSameAs(writer);
  }

  @Test
  void resolveArgumentWithWrongHttpContextType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    MockHttpContext httpContext = new MockHttpContext();

    httpContext.setHttpMethod(HttpMethod.GET);

    Method method = TestController.class.getDeclaredMethod("handleWrongHttpContext", WrongHttpContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(httpContext, parameter))
            .withMessageContaining("Current request is not of type");
  }

  @Test
  void resolveArgumentWithWrongMultipartRequestType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    when(http.asMultipartRequest()).thenReturn(mock(MultipartRequest.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongMultipartRequest", WrongMultipartRequest.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(http, parameter))
            .withMessageContaining("Current multipart request is not of type");
  }

  @Test
  void resolveArgumentWithWrongInputStreamType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    when(http.getInputStream()).thenReturn(mock(InputStream.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongInputStream", WrongInputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(http, parameter))
            .withMessageContaining("Request input stream is not of type");
  }

  @Test
  void resolveArgumentWithWrongOutputStreamType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    when(http.getOutputStream()).thenReturn(mock(OutputStream.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongOutputStream", WrongOutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(http, parameter))
            .withMessageContaining("Response output stream is not of type");
  }

  @Test
  void resolveArgumentWithWrongReaderType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    when(http.getReader()).thenReturn(mock(BufferedReader.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongReader", WrongReader.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(http, parameter))
            .withMessageContaining("Request body reader is not of type");
  }

  @Test
  void resolveArgumentWithWrongWriterType() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext http = mock();
    when(http.getWriter()).thenReturn(mock(PrintWriter.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongWriter", WrongWriter.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(http, parameter))
            .withMessageContaining("Request body writer is not of type");
  }

  @Test
  void resolveArgumentWithCustomHttpContextImplementation() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    CustomHttpContext httpContext = new CustomHttpContext();

    Method method = TestController.class.getDeclaredMethod("handleHttpContext", HttpContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(httpContext);
  }

  @Test
  void resolveArgumentWithCustomInputStreamImplementation() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext httpContext = mock();
    CustomInputStream inputStream = new CustomInputStream();
    when(httpContext.getInputStream()).thenReturn(inputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", InputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(inputStream);
  }

  @Test
  void resolveArgumentWithCustomOutputStreamImplementation() throws Throwable {
    HttpContextMethodArgumentResolver resolver = new HttpContextMethodArgumentResolver();
    HttpContext httpContext = mock();
    CustomOutputStream outputStream = new CustomOutputStream();
    when(httpContext.getOutputStream()).thenReturn(outputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", OutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(httpContext, parameter);

    assertThat(result).isSameAs(outputStream);
  }

  static class CustomHttpContext extends MockHttpContext {
    // Inherits all behavior from MockHttpContext
  }

  static abstract class CustomMultipartRequest implements MultipartRequest {
  }

  static class CustomInputStream extends InputStream {
    @Override
    public int read() {
      return 0;
    }
  }

  static class CustomOutputStream extends OutputStream {
    @Override
    public void write(int b) {
    }
  }

  static class WrongInputStream extends InputStream {
    @Override
    public int read() {
      return 0;
    }
  }

  static class WrongOutputStream extends OutputStream {
    @Override
    public void write(int b) {
    }
  }

  static class WrongReader extends Reader {
    @Override
    public int read(char[] cbuf, int off, int len) {
      return 0;
    }

    @Override
    public void close() {
    }
  }

  static class WrongWriter extends Writer {
    @Override
    public void write(char[] cbuf, int off, int len) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
  }

  static class TestController {
    public void handleRequest(HttpContext httpContext, MultipartRequest multipartRequest,
            InputStream inputStream, OutputStream outputStream,
            Reader reader, Writer writer, HttpMethod method,
            Locale locale, TimeZone timeZone, InputStreamSource inputSource,
            OutputStreamSource outputSource, ZoneId zoneId) { }

    public void handleRequest(Writer writer) {
    }

    public void handleRequest(Reader reader) {
    }

    public void handleRequest(InputStream inputStream) {
    }

    public void handleRequest(MultipartRequest multipartRequest) {
    }

    public void handleRequest(OutputStream outputStream) {
    }

    public void handleUnsupported(String unsupported) { }

    public void handleHttpContext(HttpContext context) { }

    public void handleMethod(HttpMethod method) { }

    public void handleLocale(Locale locale) { }

    public void handleTimeZone(TimeZone timeZone) { }

    public void handleZoneId(ZoneId zoneId) { }

    public void handleInputStreamSource(InputStreamSource source) { }

    public void handleOutputStreamSource(OutputStreamSource source) { }

    public void handleWrongHttpContext(WrongHttpContext context) { }

    public void handleWrongMultipartRequest(WrongMultipartRequest request) { }

    public void handleWrongInputStream(WrongInputStream inputStream) { }

    public void handleWrongOutputStream(WrongOutputStream outputStream) { }

    public void handleWrongReader(WrongReader reader) { }

    public void handleWrongWriter(WrongWriter writer) { }

  }

  static abstract class WrongHttpContext extends HttpContext {
    protected WrongHttpContext(ApplicationContext context, DispatcherHandler dispatcherHandler) {
      super(context, dispatcherHandler);
    }

    @Override
    public HttpMethod getMethod() {
      return HttpMethod.GET;
    }
  }

  static abstract class WrongMultipartRequest implements MultipartRequest {
  }
}