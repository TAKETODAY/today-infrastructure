/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import java.lang.reflect.Type;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

/**
 * A specialization of {@link HttpMessageConverter} that can convert an HTTP request
 * into a target object of a specified generic type and a source object of a specified
 * generic type into an HTTP response.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @see ParameterizedTypeReference
 * @since 4.0
 */
public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {

  /**
   * Indicates whether the given type can be read by this converter.
   * This method should perform the same checks than
   * {@link HttpMessageConverter#canRead(Class, MediaType)} with additional ones
   * related to the generic type.
   *
   * @param type the (potentially generic) type to test for readability
   * @param contextClass a context class for the target type, for example a class
   * in which the target type appears in a method signature (can be {@code null})
   * @param mediaType the media type to read, can be {@code null} if not specified.
   * Typically the value of a {@code Content-Type} header.
   * @return {@code true} if readable; {@code false} otherwise
   */
  boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);

  /**
   * Read an object of the given type form the given input message, and returns it.
   *
   * @param type the (potentially generic) type of object to return. This type must have
   * previously been passed to the {@link #canRead canRead} method of this interface,
   * which must have returned {@code true}.
   * @param contextClass a context class for the target type, for example a class
   * in which the target type appears in a method signature (can be {@code null})
   * @param inputMessage the HTTP input message to read from
   * @return the converted object
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotReadableException in case of conversion errors
   */
  T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException;

  /**
   * Indicates whether the given class can be written by this converter.
   * <p>This method should perform the same checks than
   * {@link HttpMessageConverter#canWrite(Class, MediaType)} with additional ones
   * related to the generic type.
   *
   * @param type the (potentially generic) type to test for writability
   * (can be {@code null} if not specified)
   * @param clazz the source object class to test for writability
   * @param mediaType the media type to write (can be {@code null} if not specified);
   * typically the value of an {@code Accept} header.
   * @return {@code true} if writable; {@code false} otherwise
   */
  boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);

  /**
   * Write an given object to the given output message.
   *
   * @param t the object to write to the output message. The type of this object must
   * have previously been passed to the {@link #canWrite canWrite} method of this
   * interface, which must have returned {@code true}.
   * @param type the (potentially generic) type of object to write. This type must have
   * previously been passed to the {@link #canWrite canWrite} method of this interface,
   * which must have returned {@code true}. Can be {@code null} if not specified.
   * @param contentType the content type to use when writing. May be {@code null} to
   * indicate that the default content type of the converter must be used. If not
   * {@code null}, this media type must have previously been passed to the
   * {@link #canWrite canWrite} method of this interface, which must have returned
   * {@code true}.
   * @param outputMessage the message to write to
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException;

}
