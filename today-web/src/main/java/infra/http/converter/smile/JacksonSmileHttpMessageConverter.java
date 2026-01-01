/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.converter.smile;

import infra.http.MediaType;
import infra.http.converter.AbstractJacksonHttpMessageConverter;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

/**
 * Implementation of {@link infra.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write Smile data format ("binary JSON") using
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/3.x/smile">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports {@code "application/x-jackson-smile"}
 * media type. This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The following hints entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>"com.fasterxml.jackson.annotation.JsonView"</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>"tools.jackson.databind.ser.FilterProvider"</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class JacksonSmileHttpMessageConverter extends AbstractJacksonHttpMessageConverter<SmileMapper> {

  private static final MediaType DEFAULT_SMILE_MIME_TYPES = new MediaType("application", "x-jackson-smile");

  /**
   * Construct a new instance with a {@link SmileMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   */
  public JacksonSmileHttpMessageConverter() {
    super(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper} customized
   * with the {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileHttpMessageConverter(SmileMapper.Builder builder) {
    super(builder, DEFAULT_SMILE_MIME_TYPES);
  }

  /**
   * Construct a new instance with the provided {@link SmileMapper}.
   *
   * @see SmileMapper#builder()
   */
  public JacksonSmileHttpMessageConverter(SmileMapper mapper) {
    super(mapper, DEFAULT_SMILE_MIME_TYPES);
  }

}
