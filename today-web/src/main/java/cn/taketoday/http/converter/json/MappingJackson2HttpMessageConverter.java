/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter} that can read and
 * write JSON using <a href="https://github.com/FasterXML/jackson">Jackson 2.x's</a> {@link ObjectMapper}.
 *
 * <p>This converter can be used to bind to typed beans, or untyped {@code HashMap} instances.
 *
 * <p>By default, this converter supports {@code application/json} and {@code application/*+json}
 * with {@code UTF-8} character set. This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12.
 *
 * @author Arjen Poutsma
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MappingJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

  private static final List<MediaType> problemDetailMediaTypes =
          Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);

  @Nullable
  private String jsonPrefix;

  /**
   * Construct a new {@link MappingJackson2HttpMessageConverter} using default configuration
   * provided by {@link Jackson2ObjectMapperBuilder}.
   */
  public MappingJackson2HttpMessageConverter() {
    this(Jackson2ObjectMapperBuilder.json().build());
  }

  /**
   * Construct a new {@link MappingJackson2HttpMessageConverter} with a custom {@link ObjectMapper}.
   * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   *
   * @see Jackson2ObjectMapperBuilder#json()
   */
  public MappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
  }

  /**
   * Specify a custom prefix to use for this view's JSON output.
   * Default is none.
   *
   * @see #setPrefixJson
   */
  public void setJsonPrefix(@Nullable String jsonPrefix) {
    this.jsonPrefix = jsonPrefix;
  }

  /**
   * Indicate whether the JSON output by this view should be prefixed with ")]}', ". Default is false.
   * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
   * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
   * This prefix should be stripped before parsing the string as JSON.
   *
   * @see #setJsonPrefix
   */
  public void setPrefixJson(boolean prefixJson) {
    this.jsonPrefix = (prefixJson ? ")]}', " : null);
  }

  @Override
  protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
    if (this.jsonPrefix != null) {
      generator.writeRaw(this.jsonPrefix);
    }
  }

  @Override
  protected List<MediaType> getMediaTypesForProblemDetail() {
    return problemDetailMediaTypes;
  }

}
