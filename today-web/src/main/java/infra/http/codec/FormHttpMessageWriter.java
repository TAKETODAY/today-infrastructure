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

package infra.http.codec;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBuffer;
import infra.http.MediaType;
import infra.http.ReactiveHttpOutputMessage;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.LogFormatUtils;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * {@link HttpMessageWriter} for writing a {@code MultiValueMap<String, String>}
 * as HTML form data, i.e. {@code "application/x-www-form-urlencoded"}, to the
 * body of a request.
 *
 * <p>Note that unless the media type is explicitly set to
 * {@link MediaType#APPLICATION_FORM_URLENCODED}, the {@link #canWrite} method
 * will need generic type information to confirm the target map has String values.
 * This is because a MultiValueMap with non-String values can be used to write
 * multipart requests.
 *
 * <p>To support both form data and multipart requests, consider using
 * {@link infra.http.codec.multipart.MultipartHttpMessageWriter}
 * configured with this writer as the fallback for writing plain form data.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.http.codec.multipart.MultipartHttpMessageWriter
 * @since 4.0
 */
public class FormHttpMessageWriter extends LoggingCodecSupport implements HttpMessageWriter<MultiValueMap<String, String>> {

  private static final List<MediaType> MEDIA_TYPES =
          Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);

  private static final ResolvableType MULTI_VALUE_TYPE =
          ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

  private Charset defaultCharset = StandardCharsets.UTF_8;

  /**
   * Set the default character set to use for writing form data when the response
   * Content-Type header does not explicitly specify it.
   * <p>By default this is set to "UTF-8".
   */
  public void setDefaultCharset(Charset charset) {
    Assert.notNull(charset, "Charset is required");
    this.defaultCharset = charset;
  }

  /**
   * Return the configured default charset.
   */
  public Charset getDefaultCharset() {
    return this.defaultCharset;
  }

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return MEDIA_TYPES;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
      if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
        // Optimistically, any MultiValueMap with or without generics
        return true;
      }
      if (mediaType == null) {
        // Only String-based MultiValueMap
        return MULTI_VALUE_TYPE.isAssignableFrom(elementType);
      }
    }
    return false;
  }

  @Override
  public Mono<Void> write(Publisher<? extends MultiValueMap<String, String>> inputStream, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    mediaType = getMediaType(mediaType);
    message.getHeaders().setContentType(mediaType);

    Charset charset = mediaType.getCharset() != null ? mediaType.getCharset() : getDefaultCharset();

    return Mono.from(inputStream).flatMap(form -> {
      if (logger.isDebugEnabled()) {
        logFormData(form, hints);
      }
      String value = serializeForm(form, charset);
      ByteBuffer byteBuffer = charset.encode(value);
      DataBuffer buffer = message.bufferFactory().wrap(byteBuffer); // wrapping only, no allocation
      message.getHeaders().setContentLength(byteBuffer.remaining());
      return message.writeWith(Mono.just(buffer));
    });
  }

  protected MediaType getMediaType(@Nullable MediaType mediaType) {
    if (mediaType == null) {
      return MediaType.APPLICATION_FORM_URLENCODED;
    }
    // Some servers don't handle charset parameter and spec is unclear,
    // Add it only if it is not DEFAULT_CHARSET.
    if (mediaType.getCharset() == null) {
      Charset defaultCharset = getDefaultCharset();
      if (defaultCharset != Constant.DEFAULT_CHARSET) {
        return mediaType.withCharset(defaultCharset);
      }
    }
    return mediaType;
  }

  private void logFormData(MultiValueMap<String, String> form, Map<String, Object> hints) {
    LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Writing " +
            (isEnableLoggingRequestDetails() ? LogFormatUtils.formatValue(form, !traceOn)
                    : "form fields " + form.keySet() + " (content masked)"));
  }

  protected String serializeForm(MultiValueMap<String, String> formData, Charset charset) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
      String name = entry.getKey();
      List<String> values = entry.getValue();
      for (String value : values) {
        if (!builder.isEmpty()) {
          builder.append('&');
        }
        builder.append(URLEncoder.encode(name, charset));
        if (value != null) {
          builder.append('=');
          builder.append(URLEncoder.encode(value, charset));
        }
      }
    }
    return builder.toString();
  }

}
