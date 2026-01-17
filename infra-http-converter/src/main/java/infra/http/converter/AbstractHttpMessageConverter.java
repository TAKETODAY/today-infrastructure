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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpLogging;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.StreamingHttpOutputMessage;
import infra.lang.Assert;
import infra.logging.Logger;

/**
 * Abstract base class for most {@link HttpMessageConverter} implementations.
 *
 * <p>This base class adds support for setting supported {@code MediaTypes}, through the
 * {@link #setSupportedMediaTypes supportedMediaTypes} bean property. It also adds
 * support for {@code Content-Type} and {@code Content-Length} when writing to output messages.
 *
 * @param <T> the converted object type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

  /** Logger available to subclasses. */
  protected final Logger logger = HttpLogging.forLogName(getClass());

  private List<MediaType> supportedMediaTypes = Collections.emptyList();

  private @Nullable Charset defaultCharset;

  /**
   * Construct an {@code AbstractHttpMessageConverter} with no supported media types.
   *
   * @see #setSupportedMediaTypes
   */
  protected AbstractHttpMessageConverter() {
  }

  /**
   * Construct an {@code AbstractHttpMessageConverter} with multiple supported media types.
   *
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
    this(null, supportedMediaTypes);
  }

  /**
   * Construct an {@code AbstractHttpMessageConverter} with a default charset and
   * multiple supported media types.
   *
   * @param defaultCharset the default character set
   * @param supportedMediaTypes the supported media types
   */
  protected AbstractHttpMessageConverter(@Nullable Charset defaultCharset, MediaType... supportedMediaTypes) {
    this.defaultCharset = defaultCharset;
    setSupportedMediaTypes(supportedMediaTypes);
  }

  /**
   * Set the list of {@link MediaType} objects supported by this converter.
   *
   * @since 5.0
   */
  public void setSupportedMediaTypes(MediaType @Nullable ... supportedMediaTypes) {
    if (supportedMediaTypes == null) {
      this.supportedMediaTypes = Collections.emptyList();
    }
    else {
      this.supportedMediaTypes = List.of(supportedMediaTypes);
    }
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
   * Returns {@code true} if any of the {@linkplain #setSupportedMediaTypes
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

    addDefaultHeaders(outputMessage, t, contentType);

    if (outputMessage instanceof StreamingHttpOutputMessage streaming) {
      streaming.setBody(new StreamingHttpOutputMessage.Body() {

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
          writeInternal(t, new BodyHttpOutputMessage(outputMessage, outputStream));
        }

        @Override
        public boolean repeatable() {
          return supportsRepeatableWrites(t);
        }
      });
    }
    else {
      writeInternal(t, outputMessage);
    }
  }

  /**
   * Add default headers to the output message.
   * <p>This implementation delegates to {@link #getDefaultContentType(Object)} if a
   * content type was not provided, set if necessary the default character set, calls
   * {@link #getContentLength}, and sets the corresponding headers.
   *
   * @since 5.0
   */
  public void addDefaultHeaders(HttpOutputMessage message, T t, @Nullable MediaType contentType) throws IOException {
    String contentTypeString = message.getContentTypeAsString();
    if (contentTypeString == null) {
      MediaType contentTypeToUse = contentType;
      if (contentType == null || !contentType.isConcrete()) {
        contentTypeToUse = getDefaultContentType(t);
      }
      else if (MediaType.APPLICATION_OCTET_STREAM.equalsTypeAndSubtype(contentType)) {
        MediaType mediaType = getDefaultContentType(t);
        if (mediaType != null) {
          contentTypeToUse = mediaType;
        }
      }
      if (contentTypeToUse != null) {
        if (contentTypeToUse.getCharset() == null) {
          Charset defaultCharset = getDefaultCharset();
          if (defaultCharset != null) {
            contentTypeToUse = contentTypeToUse.withCharset(defaultCharset);
          }
        }
        message.setContentType(contentTypeToUse);
      }
    }

    if (!MediaType.TEXT_EVENT_STREAM_VALUE.equals(contentTypeString)
            && !message.containsHeader(HttpHeaders.TRANSFER_ENCODING)
            && message.getContentLength() < 0) {
      Long contentLength = getContentLength(t, message);
      if (contentLength != null) {
        message.setContentLength(contentLength);
      }
    }
  }

  /**
   * Returns the default content type for the given type. Called when {@link #write}
   * is invoked without a specified content type parameter.
   * <p>By default, this returns the first element of the
   * {@link #setSupportedMediaTypes supportedMediaTypes} property, if any.
   * Can be overridden in subclasses.
   *
   * @param t the type to return the content type for
   * @return the content type, or {@code null} if not known
   */
  @Nullable
  protected MediaType getDefaultContentType(T t) throws IOException {
    List<MediaType> mediaTypes = getSupportedMediaTypes();
    return !mediaTypes.isEmpty() ? mediaTypes.get(0) : null;
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
  protected Long getContentLength(T t, HttpOutputMessage message) throws IOException {
    return null;
  }

  /**
   * Indicates whether this message converter can
   * {@linkplain #write(Object, MediaType, HttpOutputMessage) write} the
   * given object multiple times.
   *
   * <p>Default implementation returns {@code false}.
   *
   * @param t the object t
   * @return {@code true} if {@code t} can be written repeatedly;
   * {@code false} otherwise
   */
  protected boolean supportsRepeatableWrites(T t) {
    return false;
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
