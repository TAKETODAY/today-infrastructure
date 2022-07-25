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

package cn.taketoday.instrument;

import cn.taketoday.core.OverridingClassLoader;
import cn.taketoday.lang.Nullable;

/**
 * ClassLoader that can be used to load classes without bringing them
 * into the parent loader. Intended to support JPA "temp class loader"
 * requirement, but not JPA-specific.
 *
 * @author Rod Johnson
 * @since 4.0
 */
public class SimpleThrowawayClassLoader extends OverridingClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  /**
   * Create a new SimpleThrowawayClassLoader for the given ClassLoader.
   *
   * @param parent the ClassLoader to build a throwaway ClassLoader for
   */
  public SimpleThrowawayClassLoader(@Nullable ClassLoader parent) {
    super(parent);
  }

}
