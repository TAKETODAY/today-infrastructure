/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.StreamingHttpOutputMessage;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for most {@link GenericHttpMessageConverter} implementations.
 *
 * @param <T> the converted object type
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractGenericHttpMessageConverter<T>
        extends AbstractHttpMessageConverter<T> implements GenericHttpMessageConverter<T> {

  /**
   * Construct an {@code AbstractGenericHttpMessageConverter} with no supported media types.
   *
   * @see #setSupportedMediaTypes
   */
  protected AbstractGenericHttpMessageConverter() { }

  /**
   * Construct an {@code AbstractGenericHttpMessageConverter} with one supported media type.
   *
   * @param supportedMediaType the supported media type
   */
  protected AbstractGenericHttpMessageConverter(MediaType supportedMediaType) {
    super(supportedMediaType);
  }

  /**
   * Construct an {@code AbstractGenericHttpMessageConverter} with multiple supported media type.
   *
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractGenericHttpMessageConverter(MediaType... supportedMediaTypes) {
    super(supportedMediaTypes);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return true;
  }

  @Override
  public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
    return type instanceof Class<?> clazz ? canRead(clazz, mediaType) : canRead(mediaType);
  }

  @Override
  public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
    return canWrite(clazz, mediaType);
  }

  /**
   * This implementation sets the default headers by calling {@link #addDefaultHeaders},
   * and then calls {@link #writeInternal}.
   */
  @Override
  public final void write(final T t, @Nullable final Type type, @Nullable MediaType contentType,
          HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    final HttpHeaders headers = outputMessage.getHeaders();
    addDefaultHeaders(headers, t, contentType);

    if (outputMessage instanceof StreamingHttpOutputMessage streaming) {
      streaming.setBody(new StreamingHttpOutputMessage.Body() {
        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
          writeInternal(t, type, new HttpOutputMessage() {
            @Override
            public OutputStream getBody() {
              return outputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
              return headers;
            }
          });
        }

        @Override
        public boolean repeatable() {
          return supportsRepeatableWrites(t);
        }
      });
    }
    else {
      writeInternal(t, type, outputMessage);
      outputMessage.getBody().flush();
    }
  }

  @Override
  protected void writeInternal(T t, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {
    writeInternal(t, null, outputMessage);
  }

  /**
   * Abstract template method that writes the actual body. Invoked from {@link #write}.
   *
   * @param t the object to write to the output message
   * @param type the type of object to write (may be {@code null})
   * @param outputMessage the HTTP output message to write to
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  protected abstract void writeInternal(T t, @Nullable Type type, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException;

}
