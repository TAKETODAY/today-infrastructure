/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.view.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.view.View;

/**
 * Framework MVC {@link View} that renders JSON content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 2's</a> {@link ObjectMapper}.
 *
 * <p>By default, the entire contents of the model map (with the exception of framework-specific classes)
 * will be encoded as JSON. If the model contains only one key, you can have it extracted encoded as JSON
 * alone via  {@link #setExtractValueFromSingleKeyModel}.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJackson2JsonView extends AbstractJackson2View {

  /**
   * Default content type: "application/json".
   * Overridable through {@link #setContentType}.
   */
  public static final String DEFAULT_CONTENT_TYPE = "application/json";

  @Nullable
  private String jsonPrefix;

  @Nullable
  private Set<String> modelKeys;

  private boolean extractValueFromSingleKeyModel = false;

  /**
   * Construct a new {@code MappingJackson2JsonView} using default configuration
   * provided by {@link Jackson2ObjectMapperBuilder} and setting the content type
   * to {@code application/json}.
   */
  public MappingJackson2JsonView() {
    super(Jackson2ObjectMapperBuilder.json().build(), DEFAULT_CONTENT_TYPE);
  }

  /**
   * Construct a new {@code MappingJackson2JsonView} using the provided
   * {@link ObjectMapper} and setting the content type to {@code application/json}.
   */
  public MappingJackson2JsonView(ObjectMapper objectMapper) {
    super(objectMapper, DEFAULT_CONTENT_TYPE);
  }

  /**
   * Specify a custom prefix to use for this view's JSON output.
   * Default is none.
   *
   * @see #setPrefixJson
   */
  public void setJsonPrefix(String jsonPrefix) {
    this.jsonPrefix = jsonPrefix;
  }

  /**
   * Indicates whether the JSON output by this view should be prefixed with <tt>")]}', "</tt>.
   * Default is {@code false}.
   * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
   * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
   * This prefix should be stripped before parsing the string as JSON.
   *
   * @see #setJsonPrefix
   */
  public void setPrefixJson(boolean prefixJson) {
    this.jsonPrefix = (prefixJson ? ")]}', " : null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setModelKey(String modelKey) {
    this.modelKeys = Collections.singleton(modelKey);
  }

  /**
   * Set the attributes in the model that should be rendered by this view.
   * When set, all other model attributes will be ignored.
   */
  public void setModelKeys(@Nullable Set<String> modelKeys) {
    this.modelKeys = modelKeys;
  }

  /**
   * Return the attributes in the model that should be rendered by this view.
   */
  @Nullable
  public final Set<String> getModelKeys() {
    return this.modelKeys;
  }

  /**
   * Set whether to serialize models containing a single attribute as a map or
   * whether to extract the single value from the model and serialize it directly.
   * <p>The effect of setting this flag is similar to using
   * {@code MappingJackson2HttpMessageConverter} with an {@code @ResponseBody}
   * request-handling method.
   * <p>Default is {@code false}.
   */
  public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
    this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
  }

  /**
   * Filter out undesired attributes from the given model.
   * The return value can be either another {@link Map} or a single value object.
   *
   * @param model the model, as passed on to {@link #renderMergedOutputModel}
   * @return the value to be rendered
   */
  @Override
  protected Object filterModel(Map<String, Object> model) {
    Map<String, Object> result = CollectionUtils.newHashMap(model.size());
    Set<String> modelKeys = CollectionUtils.isNotEmpty(this.modelKeys) ? this.modelKeys : model.keySet();

    for (Map.Entry<String, Object> entry : model.entrySet()) {
      String clazz = entry.getKey();
      if (modelKeys.contains(clazz)
              && !clazz.equals(JsonView.class.getName())
              && !clazz.equals(FilterProvider.class.getName())) {
        result.put(clazz, entry.getValue());
      }
    }
    return extractValueFromSingleKeyModel && result.size() == 1
           ? result.values().iterator().next()
           : result;
  }

  @Override
  protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
    if (this.jsonPrefix != null) {
      generator.writeRaw(this.jsonPrefix);
    }
  }

}
