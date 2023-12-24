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

import java.nio.charset.Charset;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CharSequenceEncoder
 * @since 4.0
 */
public final class StringDecoder extends AbstractCharSequenceDecoder<String> {

  private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
    super(delimiters, stripDelimiter, mimeTypes);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return elementType.resolve() == String.class && super.canDecode(elementType, mimeType);
  }

  @Override
  protected String decodeInternal(DataBuffer dataBuffer, Charset charset) {
    return dataBuffer.toString(charset);
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
    return new StringDecoder(delimiters, stripDelimiter, MimeTypeUtils.TEXT_PLAIN.withCharset(Constant.DEFAULT_CHARSET));
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
    return new StringDecoder(delimiters, stripDelimiter,
            MimeTypeUtils.TEXT_PLAIN.withCharset(Constant.DEFAULT_CHARSET), MimeTypeUtils.ALL);
  }

}
