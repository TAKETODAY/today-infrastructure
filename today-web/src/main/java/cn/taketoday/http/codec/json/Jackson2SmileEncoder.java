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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import java.util.Collections;
import java.util.List;

import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Encode from an {@code Object} stream to a byte stream of Smile objects using Jackson 2.9.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @see Jackson2SmileDecoder
 * @since 4.0
 */
public class Jackson2SmileEncoder extends AbstractJackson2Encoder {

  static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
          new MimeType("application", "x-jackson-smile"),
          new MimeType("application", "*+x-jackson-smile")
  };

  private static final byte[] STREAM_SEPARATOR = Constant.EMPTY_BYTES;

  public Jackson2SmileEncoder() {
    this(Jackson2ObjectMapperBuilder.smile().build(), DEFAULT_SMILE_MIME_TYPES);
  }

  public Jackson2SmileEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
    Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
    setStreamingMediaTypes(Collections.singletonList(new MediaType("application", "stream+x-jackson-smile")));
  }

  /**
   * Return the separator to use for the given mime type.
   * <p>By default, this method returns a single byte 0 if the given
   * mime type is one of the configured {@link #setStreamingMediaTypes(List)
   * streaming} mime types.
   */
  @Nullable
  @Override
  protected byte[] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
    for (MediaType streamingMediaType : getStreamingMediaTypes()) {
      if (streamingMediaType.isCompatibleWith(mimeType)) {
        return STREAM_SEPARATOR;
      }
    }
    return null;
  }
}
