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

package infra.http.codec.json;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.http.MediaType;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.lang.Nullable;
import infra.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using Jackson 2.9.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Jackson2JsonDecoder
 * @since 4.0
 */
public class Jackson2JsonEncoder extends AbstractJackson2Encoder {

  private static final List<MimeType> problemDetailMimeTypes =
          Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);

  @Nullable
  private final PrettyPrinter ssePrettyPrinter;

  public Jackson2JsonEncoder() {
    this(Jackson2ObjectMapperBuilder.json().build());
  }

  public Jackson2JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
    setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
    this.ssePrettyPrinter = initSsePrettyPrinter();
  }

  private static PrettyPrinter initSsePrettyPrinter() {
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    printer.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
    return printer;
  }

  @Override
  protected ObjectWriter customizeWriter(ObjectWriter writer,
          @Nullable MimeType mimeType, ResolvableType elementType, @Nullable Map<String, Object> hints) {

    return this.ssePrettyPrinter != null
            && MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType)
            && writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT)
            ? writer.with(this.ssePrettyPrinter)
            : writer;
  }

  @Override
  protected List<MimeType> getMediaTypesForProblemDetail() {
    return problemDetailMimeTypes;
  }

}
