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

package infra.http.codec.smile;

import infra.http.codec.AbstractJacksonDecoder;
import infra.util.MimeType;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

/**
 * Decode a byte stream into Smile and convert to Objects with Jackson 3.x,
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @see JacksonSmileEncoder
 * @since 5.0
 */
public class JacksonSmileDecoder extends AbstractJacksonDecoder<SmileMapper> {

  private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
          new MimeType("application", "x-jackson-smile"),
          new MimeType("application", "*+x-jackson-smile") };

  /**
   * Construct a new instance with a {@link SmileMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileDecoder() {
    super(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileDecoder(SmileMapper.Builder builder) {
    this(builder, DEFAULT_SMILE_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper}.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileDecoder(SmileMapper mapper) {
    this(mapper, DEFAULT_SMILE_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}, and
   * {@link MimeType}s.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileDecoder(SmileMapper.Builder builder, MimeType... mimeTypes) {
    super(builder, mimeTypes);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper} and {@link MimeType}s.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileDecoder(SmileMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
  }

}
