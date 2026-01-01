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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.StreamingHttpOutputMessage;

/**
 * Abstract base class for most {@link SmartHttpMessageConverter} implementations.
 *
 * @param <T> the converted object type
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class AbstractSmartHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
        implements SmartHttpMessageConverter<T> {

  /**
   * Construct an {@code AbstractSmartHttpMessageConverter} with no supported media types.
   *
   * @see #setSupportedMediaTypes
   */
  protected AbstractSmartHttpMessageConverter() {
  }

  /**
   * Construct an {@code AbstractSmartHttpMessageConverter} with one supported media type.
   *
   * @param supportedMediaType the supported media type
   */
  protected AbstractSmartHttpMessageConverter(MediaType supportedMediaType) {
    super(supportedMediaType);
  }

  /**
   * Construct an {@code AbstractSmartHttpMessageConverter} with multiple supported media type.
   *
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractSmartHttpMessageConverter(MediaType... supportedMediaTypes) {
    super(supportedMediaTypes);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return true;
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return canRead(ResolvableType.forClass(clazz), mediaType);
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return canWrite(ResolvableType.forClass(clazz), clazz, mediaType);
  }

  /**
   * This implementation sets the default headers by calling {@link #addDefaultHeaders},
   * and then calls {@link #writeInternal}.
   */
  @Override
  public final void write(T t, ResolvableType type, @Nullable MediaType contentType,
          HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints)
          throws IOException, HttpMessageNotWritableException {

    HttpHeaders headers = outputMessage.getHeaders();
    addDefaultHeaders(headers, t, contentType);

    if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
      streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
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
          }, hints);
        }

        @Override
        public boolean repeatable() {
          return supportsRepeatableWrites(t);
        }
      });
    }
    else {
      writeInternal(t, type, outputMessage, hints);
      outputMessage.getBody().flush();
    }
  }

  @Override
  protected void writeInternal(T t, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    writeInternal(t, ResolvableType.NONE, outputMessage, null);
  }

  /**
   * Abstract template method that writes the actual body. Invoked from
   * {@link #write(Object, ResolvableType, MediaType, HttpOutputMessage, Map)}.
   *
   * @param t the object to write to the output message
   * @param type the type of object to write
   * @param outputMessage the HTTP output message to write to
   * @param hints additional information about how to encode
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  protected abstract void writeInternal(T t, ResolvableType type, HttpOutputMessage outputMessage,
          @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException;

  @Override
  protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return read(ResolvableType.forClass(clazz), inputMessage, null);
  }
}
