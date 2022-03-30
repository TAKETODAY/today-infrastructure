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

package cn.taketoday.framework;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:28
 */
public class TestPropertySourceUtils {
  /**
   * The name of the {@link MapPropertySource} created from <em>inlined properties</em>.
   *
   * @since 4.0
   */
  public static final String INLINED_PROPERTIES_PROPERTY_SOURCE_NAME = "Inlined Test Properties";
  private static final Logger logger = LoggerFactory.getLogger(TestPropertySourceUtils.class);

  public static void addInlinedPropertiesToEnvironment(ConfigurableApplicationContext context, String... inlinedProperties) {
    Assert.notNull(context, "'context' must not be null");
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    addInlinedPropertiesToEnvironment(context.getEnvironment(), inlinedProperties);
  }

  public static void addInlinedPropertiesToEnvironment(ConfigurableEnvironment environment, String... inlinedProperties) {
    Assert.notNull(environment, "'environment' must not be null");
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    if (ObjectUtils.isNotEmpty(inlinedProperties)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Adding inlined properties to environment: " +
                ObjectUtils.nullSafeToString(inlinedProperties));
      }
      MapPropertySource ps = (MapPropertySource)
              environment.getPropertySources().get(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
      if (ps == null) {
        ps = new MapPropertySource(INLINED_PROPERTIES_PROPERTY_SOURCE_NAME, new LinkedHashMap<>());
        environment.getPropertySources().addFirst(ps);
      }
      ps.getSource().putAll(convertInlinedPropertiesToMap(inlinedProperties));
    }
  }

  public static Map<String, Object> convertInlinedPropertiesToMap(String... inlinedProperties) {
    Assert.notNull(inlinedProperties, "'inlinedProperties' must not be null");
    Map<String, Object> map = new LinkedHashMap<>();
    Properties props = new Properties();

    for (String pair : inlinedProperties) {
      if (!StringUtils.hasText(pair)) {
        continue;
      }
      try {
        props.load(new StringReader(pair));
      }
      catch (Exception ex) {
        throw new IllegalStateException("Failed to load test environment property from [" + pair + "]", ex);
      }
      Assert.state(props.size() == 1, () -> "Failed to load exactly one test environment property from [" + pair + "]");
      for (String name : props.stringPropertyNames()) {
        map.put(name, props.getProperty(name));
      }
      props.clear();
    }

    return map;
  }
}
