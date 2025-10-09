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
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 17:08
 */
class RequestContextMethodArgumentResolverTests {

  @Test
  void supportsParameterWithSupportedTypes() throws Exception {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();

    Method method = TestController.class.getDeclaredMethod("handleRequest",
            RequestContext.class, MultipartRequest.class, InputStream.class, OutputStream.class,
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
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();

    Method method = TestController.class.getDeclaredMethod("handleUnsupported", String.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  void resolveArgumentWithRequestContext() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleRequestContext", RequestContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveArgumentWithHttpMethod() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();
    requestContext.setHttpMethod(HttpMethod.POST);

    Method method = TestController.class.getDeclaredMethod("handleMethod", HttpMethod.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isEqualTo(HttpMethod.POST);
  }

  @Test
  void resolveArgumentWithLocale() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleLocale", Locale.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void resolveArgumentWithTimeZone() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleTimeZone", TimeZone.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isEqualTo(TimeZone.getDefault());
  }

  @Test
  void resolveArgumentWithZoneId() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleZoneId", ZoneId.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isEqualTo(ZoneId.systemDefault());
  }

  @Test
  void resolveArgumentWithInputStreamSource() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleInputStreamSource", InputStreamSource.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveArgumentWithOutputStreamSource() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleOutputStreamSource", OutputStreamSource.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveArgumentWithMultipartRequest() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    MultipartRequest multipartRequest = mock(MultipartRequest.class);
    when(requestContext.multipartRequest()).thenReturn(multipartRequest);

    Method method = TestController.class.getDeclaredMethod("handleRequest", MultipartRequest.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(multipartRequest);
  }

  @Test
  void resolveArgumentWithInputStream() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    InputStream inputStream = mock(InputStream.class);
    when(requestContext.getInputStream()).thenReturn(inputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", InputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(inputStream);
  }

  @Test
  void resolveArgumentWithOutputStream() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    OutputStream outputStream = mock(OutputStream.class);
    when(requestContext.getOutputStream()).thenReturn(outputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", OutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(outputStream);
  }

  @Test
  void resolveArgumentWithReader() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    BufferedReader reader = mock(BufferedReader.class);
    when(requestContext.getReader()).thenReturn(reader);

    Method method = TestController.class.getDeclaredMethod("handleRequest", Reader.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(reader);
  }

  @Test
  void resolveArgumentWithWriter() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    PrintWriter writer = mock(PrintWriter.class);
    when(requestContext.getWriter()).thenReturn(writer);

    Method method = TestController.class.getDeclaredMethod("handleRequest", Writer.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(writer);
  }

  @Test
  void resolveArgumentWithWrongRequestContextType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    MockRequestContext requestContext = new MockRequestContext();

    requestContext.setHttpMethod(HttpMethod.GET);

    Method method = TestController.class.getDeclaredMethod("handleWrongRequestContext", WrongRequestContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Current request is not of type");
  }

  @Test
  void resolveArgumentWithWrongMultipartRequestType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    when(requestContext.multipartRequest()).thenReturn(mock(MultipartRequest.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongMultipartRequest", WrongMultipartRequest.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Current multipart request is not of type");
  }

  @Test
  void resolveArgumentWithWrongInputStreamType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    when(requestContext.getInputStream()).thenReturn(mock(InputStream.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongInputStream", WrongInputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Request input stream is not of type");
  }

  @Test
  void resolveArgumentWithWrongOutputStreamType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    when(requestContext.getOutputStream()).thenReturn(mock(OutputStream.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongOutputStream", WrongOutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Response output stream is not of type");
  }

  @Test
  void resolveArgumentWithWrongReaderType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    when(requestContext.getReader()).thenReturn(mock(BufferedReader.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongReader", WrongReader.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Request body reader is not of type");
  }

  @Test
  void resolveArgumentWithWrongWriterType() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    when(requestContext.getWriter()).thenReturn(mock(PrintWriter.class));

    Method method = TestController.class.getDeclaredMethod("handleWrongWriter", WrongWriter.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveArgument(requestContext, parameter))
            .withMessageContaining("Request body writer is not of type");
  }

  @Test
  void resolveArgumentWithCustomRequestContextImplementation() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    CustomRequestContext requestContext = new CustomRequestContext();

    Method method = TestController.class.getDeclaredMethod("handleRequestContext", RequestContext.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(requestContext);
  }

  @Test
  void resolveArgumentWithCustomInputStreamImplementation() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    CustomInputStream inputStream = new CustomInputStream();
    when(requestContext.getInputStream()).thenReturn(inputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", InputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(inputStream);
  }

  @Test
  void resolveArgumentWithCustomOutputStreamImplementation() throws Throwable {
    RequestContextMethodArgumentResolver resolver = new RequestContextMethodArgumentResolver();
    RequestContext requestContext = mock();
    CustomOutputStream outputStream = new CustomOutputStream();
    when(requestContext.getOutputStream()).thenReturn(outputStream);

    Method method = TestController.class.getDeclaredMethod("handleRequest", OutputStream.class);
    ResolvableMethodParameter parameter = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));

    Object result = resolver.resolveArgument(requestContext, parameter);

    assertThat(result).isSameAs(outputStream);
  }

  static class CustomRequestContext extends MockRequestContext {
    // Inherits all behavior from MockRequestContext
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
    public void handleRequest(RequestContext requestContext, MultipartRequest multipartRequest,
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

    public void handleRequestContext(RequestContext context) { }

    public void handleMethod(HttpMethod method) { }

    public void handleLocale(Locale locale) { }

    public void handleTimeZone(TimeZone timeZone) { }

    public void handleZoneId(ZoneId zoneId) { }

    public void handleInputStreamSource(InputStreamSource source) { }

    public void handleOutputStreamSource(OutputStreamSource source) { }

    public void handleWrongRequestContext(WrongRequestContext context) { }

    public void handleWrongMultipartRequest(WrongMultipartRequest request) { }

    public void handleWrongInputStream(WrongInputStream inputStream) { }

    public void handleWrongOutputStream(WrongOutputStream outputStream) { }

    public void handleWrongReader(WrongReader reader) { }

    public void handleWrongWriter(WrongWriter writer) { }

  }

  static abstract class WrongRequestContext extends RequestContext {
    protected WrongRequestContext(ApplicationContext context, DispatcherHandler dispatcherHandler) {
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