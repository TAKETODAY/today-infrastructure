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

package cn.taketoday.core.codec;

import org.reactivestreams.Publisher;

import java.nio.CharBuffer;
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
import cn.taketoday.core.io.buffer.PooledDataBuffer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decode from a data buffer stream to a {@code String} stream, either splitting
 * or aggregating incoming data chunks to realign along newlines delimiters
 * and produce a stream of strings. This is useful for streaming but is also
 * necessary to ensure that that multibyte characters can be decoded correctly,
 * avoiding split-character issues. The default delimiters used by default are
 * {@code \n} and {@code \r\n} but that can be customized.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author Mark Paluch
 * @see CharSequenceEncoder
 * @since 4.0
 */
public final class StringDecoder extends AbstractDataBufferDecoder<String> {

  /** The default charset to use, i.e. "UTF-8". */
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /** The default delimiter strings to use, i.e. {@code \r\n} and {@code \n}. */
  public static final List<String> DEFAULT_DELIMITERS = List.of("\r\n", "\n");

  private final ArrayList<String> delimiters;

  private final boolean stripDelimiter;

  private Charset defaultCharset = DEFAULT_CHARSET;

  private final ConcurrentHashMap<Charset, byte[][]> delimitersCache = new ConcurrentHashMap<>();

  private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
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
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return elementType.resolve() == String.class && super.canDecode(elementType, mimeType);
  }

  @Override
  public Flux<String> decode(
          Publisher<DataBuffer> input, ResolvableType elementType,
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
            .doOnTerminate(chunks::releaseAndClear)
            .doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release)
            .mapNotNull(buffer -> decode(buffer, elementType, mimeType, hints));
  }

  private byte[][] getDelimiterBytes(@Nullable MimeType mimeType) {
    return this.delimitersCache.computeIfAbsent(getCharset(mimeType), charset -> {
      byte[][] result = new byte[delimiters.size()][];
      for (int i = 0; i < delimiters.size(); i++) {
        result[i] = delimiters.get(i).getBytes(charset);
      }
      return result;
    });
  }

  private Collection<DataBuffer> processDataBuffer(
          DataBuffer buffer, DataBufferUtils.Matcher matcher, LimitedDataBufferList chunks) {

    try {
      List<DataBuffer> result = null;
      do {
        int endIndex = matcher.match(buffer);
        if (endIndex == -1) {
          chunks.add(buffer);
          DataBufferUtils.retain(buffer); // retain after add (may raise DataBufferLimitException)
          break;
        }
        int startIndex = buffer.readPosition();
        int length = (endIndex - startIndex + 1);
        DataBuffer slice = buffer.retainedSlice(startIndex, length);
        result = (result != null ? result : new ArrayList<>());
        if (chunks.isEmpty()) {
          if (this.stripDelimiter) {
            slice.writePosition(slice.writePosition() - matcher.delimiter().length);
          }
          result.add(slice);
        }
        else {
          chunks.add(slice);
          DataBuffer joined = buffer.factory().join(chunks);
          if (this.stripDelimiter) {
            joined.writePosition(joined.writePosition() - matcher.delimiter().length);
          }
          result.add(joined);
          chunks.clear();
        }
        buffer.readPosition(endIndex + 1);
      }
      while (buffer.readableByteCount() > 0);
      return (result != null ? result : Collections.emptyList());
    }
    finally {
      DataBufferUtils.release(buffer);
    }
  }

  @Override
  public String decode(DataBuffer dataBuffer, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Charset charset = getCharset(mimeType);
    CharBuffer charBuffer = charset.decode(dataBuffer.asByteBuffer());
    DataBufferUtils.release(dataBuffer);
    String value = charBuffer.toString();
    LogFormatUtils.traceDebug(logger, traceOn -> {
      String formatted = LogFormatUtils.formatValue(value, !traceOn);
      return Hints.getLogPrefix(hints) + "Decoded " + formatted;
    });
    return value;
  }

  private Charset getCharset(@Nullable MimeType mimeType) {
    if (mimeType != null && mimeType.getCharset() != null) {
      return mimeType.getCharset();
    }
    else {
      return getDefaultCharset();
    }
  }

  /**
   * Create a {@code StringDecoder} for {@code "text/plain"}.
   */
  public static StringDecoder textPlainOnly() {
    return textPlainOnly(DEFAULT_DELIMITERS, true);
  }

  /**
   * Create a {@code StringDecoder} for {@code "text/plain"}.
   *
   * @param delimiters delimiter strings to use to split the input stream
   * @param stripDelimiter whether to remove delimiters from the resulting
   * input strings
   */
  public static StringDecoder textPlainOnly(List<String> delimiters, boolean stripDelimiter) {
    return new StringDecoder(delimiters, stripDelimiter, new MimeType("text", "plain", DEFAULT_CHARSET));
  }

  /**
   * Create a {@code StringDecoder} that supports all MIME types.
   */
  public static StringDecoder allMimeTypes() {
    return allMimeTypes(DEFAULT_DELIMITERS, true);
  }

  /**
   * Create a {@code StringDecoder} that supports all MIME types.
   *
   * @param delimiters delimiter strings to use to split the input stream
   * @param stripDelimiter whether to remove delimiters from the resulting
   * input strings
   */
  public static StringDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
    return new StringDecoder(
            delimiters, stripDelimiter, new MimeType("text", "plain", DEFAULT_CHARSET), MimeTypeUtils.ALL);
  }

}
