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

package infra.http.converter;

import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import infra.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import infra.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import infra.util.ClassUtils;

/**
 * Extension of {@link infra.http.converter.FormHttpMessageConverter},
 * adding support for XML and JSON-based parts.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

  private static final boolean jackson2Present;

  private static final boolean jackson2SmilePresent;

  private static final boolean gsonPresent;

  private static final boolean jsonbPresent;

  private static final boolean jackson2XmlPresent;

  private static final boolean jackson2YamlPresent;

  static {
    ClassLoader classLoader = AllEncompassingFormHttpMessageConverter.class.getClassLoader();
    jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
            ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
    jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
    gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
    jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
    jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
    jackson2YamlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
  }

  public AllEncompassingFormHttpMessageConverter() {

    if (jackson2Present) {
      addPartConverter(new MappingJackson2HttpMessageConverter());
    }
    else if (gsonPresent) {
      addPartConverter(new GsonHttpMessageConverter());
    }
    else if (jsonbPresent) {
      addPartConverter(new JsonbHttpMessageConverter());
    }

    if (jackson2XmlPresent) {
      addPartConverter(new MappingJackson2XmlHttpMessageConverter());
    }

    if (jackson2SmilePresent) {
      addPartConverter(new MappingJackson2SmileHttpMessageConverter());
    }

    if (jackson2YamlPresent) {
      addPartConverter(new MappingJackson2YamlHttpMessageConverter());
    }
  }

}
