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

package cn.taketoday.core.type.filter;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * A simple filter which matches classes that are assignable to a given type.
 *
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author TODAY
 * @since 4.0
 */
public class AssignableTypeFilter extends AbstractTypeHierarchyTraversingFilter {

  private final Class<?> targetType;

  /**
   * Create a new AssignableTypeFilter for the given type.
   *
   * @param targetType the type to match
   */
  public AssignableTypeFilter(Class<?> targetType) {
    super(true, true);
    this.targetType = targetType;
  }

  /**
   * Return the {@code type} that this instance is using to filter candidates.
   */
  public final Class<?> getTargetType() {
    return this.targetType;
  }

  @Override
  protected boolean matchClassName(String className) {
    return this.targetType.getName().equals(className);
  }

  @Override
  @Nullable
  protected Boolean matchSuperClass(String superClassName) {
    return matchTargetType(superClassName);
  }

  @Override
  @Nullable
  protected Boolean matchInterface(String interfaceName) {
    return matchTargetType(interfaceName);
  }

  @Nullable
  protected Boolean matchTargetType(String typeName) {
    if (this.targetType.getName().equals(typeName)) {
      return true;
    }
    else if (Object.class.getName().equals(typeName)) {
      return false;
    }
    else if (typeName.startsWith("java")) {
      try {
        Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
        return this.targetType.isAssignableFrom(clazz);
      }
      catch (Throwable ex) {
        // Class not regularly loadable - can't determine a match that way.
      }
    }
    return null;
  }

}
