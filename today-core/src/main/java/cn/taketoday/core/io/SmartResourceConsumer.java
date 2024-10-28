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

package cn.taketoday.core.io;

import java.util.Set;

import cn.taketoday.lang.Modifiable;

/**
 * Extended ResourceConsumer
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/28 14:54
 */
public interface SmartResourceConsumer extends ResourceConsumer {

  /**
   * Check if the resource has been added
   *
   * @param resource resource to check
   * @return Check if the resource has been added
   */
  boolean contains(Resource resource);

  /**
   * All Resources
   */
  @Modifiable
  Set<Resource> getResources();

}
