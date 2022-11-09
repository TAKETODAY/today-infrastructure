/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.SimpleHttpOutputMessage;
import cn.taketoday.http.StreamingHttpOutputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;

/**
 * Abstract base class for most {@link HttpMessageConverter} implementations.
 *
 * <p>This base class adds support for setting supported {@code MediaTypes}, through the
 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} bean property. It also adds
 * support for {@code Content-Type} and {@code Content-Length} when writing to output messages.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

  /** Logger available to subclasses. */
  protected final Logger logger = HttpLogging.forLogName(getClass());

  private List<MediaType> supportedMediaTypes = Collections.emptyList();

  @Nullable
  private Charset defaultCharset;

  /**
   * Construct an {@code AbstractHttpMessageConverter} with no supported media types.
   *
   * @see #setSupportedMediaTypes
   */
  protected AbstractHttpMessageConverter() { }

  /**
   * Construct an {@code AbstractHttpMessageConverter} with one supported media type.
   *
   * @param supportedMediaType the supported media type
   */
  protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
    setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
  }

  /**
   * Construct an {@code AbstractHttpMessageConverter} with multiple supported media types.
   *
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
    setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
  }

  /**
   * Construct an {@code AbstractHttpMessageConverter} with a default charset and
   * multiple supported media types.
   *
   * @param defaultCharset the default character set
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
    this.defaultCharset = defaultCharset;
    setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
  }

  /**
   * Set the list of {@link MediaType} objects supported by this converter.
   */
  public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
    Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
    this.supportedMediaTypes = List.copyOf(supportedMediaTypes);
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

  /**
   * Set the default character set, if any.
   */
  public void setDefaultCharset(@Nullable Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  /**
   * Return the default character set, if any.
   */
  @Nullable
  public Charset getDefaultCharset() {
    return this.defaultCharset;
  }

  /**
   * This implementation checks if the given class is {@linkplain #supports(Class) supported},
   * and if the {@linkplain #getSupportedMediaTypes() supported media types}
   * {@linkplain MediaType#includes(MediaType) include} the given media type.
   */
  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return supports(clazz) && canRead(mediaType);
  }

  /**
   * Returns {@code true} if any of the {@linkplain #setSupportedMediaTypes(List)
   * supported} media types {@link MediaType#includes(MediaType) include} the
   * given media type.
   *
   * @param mediaType the media type to read, can be {@code null} if not specified.
   * Typically the value of a {@code Content-Type} header.
   * @return {@code true} if the supported media types include the media type,
   * or if the media type is {@code null}
   */
  protected boolean canRead(@Nullable MediaType mediaType) {
    if (mediaType == null) {
      return true;
    }
    for (MediaType supportedMediaType : getSupportedMediaTypes()) {
      if (supportedMediaType.includes(mediaType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This implementation checks if the given class is
   * {@linkplain #supports(Class) supported}, and if the
   * {@linkplain #getSupportedMediaTypes() supported} media types
   * {@linkplain MediaType#includes(MediaType) include} the given media type.
   */
  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return supports(clazz) && canWrite(mediaType);
  }

  /**
   * Returns {@code true} if the given media type includes any of the
   * {@linkplain #setSupportedMediaTypes(List) supported media types}.
   *
   * @param mediaType the media type to write, can be {@code null} if not specified.
   * Typically the value of an {@code Accept} header.
   * @return {@code true} if the supported media types are compatible with the media type,
   * or if the media type is {@code null}
   */
  protected boolean canWrite(@Nullable MediaType mediaType) {
    if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
      return true;
    }
    for (MediaType supportedMediaType : getSupportedMediaTypes()) {
      if (supportedMediaType.isCompatibleWith(mediaType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This implementation simple delegates to {@link #readInternal(Class, HttpInputMessage)}.
   * Future implementations might add some default behavior, however.
   */
  @Override
  public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return readInternal(clazz, inputMessage);
  }

  /**
   * This implementation sets the default headers by calling {@link #addDefaultHeaders},
   * and then calls {@link #writeInternal}.
   */
  @Override
  public final void write(final T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    final HttpHeaders headers = outputMessage.getHeaders();
    addDefaultHeaders(headers, t, contentType);

    if (outputMessage instanceof StreamingHttpOutputMessage streamingOutput) {
      // FIXME SimpleHttpOutputMessage
      streamingOutput.setBody(outputStream -> writeInternal(t, new SimpleHttpOutputMessage(headers, outputStream)));
    }
    else {
      writeInternal(t, outputMessage);
      outputMessage.getBody().flush();
    }
  }

  /**
   * Add default headers to the output message.
   * <p>This implementation delegates to {@link #getDefaultContentType(Object)} if a
   * content type was not provided, set if necessary the default character set, calls
   * {@link #getContentLength}, and sets the corresponding headers.
   */
  public void addDefaultHeaders(
          HttpHeaders headers, T t, @Nullable MediaType contentType) throws IOException {
    String contentTypeString = headers.getFirst(HttpHeaders.CONTENT_TYPE);
    if (contentTypeString == null) {
      MediaType contentTypeToUse = contentType;
      if (contentType == null || !contentType.isConcrete()) {
        contentTypeToUse = getDefaultContentType(t);
      }
      else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
        MediaType mediaType = getDefaultContentType(t);
        contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
      }
      if (contentTypeToUse != null) {
        if (contentTypeToUse.getCharset() == null) {
          Charset defaultCharset = getDefaultCharset();
          if (defaultCharset != null) {
            contentTypeToUse = new MediaType(contentTypeToUse, defaultCharset);
          }
        }
        headers.setContentType(contentTypeToUse);
      }
    }
    else if (!MediaType.TEXT_EVENT_STREAM_VALUE.equals(contentTypeString)
            && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
            && headers.getContentLength() < 0) {
      Long contentLength = getContentLength(t, headers.getContentType());
      if (contentLength != null) {
        headers.setContentLength(contentLength);
      }
    }
  }

  /**
   * Returns the default content type for the given type. Called when {@link #write}
   * is invoked without a specified content type parameter.
   * <p>By default, this returns the first element of the
   * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property, if any.
   * Can be overridden in subclasses.
   *
   * @param t the type to return the content type for
   * @return the content type, or {@code null} if not known
   */
  @Nullable
  protected MediaType getDefaultContentType(T t) throws IOException {
    List<MediaType> mediaTypes = getSupportedMediaTypes();
    return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
  }

  /**
   * Returns the content length for the given type.
   * <p>By default, this returns {@code null}, meaning that the content length is unknown.
   * Can be overridden in subclasses.
   *
   * @param t the type to return the content length for
   * @return the content length, or {@code null} if not known
   */
  @Nullable
  protected Long getContentLength(T t, @Nullable MediaType contentType) throws IOException {
    return null;
  }

  /**
   * Indicates whether the given class is supported by this converter.
   *
   * @param clazz the class to test for support
   * @return {@code true} if supported; {@code false} otherwise
   */
  protected abstract boolean supports(Class<?> clazz);

  /**
   * Abstract template method that reads the actual object. Invoked from {@link #read}.
   *
   * @param clazz the type of object to return
   * @param inputMessage the HTTP input message to read from
   * @return the converted object
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotReadableException in case of conversion errors
   */
  protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException;

  /**
   * Abstract template method that writes the actual body. Invoked from {@link #write}.
   *
   * @param t the object to write to the output message
   * @param outputMessage the HTTP output message to write to
   * @throws IOException in case of I/O errors
   * @throws HttpMessageNotWritableException in case of conversion errors
   */
  protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException;

}
