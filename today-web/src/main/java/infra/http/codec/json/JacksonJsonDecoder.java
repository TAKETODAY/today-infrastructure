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
import org.reactivestreams.Publisher;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.CharBufferDecoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.MediaType;
import infra.http.codec.AbstractJacksonDecoder;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/FasterXML/jackson">Jackson 3.x</a>
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see JacksonJsonEncoder
 * @since 5.0
 */
public class JacksonJsonDecoder extends AbstractJacksonDecoder<JsonMapper> {

  private static final CharBufferDecoder CHAR_BUFFER_DECODER = CharBufferDecoder.textPlainOnly(Arrays.asList(",", "\n"), false);

  private static final ResolvableType CHAR_BUFFER_TYPE = ResolvableType.forClass(CharBuffer.class);

  private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
          MediaType.APPLICATION_JSON,
          new MediaType("application", "*+json"),
          MediaType.APPLICATION_NDJSON
  };

  /**
   * Construct a new instance with a {@link JsonMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   */
  public JacksonJsonDecoder() {
    super(JsonMapper.builder(), DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonDecoder(JsonMapper.Builder builder) {
    super(builder, DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper}.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonDecoder(JsonMapper mapper) {
    super(mapper, DEFAULT_JSON_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}, and
   * {@link MimeType}s.
   *
   * @see JsonMapper#builder()
   */
  public JacksonJsonDecoder(JsonMapper.Builder builder, MimeType... mimeTypes) {
    super(builder, mimeTypes);
  }

  /**
   * Construct a new instance with the provided {@link JsonMapper} and {@link MimeType}s.
   */
  public JacksonJsonDecoder(JsonMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return super.canDecode(elementType, mimeType) && !CharSequence.class.isAssignableFrom(elementType.toClass());
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

    // Re-encode as UTF-8.
    MimeType textMimeType = new MimeType(MimeType.TEXT_PLAIN, charset);
    Flux<CharBuffer> decoded = CHAR_BUFFER_DECODER.decode(input, CHAR_BUFFER_TYPE, textMimeType, null);
    return decoded.map(charBuffer -> DefaultDataBufferFactory.sharedInstance.wrap(StandardCharsets.UTF_8.encode(charBuffer)));
  }

}
