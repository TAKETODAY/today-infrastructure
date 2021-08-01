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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.StringUtils;

/**
 * Default read strategy
 * <p>
 * read properties file structure and split as string list
 * </p>
 *
 * @author TODAY 2021/7/17 22:47
 * @since 4.0
 */
public class DefaultStrategiesReader extends StrategiesReader {

  @Override
  protected void readInternal(InputStream inputStream, MultiValueMap<String, String> strategies)
          throws IOException {

    final Properties properties = new Properties();
    properties.load(inputStream);

    for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
      final Object key = entry.getKey();
      final Object value = entry.getValue();
      if (key != null && value != null) {
        final String strategyKey = key.toString();
        // split as string list
        final List<String> strategyValues = StringUtils.splitAsList(value.toString());
        for (String strategyValue : strategyValues) {
          strategyValue = strategyValue.trim(); // trim whitespace
          if (StringUtils.isNotEmpty(strategyValue)) {
            strategies.add(strategyKey, strategyValue);
          }
        }
      }
    }

  }

}
