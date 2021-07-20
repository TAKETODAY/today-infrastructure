/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.loader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.utils.MultiValueMap;

/**
 * Yaml files reader
 * <p>
 * read as {@link MultiValueMap}
 *
 * <pre>
 *  cn:
 *   taketoday:
 *     context:
 *       loader:
 *         PropertyValueResolver:
 *           - 111111
 *           - 222222
 *           - 333333
 *           - cn.taketoday.context.loader.StrategiesDetectorTests$MyPropertyValueResolver
 *   </pre>
 *
 * @author TODAY 2021/7/17 22:42
 * @see MultiValueMap
 * @since 3.1.0
 */
public class YamlStrategiesReader extends StrategiesReader {

  @Override
  protected void readInternal(InputStream yamlStream, MultiValueMap<String, String> properties) {
    final Map<String, Object> base = new Yaml(new CompactConstructor()).load(yamlStream);
    doMapping(properties, base, null);
  }

  @SuppressWarnings("unchecked")
  static void doMapping(final MultiValueMap<String, String> properties, final Map<String, Object> base, final String prefix) {
    for (final Map.Entry<String, Object> entry : base.entrySet()) {
      String key = entry.getKey();
      final Object value = entry.getValue();
      key = prefix == null ? key : (prefix + '.' + key);
      if (value instanceof Map) {
        doMapping(properties, (Map<String, Object>) value, key);
      }
      else if (value instanceof List) {
        properties.addAll(key, (List<? extends String>) value);
      }
      else if (value instanceof String) {
        properties.add(key, value.toString());
      }
      else {
        log.warn("not support value: '{}' map to properties", value);
      }
    }
  }

}
