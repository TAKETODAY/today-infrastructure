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
package cn.taketoday.framework;

import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.SimpleCommandLinePropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2019-06-17 22:34
 */
public class StandardWebEnvironment extends StandardEnvironment {

  private final String[] arguments;

  public StandardWebEnvironment() {
    this.arguments = null;
  }

  public StandardWebEnvironment(String... arguments) {
    this.arguments = arguments;
  }

  /**
   * Add command-line arguments
   *
   * @param propertySources
   *         propertySources add to
   *
   * @since 4.0
   */
  @Override
  protected void customizePropertySources(PropertySources propertySources) {
    super.customizePropertySources(propertySources);
    if (ObjectUtils.isNotEmpty(arguments)) {
      propertySources.addFirst(new SimpleCommandLinePropertySource(arguments));
    }
  }

}
