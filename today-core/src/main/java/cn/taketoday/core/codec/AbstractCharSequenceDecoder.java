/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.codec;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.LimitedDataBufferList;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstract base class that decodes from a data buffer stream to a
 * {@code CharSequence} stream.
 *
 * @param <T> the character sequence type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractCharSequenceDecoder<T extends CharSequence> extends AbstractDataBufferDecoder<T> {

  /** The default charset to use, i.e. "UTF-8". */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /** The default delimiter strings to use, i.e. {@code \r\n} and {@code \n}. */
  public static final List<String> DEFAULT_DELIMITERS = List.of("\r\n", "\n");

  private final ArrayList<String> delimiters;

  private final boolean stripDelimiter;

  private Charset defaultCharset = DEFAULT_CHARSET;

  private final ConcurrentHashMap<Charset, byte[][]> delimitersCache = new ConcurrentHashMap<>();

  /**
   * Create a new {@code AbstractCharSequenceDecoder} with the given parameters.
   */
  protected AbstractCharSequenceDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
    super(mimeTypes);
    Assert.notEmpty(delimiters, "'delimiters' must not be empty");
    this.delimiters = new ArrayList<>(delimiters);
    this.stripDelimiter = stripDelimiter;
  }

  /**
   * Set the default character set to fall back on if the MimeType does not specify any.
   * <p>By default this is {@code UTF-8}.
   *
   * @param defaultCharset the charset to fall back on
   */
  public void setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  /**
   * Return the configured {@link #setDefaultCharset(Charset) defaultCharset}.
   */
  public Charset getDefaultCharset() {
    return this.defaultCharset;
  }

  @Override
  public final Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    byte[][] delimiterBytes = getDelimiterBytes(mimeType);

    LimitedDataBufferList chunks = new LimitedDataBufferList(getMaxInMemorySize());
    DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delimiterBytes);

    return Flux.from(input)
            .concatMapIterable(buffer -> processDataBuffer(buffer, matcher, chunks))
            .concatWith(Mono.defer(() -> {
              if (chunks.isEmpty()) {
                return Mono.empty();
              }
              DataBuffer lastBuffer = chunks.get(0).factory().join(chunks);
              chunks.clear();
              return Mono.just(lastBuffer);
            }))
            .doFinally(signalType -> chunks.releaseAndClear())
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
            .map(buffer -> decode(buffer, elementType, mimeType, hints));
  }

  private byte[][] getDelimiterBytes(@Nullable MimeType mimeType) {
    return this.delimitersCache.computeIfAbsent(getCharset(mimeType), charset -> {
      byte[][] result = new byte[this.delimiters.size()][];
      for (int i = 0; i < this.delimiters.size(); i++) {
        result[i] = this.delimiters.get(i).getBytes(charset);
      }
      return result;
    });
  }

  private Collection<DataBuffer> processDataBuffer(DataBuffer buffer, DataBufferUtils.Matcher matcher,
          LimitedDataBufferList chunks) {

    boolean release = true;
    try {
      List<DataBuffer> result = null;
      do {
        int endIndex = matcher.match(buffer);
        if (endIndex == -1) {
          chunks.add(buffer);
          release = false;
          break;
        }
        DataBuffer split = buffer.split(endIndex + 1);
        if (result == null) {
          result = new ArrayList<>();
        }
        int delimiterLength = matcher.delimiter().length;
        if (chunks.isEmpty()) {
          if (this.stripDelimiter) {
            split.writePosition(split.writePosition() - delimiterLength);
          }
          result.add(split);
        }
        else {
          chunks.add(split);
          DataBuffer joined = buffer.factory().join(chunks);
          if (this.stripDelimiter) {
            joined.writePosition(joined.writePosition() - delimiterLength);
          }
          result.add(joined);
          chunks.clear();
        }
      }
      while (buffer.readableByteCount() > 0);
      return (result != null ? result : Collections.emptyList());
    }
    finally {
      if (release) {
        DataBufferUtils.release(buffer);
      }
    }
  }

  @Override
  public final T decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Charset charset = getCharset(mimeType);
    T value = decodeInternal(dataBuffer, charset);
    DataBufferUtils.release(dataBuffer);
    LogFormatUtils.traceDebug(logger, traceOn -> {
      String formatted = LogFormatUtils.formatValue(value, !traceOn);
      return Hints.getLogPrefix(hints) + "Decoded " + formatted;
    });
    return value;
  }

  private Charset getCharset(@Nullable MimeType mimeType) {
    if (mimeType != null) {
      Charset charset = mimeType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    return getDefaultCharset();
  }

  /**
   * Template method that decodes the given data buffer into {@code T}, given
   * the charset.
   */
  protected abstract T decodeInternal(DataBuffer dataBuffer, Charset charset);

}
