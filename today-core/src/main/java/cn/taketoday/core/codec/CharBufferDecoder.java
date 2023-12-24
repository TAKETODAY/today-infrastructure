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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;

/**
 * Decode from a data buffer stream to a {@code CharBuffer} stream, either splitting
 * or aggregating incoming data chunks to realign along newlines delimiters
 * and produce a stream of char buffers. This is useful for streaming but is also
 * necessary to ensure that multi-byte characters can be decoded correctly,
 * avoiding split-character issues. The default delimiters used by default are
 * {@code \n} and {@code \r\n} but that can be customized.
 *
 * @author Markus Heiden
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CharSequenceEncoder
 * @since 4.0
 */
public final class CharBufferDecoder extends AbstractCharSequenceDecoder<CharBuffer> {

  public CharBufferDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
    super(delimiters, stripDelimiter, mimeTypes);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return elementType.resolve() == CharBuffer.class && super.canDecode(elementType, mimeType);
  }

  @Override
  protected CharBuffer decodeInternal(DataBuffer dataBuffer, Charset charset) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
    dataBuffer.toByteBuffer(byteBuffer);
    return charset.decode(byteBuffer);
  }

  /**
   * Create a {@code CharBufferDecoder} for {@code "text/plain"}.
   */
  public static CharBufferDecoder textPlainOnly() {
    return textPlainOnly(DEFAULT_DELIMITERS, true);
  }

  /**
   * Create a {@code CharBufferDecoder} for {@code "text/plain"}.
   *
   * @param delimiters delimiter strings to use to split the input stream
   * @param stripDelimiter whether to remove delimiters from the resulting
   * input strings
   */
  public static CharBufferDecoder textPlainOnly(List<String> delimiters, boolean stripDelimiter) {
    return new CharBufferDecoder(delimiters, stripDelimiter, MimeTypeUtils.TEXT_PLAIN_UTF8);
  }

  /**
   * Create a {@code CharBufferDecoder} that supports all MIME types.
   */
  public static CharBufferDecoder allMimeTypes() {
    return allMimeTypes(DEFAULT_DELIMITERS, true);
  }

  /**
   * Create a {@code CharBufferDecoder} that supports all MIME types.
   *
   * @param delimiters delimiter strings to use to split the input stream
   * @param stripDelimiter whether to remove delimiters from the resulting
   * input strings
   */
  public static CharBufferDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
    return new CharBufferDecoder(delimiters, stripDelimiter, MimeTypeUtils.TEXT_PLAIN_UTF8, MimeTypeUtils.ALL);
  }

}
