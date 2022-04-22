/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.LoggingCodecSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageReader} for parsing {@code "multipart/form-data"} requests
 * to a stream of {@link PartEvent} elements.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PartEvent
 * @since 4.0 2022/4/22 9:10
 */
public class PartEventHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<PartEvent> {

  private int maxInMemorySize = 256 * 1024;

  private int maxHeadersSize = 10 * 1024;

  private Charset headersCharset = StandardCharsets.UTF_8;

  /**
   * Get the {@link #setMaxInMemorySize configured} maximum in-memory size.
   */
  public int getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  /**
   * Configure the maximum amount of memory allowed for form fields.
   * When the limit is exceeded, form fields parts are rejected with
   * {@link DataBufferLimitException}.
   *
   * <p>By default this is set to 256K.
   *
   * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
   * contents will be stored in memory
   */
  public void setMaxInMemorySize(int maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }

  /**
   * Configure the maximum amount of memory that is allowed per headers section of each part.
   * Defaults to 10K.
   *
   * @param byteCount the maximum amount of memory for headers
   */
  public void setMaxHeadersSize(int byteCount) {
    this.maxHeadersSize = byteCount;
  }

  /**
   * Set the character set used to decode headers.
   * Defaults to UTF-8 as per RFC 7578.
   *
   * @param headersCharset the charset to use for decoding headers
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 Section 5.1</a>
   */
  public void setHeadersCharset(Charset headersCharset) {
    Assert.notNull(headersCharset, "HeadersCharset must not be null");
    this.headersCharset = headersCharset;
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    return PartEvent.class.equals(elementType.toClass()) &&
            (mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
  }

  @Override
  public Mono<PartEvent> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
          Map<String, Object> hints) {
    return Mono.error(
            new UnsupportedOperationException("Cannot read multipart request body into single PartEvent"));
  }

  @Override
  public Flux<PartEvent> read(ResolvableType elementType, ReactiveHttpInputMessage message,
          Map<String, Object> hints) {

    return Flux.defer(() -> {
      byte[] boundary = MultipartUtils.boundary(message, this.headersCharset);
      if (boundary == null) {
        return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
                message.getHeaders().getContentType() + "\""));
      }
      return MultipartParser.parse(message.getBody(), boundary, this.maxHeadersSize, this.headersCharset)
              .windowUntil(t -> t instanceof MultipartParser.HeadersToken, true)
              .concatMap(tokens -> tokens.switchOnFirst((signal, flux) -> {
                if (signal.hasValue()) {
                  MultipartParser.HeadersToken headersToken = (MultipartParser.HeadersToken) signal.get();
                  Assert.state(headersToken != null, "Signal should be headers token");

                  HttpHeaders headers = headersToken.getHeaders();
                  Flux<MultipartParser.BodyToken> bodyTokens =
                          flux.filter(t -> t instanceof MultipartParser.BodyToken)
                                  .cast(MultipartParser.BodyToken.class);
                  return createEvents(headers, bodyTokens);
                }
                else {
                  // complete or error signal
                  return flux.cast(PartEvent.class);
                }
              }));
    });
  }

  private Publisher<? extends PartEvent> createEvents(HttpHeaders headers, Flux<MultipartParser.BodyToken> bodyTokens) {
    if (MultipartUtils.isFormField(headers)) {
      Flux<DataBuffer> contents = bodyTokens.map(MultipartParser.BodyToken::getBuffer);
      return DataBufferUtils.join(contents, this.maxInMemorySize)
              .map(content -> {
                String value = content.toString(MultipartUtils.charset(headers));
                DataBufferUtils.release(content);
                return DefaultPartEvents.form(headers, value);
              })
              .switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.form(headers)));
    }
    else if (headers.getContentDisposition().getFilename() != null) {
      return bodyTokens
              .map(body -> DefaultPartEvents.file(headers, body.getBuffer(), body.isLast()))
              .switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.file(headers)));
    }
    else {
      return bodyTokens
              .map(body -> DefaultPartEvents.create(headers, body.getBuffer(), body.isLast()))
              .switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.create(headers))); // empty body
    }

  }

}
