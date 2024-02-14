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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

  private int maxParts = -1;

  private long maxPartSize = -1;

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
   * Specify the maximum number of parts allowed in a given multipart request.
   * <p>By default this is set to -1, meaning that there is no maximum.
   */
  public void setMaxParts(int maxParts) {
    this.maxParts = maxParts;
  }

  /**
   * Configure the maximum size allowed for any part.
   * <p>By default this is set to -1, meaning that there is no maximum.
   */
  public void setMaxPartSize(long maxPartSize) {
    this.maxPartSize = maxPartSize;
  }

  /**
   * Set the character set used to decode headers.
   * <p>Defaults to UTF-8 as per RFC 7578.
   *
   * @param headersCharset the charset to use for decoding headers
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 Section 5.1</a>
   */
  public void setHeadersCharset(Charset headersCharset) {
    Assert.notNull(headersCharset, "Charset is required");
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
      Flux<MultipartParser.Token> allPartsTokens = MultipartParser.parse(message.getBody(), boundary,
              this.maxHeadersSize, this.headersCharset);

      AtomicInteger partCount = new AtomicInteger();
      return allPartsTokens
              .windowUntil(t -> t instanceof MultipartParser.HeadersToken, true)
              .concatMap(partTokens -> {
                if (tooManyParts(partCount)) {
                  return Mono.error(new DecodingException("Too many parts (" + partCount.get() + "/" +
                          this.maxParts + " allowed)"));
                }
                else {
                  return partTokens.switchOnFirst((signal, flux) -> {
                    if (signal.hasValue()) {
                      MultipartParser.HeadersToken headersToken = (MultipartParser.HeadersToken) signal.get();
                      Assert.state(headersToken != null, "Signal should be headers token");

                      HttpHeaders headers = headersToken.getHeaders();
                      var bodyTokens = flux.ofType(MultipartParser.BodyToken.class);
                      return createEvents(headers, bodyTokens);
                    }
                    else {
                      // complete or error signal
                      return flux.cast(PartEvent.class);
                    }
                  });
                }
              });
    });
  }

  private boolean tooManyParts(AtomicInteger partCount) {
    int count = partCount.incrementAndGet();
    return this.maxParts > 0 && count > this.maxParts;
  }

  private Publisher<? extends PartEvent> createEvents(HttpHeaders headers, Flux<MultipartParser.BodyToken> bodyTokens) {
    if (MultipartUtils.isFormField(headers)) {
      Flux<DataBuffer> contents = bodyTokens.map(MultipartParser.BodyToken::getBuffer);
      int maxSize;
      if (this.maxPartSize == -1) {
        maxSize = this.maxInMemorySize;
      }
      else {
        // maxInMemorySize is an int, so we can safely cast the long result of Math.min
        maxSize = (int) Math.min(this.maxInMemorySize, this.maxPartSize);
      }
      return DataBufferUtils.join(contents, maxSize)
              .map(content -> {
                String value = content.toString(MultipartUtils.charset(headers));
                DataBufferUtils.release(content);
                return DefaultPartEvents.form(headers, value);
              })
              .switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.form(headers)));
    }
    else {
      boolean isFilePart = headers.getContentDisposition().getFilename() != null;
      AtomicLong partSize = new AtomicLong();
      return bodyTokens
              .concatMap(body -> {
                DataBuffer buffer = body.getBuffer();
                if (tooLarge(partSize, buffer)) {
                  DataBufferUtils.release(buffer);
                  return Mono.error(new DataBufferLimitException("Part exceeded the limit of " +
                          this.maxPartSize + " bytes"));
                }
                else {
                  return isFilePart ? Mono.just(DefaultPartEvents.file(headers, buffer, body.isLast()))
                                    : Mono.just(DefaultPartEvents.create(headers, body.getBuffer(), body.isLast()));
                }
              })
              .switchIfEmpty(Mono.fromCallable(() ->
                      isFilePart ? DefaultPartEvents.file(headers) : DefaultPartEvents.create(headers)));
    }
  }

  private boolean tooLarge(AtomicLong partSize, DataBuffer buffer) {
    if (this.maxPartSize != -1) {
      long size = partSize.addAndGet(buffer.readableByteCount());
      return size > this.maxPartSize;
    }
    else {
      return false;
    }
  }
}
