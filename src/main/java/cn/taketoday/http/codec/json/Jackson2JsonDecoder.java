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

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Decode a byte stream into JSON and convert to Object's with Jackson 2.9,
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see Jackson2JsonEncoder
 * @since 4.0
 */
public class Jackson2JsonDecoder extends AbstractJackson2Decoder {

  private static final StringDecoder STRING_DECODER = StringDecoder.textPlainOnly(Arrays.asList(",", "\n"), false);

  private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);

  public Jackson2JsonDecoder() {
    super(Jackson2ObjectMapperBuilder.json().build());
  }

  public Jackson2JsonDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
  }

  @Override
  protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
                                          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    Flux<DataBuffer> flux = Flux.from(input);
    if (mimeType == null) {
      return flux;
    }

    // Jackson asynchronous parser only supports UTF-8
    Charset charset = mimeType.getCharset();
    if (charset == null || StandardCharsets.UTF_8.equals(charset) || StandardCharsets.US_ASCII.equals(charset)) {
      return flux;
    }

    // Potentially, the memory consumption of this conversion could be improved by using CharBuffers instead
    // of allocating Strings, but that would require refactoring the buffer tokenization code from StringDecoder

    MimeType textMimeType = new MimeType(MimeTypeUtils.TEXT_PLAIN, charset);
    Flux<String> decoded = STRING_DECODER.decode(input, STRING_TYPE, textMimeType, null);
    return decoded.map(s -> DefaultDataBufferFactory.sharedInstance.wrap(s.getBytes(StandardCharsets.UTF_8)));
  }

}
