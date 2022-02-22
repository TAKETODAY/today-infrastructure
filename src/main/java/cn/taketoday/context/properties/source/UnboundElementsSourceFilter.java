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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import java.util.Set;
import java.util.function.Function;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;

/**
 * Function used to determine if a {@link ConfigurationPropertySource} should be included
 * when determining unbound elements. If the underlying {@link PropertySource} is a
 * systemEnvironment or systemProperties property source, it will not be considered for
 * unbound element failures.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnboundElementsSourceFilter implements Function<ConfigurationPropertySource, Boolean> {

  private static final Set<String> BENIGN_PROPERTY_SOURCE_NAMES = Set.of(
          StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
          StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME
  );

  @Override
  public Boolean apply(ConfigurationPropertySource configurationPropertySource) {
    Object underlyingSource = configurationPropertySource.getUnderlyingSource();
    if (underlyingSource instanceof PropertySource) {
      String name = ((PropertySource<?>) underlyingSource).getName();
      return !BENIGN_PROPERTY_SOURCE_NAMES.contains(name);
    }
    return true;
  }

}
