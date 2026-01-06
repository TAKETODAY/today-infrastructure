/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.codec.AbstractJacksonEncoder;
import infra.http.converter.json.ProblemDetailJacksonMixin;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/FasterXML/jackson">Jackson 3.x</a>. For non-streaming
 * use cases, {@link Flux} elements are collected into a {@link List} before
 * serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see JacksonJsonDecoder
 * @since 5.0
 */
public class JacksonJsonEncoder extends AbstractJacksonEncoder<JsonMapper> {

  private static final List<MimeType> problemDetailMimeTypes =
          Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);

  private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
          MediaType.APPLICATION_JSON,
          new MediaType("application", "*+json"),
          MediaType.APPLICATION_NDJSON
  };

  private final @Nullable PrettyPrinter ssePrettyPrinter;

  /**
   * Construct a new instance with a {@link JsonMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)} and
   * {@link ProblemDetailJacksonMixin}.
   */
  public JacksonJsonEncoder() {
    this(JsonMapper.builder(), DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with a {@link JsonMapper.Builder} customized
   * with the {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)} and
   * {@link ProblemDetailJacksonMixin}.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonEncoder(JsonMapper.Builder builder) {
    this(builder, DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper}.
   */
  public JacksonJsonEncoder(JsonMapper mapper) {
    this(mapper, DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper.Builder} customized
   * with the {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)} and
   * {@link ProblemDetailJacksonMixin}, and {@link MimeType}s.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonEncoder(JsonMapper.Builder builder, MimeType... mimeTypes) {
    super(builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class), mimeTypes);
    setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
    this.ssePrettyPrinter = initSsePrettyPrinter();
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper} and
   * {@link MimeType}s.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonEncoder(JsonMapper mapper, MimeType... mimeTypes) {
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
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return super.canEncode(elementType, mimeType) && !String.class.isAssignableFrom(elementType.toClass());
  }

  @Override
  protected List<MimeType> getMediaTypesForProblemDetail() {
    return problemDetailMimeTypes;
  }

  @Override
  protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
          ResolvableType elementType, @Nullable Map<String, Object> hints) {

    return (this.ssePrettyPrinter != null &&
            MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType) &&
            writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT) ?
            writer.with(this.ssePrettyPrinter) : writer);
  }

}
