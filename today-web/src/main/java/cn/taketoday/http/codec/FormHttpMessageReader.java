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

package cn.taketoday.http.codec;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of an {@link HttpMessageReader} to read HTML form data, i.e.
 * request body with media type {@code "application/x-www-form-urlencoded"}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class FormHttpMessageReader extends LoggingCodecSupport
        implements HttpMessageReader<MultiValueMap<String, String>> {

  /**
   * The default charset used by the reader.
   */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final ResolvableType MULTIVALUE_STRINGS_TYPE =
          ResolvableType.fromClassWithGenerics(MultiValueMap.class, String.class, String.class);

  private Charset defaultCharset = DEFAULT_CHARSET;

  private int maxInMemorySize = 256 * 1024;

  /**
   * Set the default character set to use for reading form data when the
   * request Content-Type header does not explicitly specify it.
   * <p>By default this is set to "UTF-8".
   */
  public void setDefaultCharset(Charset charset) {
    Assert.notNull(charset, "Charset must not be null");
    this.defaultCharset = charset;
  }

  /**
   * Return the configured default charset.
   */
  public Charset getDefaultCharset() {
    return this.defaultCharset;
  }

  /**
   * Set the max number of bytes for input form data. As form data is buffered
   * before it is parsed, this helps to limit the amount of buffering. Once
   * the limit is exceeded, {@link DataBufferLimitException} is raised.
   * <p>By default this is set to 256K.
   *
   * @param byteCount the max number of bytes to buffer, or -1 for unlimited
   */
  public void setMaxInMemorySize(int byteCount) {
    this.maxInMemorySize = byteCount;
  }

  /**
   * Return the {@link #setMaxInMemorySize configured} byte count limit.
   */
  public int getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    boolean multiValueUnresolved =
            elementType.hasUnresolvableGenerics() &&
                    MultiValueMap.class.isAssignableFrom(elementType.toClass());

    return (MULTIVALUE_STRINGS_TYPE.isAssignableFrom(elementType) || multiValueUnresolved)
            && (mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType));
  }

  @Override
  public Flux<MultiValueMap<String, String>> read(
          ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

    return Flux.from(readMono(elementType, message, hints));
  }

  @Override
  public Mono<MultiValueMap<String, String>> readMono(
          ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

    MediaType contentType = message.getHeaders().getContentType();
    Charset charset = getMediaTypeCharset(contentType);

    return DataBufferUtils.join(message.getBody(), this.maxInMemorySize)
            .map(buffer -> {
              String body = buffer.toString(charset);
              DataBufferUtils.release(buffer);
              MultiValueMap<String, String> formData = parseFormData(charset, body);
              if (logger.isDebugEnabled()) {
                logFormData(formData, hints);
              }
              return formData;
            });
  }

  private void logFormData(MultiValueMap<String, String> formData, Map<String, Object> hints) {
    LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Read " +
            (isEnableLoggingRequestDetails() ?
             LogFormatUtils.formatValue(formData, !traceOn) :
             "form fields " + formData.keySet() + " (content masked)"));
  }

  private Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
    if (mediaType != null && mediaType.getCharset() != null) {
      return mediaType.getCharset();
    }
    else {
      return getDefaultCharset();
    }
  }

  private MultiValueMap<String, String> parseFormData(Charset charset, String body) {
    String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
    MultiValueMap<String, String> result = MultiValueMap.fromLinkedHashMap(pairs.length);
    for (String pair : pairs) {
      int idx = pair.indexOf('=');
      if (idx == -1) {
        result.add(URLDecoder.decode(pair, charset), null);
      }
      else {
        String name = URLDecoder.decode(pair.substring(0, idx), charset);
        String value = URLDecoder.decode(pair.substring(idx + 1), charset);
        result.add(name, value);
      }
    }
    return result;
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
  }

}
