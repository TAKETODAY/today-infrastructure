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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.LoggingCodecSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Default {@code HttpMessageReader} for parsing {@code "multipart/form-data"}
 * requests to a stream of {@link Part}s.
 *
 * <p>In default, non-streaming mode, this message reader stores the
 * {@linkplain Part#content() contents} of parts smaller than
 * {@link #setMaxInMemorySize(int) maxInMemorySize} in memory, and parts larger
 * than that to a temporary file in
 * {@link #setFileStorageDirectory(Path) fileStorageDirectory}.
 * <p>In {@linkplain #setStreaming(boolean) streaming} mode, the contents of the
 * part is streamed directly from the parsed input buffer stream, and not stored
 * in memory nor file.
 *
 * <p>This reader can be provided to {@link MultipartHttpMessageReader} in order
 * to aggregate all parts into a Map.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

  private int maxHeadersSize = 8 * 1024;

  private int maxInMemorySize = 256 * 1024;

  private long maxDiskUsagePerPart = -1;

  private int maxParts = -1;

  private boolean streaming;

  private Charset headersCharset = StandardCharsets.UTF_8;

  private Scheduler blockingOperationScheduler = Schedulers.boundedElastic();

  private FileStorage fileStorage = FileStorage.tempDirectory(this::getBlockingOperationScheduler);

  /**
   * Configure the maximum amount of memory that is allowed per headers section of each part.
   * When the limit
   *
   * @param byteCount the maximum amount of memory for headers
   */
  public void setMaxHeadersSize(int byteCount) {
    this.maxHeadersSize = byteCount;
  }

  /**
   * Get the {@link #setMaxInMemorySize configured} maximum in-memory size.
   */
  public int getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  /**
   * Configure the maximum amount of memory allowed per part.
   * When the limit is exceeded:
   * <ul>
   * <li>file parts are written to a temporary file.
   * <li>non-file parts are rejected with {@link DataBufferLimitException}.
   * </ul>
   * <p>By default this is set to 256K.
   * <p>Note that this property is ignored when
   * {@linkplain #setStreaming(boolean) streaming} is enabled.
   *
   * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
   * contents will be stored in memory
   */
  public void setMaxInMemorySize(int maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }

  /**
   * Configure the maximum amount of disk space allowed for file parts.
   * <p>By default this is set to -1, meaning that there is no maximum.
   * <p>Note that this property is ignored when
   * {@linkplain #setStreaming(boolean) streaming} is enabled, , or when
   * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
   */
  public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
    this.maxDiskUsagePerPart = maxDiskUsagePerPart;
  }

  /**
   * Specify the maximum number of parts allowed in a given multipart request.
   * <p>By default this is set to -1, meaning that there is no maximum.
   */
  public void setMaxParts(int maxParts) {
    this.maxParts = maxParts;
  }

  /**
   * Set the directory used to store parts larger than
   * {@link #setMaxInMemorySize(int) maxInMemorySize}. By default, a directory
   * named {@code multipart} is created under the system
   * temporary directory.
   * <p>Note that this property is ignored when
   * {@linkplain #setStreaming(boolean) streaming} is enabled, or when
   * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
   *
   * @throws IOException if an I/O error occurs, or the parent directory
   * does not exist
   */
  public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
    Assert.notNull(fileStorageDirectory, "FileStorageDirectory is required");
    this.fileStorage = FileStorage.fromPath(fileStorageDirectory);
  }

  /**
   * Set the Reactor {@link Scheduler} to be used for creating files and
   * directories, and writing to files. By default,
   * {@link Schedulers#boundedElastic()} is used, but this property allows for
   * changing it to an externally managed scheduler.
   * <p>Note that this property is ignored when
   * {@linkplain #setStreaming(boolean) streaming} is enabled, or when
   * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
   *
   * @see Schedulers#newBoundedElastic
   */
  public void setBlockingOperationScheduler(Scheduler blockingOperationScheduler) {
    Assert.notNull(blockingOperationScheduler, "FileCreationScheduler is required");
    this.blockingOperationScheduler = blockingOperationScheduler;
  }

  private Scheduler getBlockingOperationScheduler() {
    return this.blockingOperationScheduler;
  }

  /**
   * When set to {@code true}, the {@linkplain Part#content() part content}
   * is streamed directly from the parsed input buffer stream, and not stored
   * in memory nor file.
   * When {@code false}, parts are backed by
   * in-memory and/or file storage. Defaults to {@code false}.
   * <p><strong>NOTE</strong> that with streaming enabled, the
   * {@code Flux<Part>} that is produced by this message reader must be
   * consumed in the original order, i.e. the order of the HTTP message.
   * Additionally, the {@linkplain Part#content() body contents} must either
   * be completely consumed or canceled before moving to the next part.
   * <p>Also note that enabling this property effectively ignores
   * {@link #setMaxInMemorySize(int) maxInMemorySize},
   * {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart},
   * {@link #setFileStorageDirectory(Path) fileStorageDirectory}, and
   * {@link #setBlockingOperationScheduler(Scheduler) fileCreationScheduler}.
   */
  public void setStreaming(boolean streaming) {
    this.streaming = streaming;
  }

  /**
   * Set the character set used to decode headers.
   * Defaults to UTF-8 as per RFC 7578.
   *
   * @param headersCharset the charset to use for decoding headers
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 Section 5.1</a>
   */
  public void setHeadersCharset(Charset headersCharset) {
    Assert.notNull(headersCharset, "HeadersCharset is required");
    this.headersCharset = headersCharset;
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    return Part.class.equals(elementType.toClass())
            && (mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
  }

  @Override
  public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
  }

  @Override
  public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    return Flux.defer(() -> {
      byte[] boundary = MultipartUtils.boundary(message, this.headersCharset);
      if (boundary == null) {
        return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
                message.getHeaders().getContentType() + "\""));
      }

      AtomicInteger partCount = new AtomicInteger();
      return MultipartParser.parse(message.getBody(), boundary, maxHeadersSize, headersCharset)
              .windowUntil(MultipartParser.Token::isLast)
              .concatMap(partsTokens -> {
                if (tooManyParts(partCount)) {
                  return Mono.error(new DecodingException("Too many parts (" + partCount.get() + "/" +
                          this.maxParts + " allowed)"));
                }
                else {
                  return PartGenerator.createPart(partsTokens, this.maxInMemorySize, this.maxDiskUsagePerPart,
                          this.streaming, this.fileStorage.directory(), this.blockingOperationScheduler);
                }
              });
    });
  }

  private boolean tooManyParts(AtomicInteger partCount) {
    int count = partCount.incrementAndGet();
    return this.maxParts > 0 && count > this.maxParts;
  }

}
