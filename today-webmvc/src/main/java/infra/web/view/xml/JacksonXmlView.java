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

package infra.web.view.xml;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.lang.Assert;
import infra.validation.BindingResult;
import infra.web.RequestContext;
import infra.web.view.AbstractJacksonView;
import infra.web.view.View;
import infra.web.view.json.JacksonJsonView;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Infra MVC {@link View} that renders XML content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 3's</a> {@link XmlMapper}.
 *
 * <p>The Object to be serialized is supplied as a parameter in the model. The first serializable
 * entry is used. Users can specify a specific entry in the model via the
 * {@link #setModelKey(String) sourceKey} property.
 *
 * <p>The following special model entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>com.fasterxml.jackson.annotation.JsonView</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>tools.jackson.databind.ser.FilterProvider</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see JacksonJsonView
 * @since 5.0
 */
public class JacksonXmlView extends AbstractJacksonView {

  /**
   * Default content type: {@value}.
   * <p>Overridable through {@link #setContentType(String)}.
   */
  public static final String DEFAULT_CONTENT_TYPE = "application/xml";

  private @Nullable String modelKey;

  /**
   * Construct a new instance with an {@link XmlMapper} customized with
   * the {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)} and setting
   * the content type to {@code application/xml}.
   */
  public JacksonXmlView() {
    super(XmlMapper.builder(), DEFAULT_CONTENT_TYPE);
  }

  /**
   * Construct a new instance using the provided {@link XmlMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)} and setting
   * the content type to {@code application/xml}.
   *
   * @see XmlMapper#builder()
   */
  public JacksonXmlView(XmlMapper.Builder builder) {
    super(builder, DEFAULT_CONTENT_TYPE);
  }

  /**
   * Construct a new instance using the provided {@link XmlMapper}
   * and setting the content type to {@code application/xml}.
   *
   * @see XmlMapper#builder()
   */
  public JacksonXmlView(XmlMapper mapper) {
    super(mapper, DEFAULT_CONTENT_TYPE);
  }

  @Override
  public void setModelKey(String modelKey) {
    this.modelKey = modelKey;
  }

  @Override
  protected Object filterModel(Map<String, Object> model, RequestContext request) {
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
        if (!(entry.getValue() instanceof BindingResult) &&
                !entry.getKey().equals(JSON_VIEW_HINT) &&
                !entry.getKey().equals(FILTER_PROVIDER_HINT)) {
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
