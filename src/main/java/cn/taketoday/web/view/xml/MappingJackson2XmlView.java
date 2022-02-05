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

package cn.taketoday.web.view.xml;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.Map;

import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.json.AbstractJackson2View;

/**
 * Framework MVC {@link View} that renders XML content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 2's</a> {@link XmlMapper}.
 *
 * <p>The Object to be serialized is supplied as a parameter in the model. The first serializable
 * entry is used. Users can either specify a specific entry in the model via the
 * {@link #setModelKey(String) sourceKey} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12.
 *
 * @author Sebastien Deleuze
 * @see cn.taketoday.web.view.json.MappingJackson2JsonView
 */
public class MappingJackson2XmlView extends AbstractJackson2View {

  /**
   * The default content type for the view.
   */
  public static final String DEFAULT_CONTENT_TYPE = "application/xml";

  @Nullable
  private String modelKey;

  /**
   * Construct a new {@code MappingJackson2XmlView} using default configuration
   * provided by {@link Jackson2ObjectMapperBuilder} and setting the content type
   * to {@code application/xml}.
   */
  public MappingJackson2XmlView() {
    super(Jackson2ObjectMapperBuilder.xml().build(), DEFAULT_CONTENT_TYPE);
  }

  /**
   * Construct a new {@code MappingJackson2XmlView} using the provided {@link XmlMapper}
   * and setting the content type to {@code application/xml}.
   */
  public MappingJackson2XmlView(XmlMapper xmlMapper) {
    super(xmlMapper, DEFAULT_CONTENT_TYPE);
  }

  @Override
  public void setModelKey(String modelKey) {
    this.modelKey = modelKey;
  }

  @Override
  protected Object filterModel(Map<String, Object> model) {
    Object value = null;
    if (this.modelKey != null) {
      value = model.get(this.modelKey);
      if (value == null) {
        throw new IllegalStateException(
                "Model contains no object with key [" + this.modelKey + "]");
      }
    }
    else {
      for (Map.Entry<String, Object> entry : model.entrySet()) {
        if (!entry.getKey().equals(JsonView.class.getName())) {
          if (value != null) {
            throw new IllegalStateException("Model contains more than one object to render, only one is supported");
          }
          value = entry.getValue();
        }
      }
    }
    Assert.state(value != null, "Model contains no object to render");
    return value;
  }

}
