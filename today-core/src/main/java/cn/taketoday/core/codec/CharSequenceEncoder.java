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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.codec;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.CoderMalfunctionError;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Encode from a {@code CharSequence} stream to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StringDecoder
 * @since 4.0
 */
public final class CharSequenceEncoder extends AbstractEncoder<CharSequence> {

  private final ConcurrentMap<Charset, Float> charsetToMaxBytesPerChar = new ConcurrentHashMap<>(3);

  private CharSequenceEncoder(MimeType... mimeTypes) {
    super(mimeTypes);
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    Class<?> clazz = elementType.toClass();
    return super.canEncode(elementType, mimeType) && CharSequence.class.isAssignableFrom(clazz);
  }

  @Override
  public Flux<DataBuffer> encode(
          Publisher<? extends CharSequence> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream)
            .map(charSequence -> encodeValue(charSequence, bufferFactory, elementType, mimeType, hints));
  }

  @Override
  public DataBuffer encodeValue(
          CharSequence charSequence, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (!Hints.isLoggingSuppressed(hints)) {
      LogFormatUtils.traceDebug(logger, traceOn -> {
        String formatted = LogFormatUtils.formatValue(charSequence, !traceOn);
        return Hints.getLogPrefix(hints) + "Writing " + formatted;
      });
    }
    boolean release = true;
    Charset charset = getCharset(mimeType);
    int capacity = calculateCapacity(charSequence, charset);
    DataBuffer dataBuffer = bufferFactory.allocateBuffer(capacity);
    try {
      dataBuffer.write(charSequence, charset);
      release = false;
    }
    catch (CoderMalfunctionError ex) {
      throw new EncodingException("String encoding error: " + ex.getMessage(), ex);
    }
    finally {
      if (release) {
        DataBufferUtils.release(dataBuffer);
      }
    }
    return dataBuffer;
  }

  int calculateCapacity(CharSequence sequence, Charset charset) {
    float maxBytesPerChar = this.charsetToMaxBytesPerChar.computeIfAbsent(
            charset, cs -> cs.newEncoder().maxBytesPerChar());
    float maxBytesForSequence = sequence.length() * maxBytesPerChar;
    return (int) Math.ceil(maxBytesForSequence);
  }

  private Charset getCharset(@Nullable MimeType mimeType) {
    if (mimeType != null && mimeType.getCharset() != null) {
      return mimeType.getCharset();
    }
    else {
      return Constant.DEFAULT_CHARSET;
    }
  }

  /**
   * Create a {@code CharSequenceEncoder} that supports only "text/plain".
   */
  public static CharSequenceEncoder textPlainOnly() {
    return new CharSequenceEncoder(MimeTypeUtils.TEXT_PLAIN.withCharset(Constant.DEFAULT_CHARSET));
  }

  /**
   * Create a {@code CharSequenceEncoder} that supports all MIME types.
   */
  public static CharSequenceEncoder allMimeTypes() {
    return new CharSequenceEncoder(MimeTypeUtils.TEXT_PLAIN.withCharset(Constant.DEFAULT_CHARSET), MimeTypeUtils.ALL);
  }

}
