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

package cn.taketoday.core;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Base class for decorating ClassLoaders such as {@link OverridingClassLoader}
 * providing common handling of excluded packages and classes.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author TODAY 2021/9/11 12:10
 * @since 4.0
 */
public abstract class DecoratingClassLoader extends ClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private final Set<String> excludedPackages = Collections.newSetFromMap(new ConcurrentHashMap<>(8));

  private final Set<String> excludedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(8));

  /**
   * Create a new DecoratingClassLoader with no parent ClassLoader.
   */
  public DecoratingClassLoader() { }

  /**
   * Create a new DecoratingClassLoader using the given parent ClassLoader
   * for delegation.
   */
  public DecoratingClassLoader(@Nullable ClassLoader parent) {
    super(parent);
  }

  /**
   * Add a package name to exclude from decoration (e.g. overriding).
   * <p>Any class whose fully-qualified name starts with the name registered
   * here will be handled by the parent ClassLoader in the usual fashion.
   *
   * @param packageName the package name to exclude
   */
  public void excludePackage(String packageName) {
    Assert.notNull(packageName, "Package name is required");
    this.excludedPackages.add(packageName);
  }

  /**
   * Add a class name to exclude from decoration (e.g. overriding).
   * <p>Any class name registered here will be handled by the parent
   * ClassLoader in the usual fashion.
   *
   * @param className the class name to exclude
   */
  public void excludeClass(String className) {
    Assert.notNull(className, "Class name is required");
    this.excludedClasses.add(className);
  }

  /**
   * Determine whether the specified class is excluded from decoration
   * by this class loader.
   * <p>The default implementation checks against excluded packages and classes.
   *
   * @param className the class name to check
   * @return whether the specified class is eligible
   * @see #excludePackage
   * @see #excludeClass
   */
  protected boolean isExcluded(String className) {
    if (this.excludedClasses.contains(className)) {
      return true;
    }
    for (String packageName : this.excludedPackages) {
      if (className.startsWith(packageName)) {
        return true;
      }
    }
    return false;
  }

}
