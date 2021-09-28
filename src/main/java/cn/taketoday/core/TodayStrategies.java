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

  /**
   * Retrieve the flag for the given property key.
   *
   * @param key
   *         the property key
   *
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  public static boolean getFlag(String key) {
    return getDetector().getBoolean(key);
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param key
   *         the property key
   *
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  public static boolean getFlag(String key, boolean defaultFlag) {
    return getDetector().getBoolean(key, defaultFlag);
  }

  /**
   * Programmatically set a local flag to "true", overriding an
   * entry in the {@link #STRATEGIES_LOCATION} file (if any).
   *
   * @param key
   *         the property key
   */
  public static void setFlag(String key) {
    getDetector().getStrategies().set(key, Boolean.TRUE.toString());
  }

  /**
   * Programmatically set a local property, overriding an entry in the
   * {@link #STRATEGIES_LOCATION} file (if any).
   *
   * @param key
   *         the property key
   * @param value
   *         the associated property value, or {@code null} to reset it
   */
  public static void setProperty(String key, @Nullable String value) {
    if (value != null) {
      getDetector().getStrategies().set(key, value);
    }
    else {
      getDetector().getStrategies().remove(key);
    }
  }

  /**
   * Retrieve the property value for the given key, checking local Spring
   * properties first and falling back to JVM-level system properties.
   *
   * @param key
   *         the property key
   *
   * @return the associated property value, or {@code null} if none found
   */
  @Nullable
  public static String getProperty(String key) {
    return getDetector().getFirst(key);
  }

}
