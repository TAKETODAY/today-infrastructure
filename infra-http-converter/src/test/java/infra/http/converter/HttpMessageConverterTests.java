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

package infra.http.converter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.http.StreamingHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

/**
 * Test-case for AbstractHttpMessageConverter.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class HttpMessageConverterTests {

  @Test
  void handlesAnEmptyMediaTypeListAndUnsupportedClasses() throws IOException {
    var converter = new MyHttpMessageConverter<MyType>(MediaType.TEXT_PLAIN);
    assertThat(converter.canRead(String.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(converter.canWrite(String.class, MediaType.TEXT_PLAIN)).isFalse();
    converter.setSupportedMediaTypes((MediaType[]) null);
    assertThat(converter.getSupportedMediaTypes()).isEmpty();
    var output = new MockHttpOutputMessage();
    converter.addDefaultHeaders(output, new MyType(), MediaType.APPLICATION_OCTET_STREAM);
    assertThat(output.getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    var withoutType = new MockHttpOutputMessage();
    converter.addDefaultHeaders(withoutType, new MyType(), null);
    assertThat(withoutType.getContentType()).isNull();
  }

  @Test
  void genericConverterDefersWritingAndPreservesTheDeclaredType() throws IOException {
    var converter = new TypedTextConverter();
    Type type = ResolvableType.forClassWithGenerics(List.class, String.class).getType();
    assertThat(converter.canRead((Type) String.class, null, MediaType.TEXT_PLAIN)).isTrue();
    assertThat(converter.canRead(type, null, MediaType.APPLICATION_JSON)).isFalse();
    assertThat(converter.canWrite(type, List.class, MediaType.TEXT_PLAIN)).isTrue();
    var output = mock(StreamingHttpOutputMessage.class);
    given(output.getHeaders()).willReturn(infra.http.HttpHeaders.forWritable());
    converter.write("hello", type, MediaType.TEXT_PLAIN, output);
    var captor = ArgumentCaptor.forClass(StreamingHttpOutputMessage.Body.class);
    verify(output).setBody(captor.capture());
    var body = captor.getValue();
    assertThat(body.repeatable()).isFalse();
    var bytes = new ByteArrayOutputStream();
    body.writeTo(bytes);
    assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
    assertThat(converter.writtenType).isSameAs(type);
  }

  @Test
  @SuppressWarnings("unchecked")
  void smartConverterDefaultsForwardClassBasedCallsWithoutHints() throws IOException {
    SmartHttpMessageConverter<String> converter = mock(SmartHttpMessageConverter.class, CALLS_REAL_METHODS);
    var type = ResolvableType.forClass(String.class);
    var input = new MockHttpInputMessage(new byte[0]);
    var output = new MockHttpOutputMessage();
    converter.canRead(String.class, MediaType.TEXT_PLAIN);
    converter.canWrite(String.class, MediaType.TEXT_PLAIN);
    converter.read(String.class, input);
    converter.write("hello", MediaType.TEXT_PLAIN, output);
    verify(converter).canRead(type, MediaType.TEXT_PLAIN);
    verify(converter).canWrite(type, String.class, MediaType.TEXT_PLAIN);
    verify(converter).read(type, input, null);
    verify(converter).write("hello", type, MediaType.TEXT_PLAIN, output, null);
    assertThat(converter.getSupportedMediaTypes(String.class)).isEmpty();
  }

  @Test
  void smartBaseConverterReadsThroughTheTypedMethod() throws IOException {
    var converter = new AbstractSmartHttpMessageConverter<String>(MediaType.TEXT_PLAIN) {
      @Override
      public boolean canRead(ResolvableType type, MediaType mediaType) {
        return canRead(mediaType);
      }

      @Override
      public boolean canWrite(ResolvableType type, Class<?> valueClass, MediaType mediaType) {
        return canWrite(mediaType);
      }

      @Override
      public String read(ResolvableType type, HttpInputMessage input, Map<String, Object> hints) throws IOException {
        assertThat(type.toClass()).isEqualTo(String.class);
        assertThat(hints).isNull();
        return new String(input.getBody().readAllBytes(), StandardCharsets.UTF_8);
      }

      @Override
      protected void writeInternal(String value, ResolvableType type, HttpOutputMessage output, Map<String, Object> hints) {
        throw new AssertionError("Not expected");
      }
    };
    assertThat(converter.supports(String.class)).isTrue();
    assertThat(converter.read(String.class, new MockHttpInputMessage(new byte[] { 'a' }))).isEqualTo("a");
  }

  private static final class TypedTextConverter extends AbstractGenericHttpMessageConverter<String> {

    private Type writtenType;

    TypedTextConverter() {
      setSupportedMediaTypes(MediaType.TEXT_PLAIN);
    }

    @Override
    public String read(Type type, Class<?> contextClass, HttpInputMessage input) {
      throw new AssertionError("Not expected");
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) {
      throw new AssertionError("Not expected");
    }

    @Override
    protected void writeInternal(String value, Type type, HttpOutputMessage output) throws IOException {
      this.writtenType = type;
      output.getBody().write(value.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  public void canRead() {
    MediaType mediaType = new MediaType("foo", "bar");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canRead(MyType.class, mediaType)).isTrue();
    assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isFalse();
    assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
  }

  @Test
  public void canReadWithWildcardSubtype() {
    MediaType mediaType = new MediaType("foo");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canRead(MyType.class, new MediaType("foo", "bar"))).isTrue();
    assertThat(converter.canRead(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canRead(MyType.class, MediaType.ALL)).isFalse();
  }

  @Test
  public void canWrite() {
    MediaType mediaType = new MediaType("foo", "bar");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canWrite(MyType.class, mediaType)).isTrue();
    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
  }

  @Test
  public void canWriteWithWildcardInSupportedSubtype() {
    MediaType mediaType = new MediaType("foo");
    HttpMessageConverter<MyType> converter = new MyHttpMessageConverter<>(mediaType);

    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "bar"))).isTrue();
    assertThat(converter.canWrite(MyType.class, new MediaType("foo", "*"))).isTrue();
    assertThat(converter.canWrite(MyType.class, MediaType.ALL)).isTrue();
  }

  private static class MyHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    private MyHttpMessageConverter(MediaType supportedMediaType) {
      super(supportedMediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
      return MyType.class.equals(clazz);
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
      throw new AssertionError("Not expected");
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
      throw new AssertionError("Not expected");
    }
  }

  private static class MyType {

  }

}
