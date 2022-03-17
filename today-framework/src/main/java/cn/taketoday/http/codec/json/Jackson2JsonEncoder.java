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

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using Jackson 2.9.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see Jackson2JsonDecoder
 * @since 4.0
 */
public class Jackson2JsonEncoder extends AbstractJackson2Encoder {

  @Nullable
  private final PrettyPrinter ssePrettyPrinter;

  public Jackson2JsonEncoder() {
    this(Jackson2ObjectMapperBuilder.json().build());
  }

  public Jackson2JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
    setStreamingMediaTypes(Arrays.asList(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_STREAM_JSON));
    this.ssePrettyPrinter = initSsePrettyPrinter();
  }

  private static PrettyPrinter initSsePrettyPrinter() {
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
    return printer;
  }

  @Override
  protected ObjectWriter customizeWriter(
          ObjectWriter writer, @Nullable MimeType mimeType,
          ResolvableType elementType, @Nullable Map<String, Object> hints) {

    return this.ssePrettyPrinter != null
                   && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType)
                   && writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT)
           ? writer.with(this.ssePrettyPrinter)
           : writer;
  }

}
