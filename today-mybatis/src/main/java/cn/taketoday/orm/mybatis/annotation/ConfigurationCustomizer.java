/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.mybatis.annotation;

import org.apache.ibatis.session.Configuration;

/**
 * Callback interface that can be customized a {@link Configuration} object generated on auto-configuration.
 *
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:22
 */
@FunctionalInterface
public interface ConfigurationCustomizer {

  /**
   * Customize the given a {@link Configuration} object.
   *
   * @param configuration the configuration object to customize
   */
  void customize(Configuration configuration);

}

