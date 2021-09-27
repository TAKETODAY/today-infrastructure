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

package cn.taketoday.core;

import cn.taketoday.util.ReflectionUtils;

/**
 * today-framework Strategies
 * <p>
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * @author TODAY 2021/9/5 13:57
 * @since 4.0
 */
public final class TodayStrategies extends StrategiesDetector {

  public static final String STRATEGIES_LOCATION = "classpath*:META-INF/today.strategies";
  public static final String KEY_STRATEGIES_LOCATION = "strategies.file.location";
  public static final String KEY_STRATEGIES_FILE_TYPE = "strategies.file.type";

  private static final TodayStrategies todayStrategies;

  static {
    final String strategiesFileType = System.getProperty(KEY_STRATEGIES_FILE_TYPE, Constant.DEFAULT);// default,yaml
    final String strategiesLocation = System.getProperty(KEY_STRATEGIES_LOCATION, STRATEGIES_LOCATION);
    StrategiesReader strategiesReader;
    if (Constant.DEFAULT.equals(strategiesFileType)) {
      strategiesReader = new DefaultStrategiesReader();
    }
    else if ("yaml".equals(strategiesFileType) || "yml".equals(strategiesFileType)) {
      strategiesReader = new YamlStrategiesReader();// org.yaml.snakeyaml.Yaml must present
    }
    else {
      try {
        strategiesReader = ReflectionUtils.newInstance(strategiesFileType);
      }
      catch (ClassNotFoundException e) {
        throw new UnsupportedOperationException("Unsupported strategies file type", e);
      }
    }
    todayStrategies = new TodayStrategies(strategiesReader, strategiesLocation);
  }

  private TodayStrategies(StrategiesReader reader, String strategiesLocation) {
    super(reader, strategiesLocation);
  }

  public static TodayStrategies getDetector() {
    return todayStrategies;
  }

}
