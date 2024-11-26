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

package infra.http.converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.lang.Nullable;

/**
 * Strategy interface for converting from and to HTTP requests and responses.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public interface HttpMessageConverter<T> {

  /**
   * Indicates whether the given class can be read by this converter.
   *
   * @param clazz the class to test for readability
   * @param mediaType the media type to read (can be {@code null} if not specified);
   * typically the value of a {@code Content-Type} header.
   * @return {@code true} if readable; {@code false} otherwise
   */
  boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

  /**
   * Indicates whether the given class can be written by this converter.
   *
   * @param clazz the class to test for writability
   * @param mediaType the media type to write (can be {@code null} if not specified);
   * typically the value of an {@code Accept} header.
   * @return {@code true} if writable; {@code false} otherwise
   */
  boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

  /**
   * Return the list of media types supported by this converter. The list may
   * not apply to every possible target element type and calls to this method
   * should typically be guarded via {@link #canWrite(Class, MediaType)
   * canWrite(clazz, null}. The list may also exclude MIME types supported
   * only for a specific class. Alternatively, use
   * {@link #getSupportedMediaTypes(Class)} for a more precise list.
   *
   * @return the list of supported media types
   */
  List<MediaType> getSupportedMediaTypes();

  /**
   * Return the list of media types supported by this converter for the given
   * class. The list may differ from {@link #getSupportedMediaTypes()} if the
   * converter does not support the given Class or if it supports it only for
   * a subset of media types.
   *
   * @param clazz the type of class to check
   * @return the list of media types supported for the given class
   */
  default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
    return canRead(clazz, null) || canWrite(clazz, null)
            ? getSupportedMediaTypes() : Collections.emptyList();
  }

  /**
   * Read an object of the given type from the given input message, and returns it.
   *
   * @param clazz the type of object to return. This type must have previously been passed to the
   * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
   * @param inputMessage the HTTP input message to read from
   * @return the converted object
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotReadableException in case of conversion errors
   */
  T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException;

  /**
   * Write an given object to the given output message.
   *
   * @param t the object to write to the output message. The type of this object must have previously been
   * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
   * @param contentType the content type to use when writing. May be {@code null} to indicate that the
   * default content type of the converter must be used. If not {@code null}, this media type must have
   * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
   * returned {@code true}.
   * @param outputMessage the message to write to
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException;

}
