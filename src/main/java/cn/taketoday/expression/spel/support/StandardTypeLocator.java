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

package cn.taketoday.expression.spel.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeLocator;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * A simple implementation of {@link TypeLocator} that uses the context ClassLoader
 * (or any ClassLoader set upon it). It supports 'well-known' packages: So if a
 * type cannot be found, it will try the registered imports to locate it.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class StandardTypeLocator implements TypeLocator {

  @Nullable
  private final ClassLoader classLoader;

  private final List<String> knownPackagePrefixes = new ArrayList<>(1);

  /**
   * Create a StandardTypeLocator for the default ClassLoader
   * (typically, the thread context ClassLoader).
   */
  public StandardTypeLocator() {
    this(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a StandardTypeLocator for the given ClassLoader.
   *
   * @param classLoader the ClassLoader to delegate to
   */
  public StandardTypeLocator(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
    // Similar to when writing regular Java code, it only knows about java.lang by default
    registerImport("java.lang");
  }

  /**
   * Register a new import prefix that will be used when searching for unqualified types.
   * Expected format is something like "java.lang".
   *
   * @param prefix the prefix to register
   */
  public void registerImport(String prefix) {
    this.knownPackagePrefixes.add(prefix);
  }

  /**
   * Remove that specified prefix from this locator's list of imports.
   *
   * @param prefix the prefix to remove
   */
  public void removeImport(String prefix) {
    this.knownPackagePrefixes.remove(prefix);
  }

  /**
   * Return a list of all the import prefixes registered with this StandardTypeLocator.
   *
   * @return a list of registered import prefixes
   */
  public List<String> getImportPrefixes() {
    return Collections.unmodifiableList(this.knownPackagePrefixes);
  }

  /**
   * Find a (possibly unqualified) type reference - first using the type name as-is,
   * then trying any registered prefixes if the type name cannot be found.
   *
   * @param typeName the type to locate
   * @return the class object for the type
   * @throws EvaluationException if the type cannot be found
   */
  @Override
  public Class<?> findType(String typeName) throws EvaluationException {
    String nameToLookup = typeName;
    try {
      return ClassUtils.forName(nameToLookup, this.classLoader);
    }
    catch (ClassNotFoundException ey) {
      // try any registered prefixes before giving up
    }
    for (String prefix : this.knownPackagePrefixes) {
      try {
        nameToLookup = prefix + '.' + typeName;
        return ClassUtils.forName(nameToLookup, this.classLoader);
      }
      catch (ClassNotFoundException ex) {
        // might be a different prefix
      }
    }
    throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
  }

}
