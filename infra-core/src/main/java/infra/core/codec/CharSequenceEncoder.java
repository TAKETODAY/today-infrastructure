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

package infra.core.codec;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.CoderMalfunctionError;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.lang.Constant;
import infra.util.LogFormatUtils;
import infra.util.MimeType;
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
        dataBuffer.release();
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
    return new CharSequenceEncoder(MimeType.TEXT_PLAIN_UTF8);
  }

  /**
   * Create a {@code CharSequenceEncoder} that supports all MIME types.
   */
  public static CharSequenceEncoder allMimeTypes() {
    return new CharSequenceEncoder(MimeType.TEXT_PLAIN_UTF8, MimeType.ALL);
  }

}
