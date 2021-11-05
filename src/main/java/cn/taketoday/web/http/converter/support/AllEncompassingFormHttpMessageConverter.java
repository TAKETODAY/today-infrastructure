/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.http.converter.support;

import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.http.converter.FormHttpMessageConverter;
import cn.taketoday.web.http.converter.json.GsonHttpMessageConverter;
import cn.taketoday.web.http.converter.json.JsonbHttpMessageConverter;
import cn.taketoday.web.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import cn.taketoday.web.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.web.http.converter.smile.MappingJackson2SmileHttpMessageConverter;

/**
 * Extension of {@link cn.taketoday.web.http.converter.FormHttpMessageConverter},
 * adding support for XML and JSON-based parts.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

  private static final boolean jackson2Present;

  private static final boolean jackson2SmilePresent;

  private static final boolean gsonPresent;

  private static final boolean jsonbPresent;

  private static final boolean kotlinSerializationJsonPresent;

  static {
    ClassLoader classLoader = AllEncompassingFormHttpMessageConverter.class.getClassLoader();
    jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
            ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
    jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
    gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
    jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
    kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
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
    else if (kotlinSerializationJsonPresent) {
      addPartConverter(new KotlinSerializationJsonHttpMessageConverter());
    }

    if (jackson2SmilePresent) {
      addPartConverter(new MappingJackson2SmileHttpMessageConverter());
    }
  }

}
