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

package infra.http.codec;

import org.jspecify.annotations.Nullable;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBufferLimitException;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.MediaType;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.LinkedMultiValueMap;
import infra.util.LogFormatUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of an {@link HttpMessageReader} to read HTML form data, i.e.
 * request body with media type {@code "application/x-www-form-urlencoded"}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FormHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<MultiValueMap<String, String>> {

  private static final ResolvableType MULTIVALUE_STRINGS_TYPE =
          ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

  private Charset defaultCharset = Constant.DEFAULT_CHARSET;

  private int maxInMemorySize = 256 * 1024;

  /**
   * Set the default character set to use for reading form data when the
   * request Content-Type header does not explicitly specify it.
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
  public Flux<MultiValueMap<String, String>> read(ResolvableType elementType,
          ReactiveHttpInputMessage message, Map<String, Object> hints) {

    return Flux.from(readMono(elementType, message, hints));
  }

  @Override
  public Mono<MultiValueMap<String, String>> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    MediaType contentType = message.getContentType();
    Charset charset = getMediaTypeCharset(contentType);

    return DataBufferUtils.join(message.getBody(), this.maxInMemorySize)
            .map(buffer -> {
              String body = buffer.toString(charset);
              buffer.release();
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
    LinkedMultiValueMap<String, String> result = MultiValueMap.forLinkedHashMap(pairs.length);
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
