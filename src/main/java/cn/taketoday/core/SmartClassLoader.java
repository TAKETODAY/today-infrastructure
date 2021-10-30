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

import java.security.ProtectionDomain;

import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by a reloading-aware ClassLoader.
 * Detected for example by bytecode proxy factory for making a caching decision.
 *
 * <p>If a ClassLoader does <i>not</i> implement this interface,
 * then all the classes obtained from it should be considered as
 * not reloadable (i.e. cacheable).
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/30 17:55
 * @since 4.0
 */
public interface SmartClassLoader {

  /**
   * Determine whether the given class is reloadable (in this ClassLoader).
   * <p>Typically used to check whether the result may be cached (for this
   * ClassLoader) or whether it should be reobtained every time.
   * The default implementation always returns {@code false}.
   *
   * @param clazz the class to check (usually loaded from this ClassLoader)
   * @return whether the class should be expected to appear in a reloaded
   * version (with a different {@code Class} object) later on
   */
  default boolean isClassReloadable(Class<?> clazz) {
    return false;
  }

  /**
   * Return the original ClassLoader for this SmartClassLoader, or potentially
   * the present loader itself if it is self-sufficient.
   * <p>The default implementation returns the local ClassLoader reference as-is.
   * In case of a reloadable or other selectively overriding ClassLoader which
   * commonly deals with unaffected classes from a base application class loader,
   * this should get implemented to return the original ClassLoader that the
   * present loader got derived from (e.g. through {@code return getParent();}).
   * <p>This gets specifically used in Spring's AOP framework to determine the
   * class loader for a specific proxy in case the target class has not been
   * defined in the present class loader. In case of a reloadable class loader,
   * we prefer the base application class loader for proxying general classes
   * not defined in the reloadable class loader itself.
   *
   * @return the original ClassLoader (the same reference by default)
   * @see ClassLoader#getParent()
   */
  default ClassLoader getOriginalClassLoader() {
    return (ClassLoader) this;
  }

  /**
   * Define a custom class (typically a CGLIB proxy class) in this class loader.
   * <p>This is a public equivalent of the protected
   * {@code defineClass(String, byte[], int, int, ProtectionDomain)} method
   * in {@link ClassLoader} which is traditionally invoked via reflection.
   * A concrete implementation in a custom class loader should simply delegate
   * to that protected method in order to make classloader-specific definitions
   * publicly available without "illegal access" warnings on JDK 9+:
   * {@code return defineClass(name, b, 0, b.length, protectionDomain)}.
   * Note that the JDK 9+ {@code Lookup#defineClass} method does not support
   * a custom target class loader for the new definition; it rather always
   * defines the class in the same class loader as the lookup's context class.
   *
   * @param name the name of the class
   * @param b the bytes defining the class
   * @param protectionDomain the protection domain for the class, if any
   * @return the newly created class
   * @throws LinkageError in case of a bad class definition
   * @throws SecurityException in case of an invalid definition attempt
   * @throws UnsupportedOperationException in case of a custom definition attempt
   * not being possible (thrown by the default implementation in this interface)
   * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
   */
  default Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
    throw new UnsupportedOperationException();
  }

}

