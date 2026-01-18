/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import infra.core.DefaultAliasRegistry;
import infra.expression.EvaluationException;
import infra.expression.ExpressionException;
import infra.expression.TypeLocator;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.util.ClassUtils;

/**
 * A simple implementation of {@link TypeLocator} that uses the context ClassLoader
 * (or any ClassLoader set upon it). It supports 'well-known' packages: So if a
 * type cannot be found, it will try the registered imports to locate it.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardTypeLocator extends DefaultAliasRegistry implements TypeLocator {

  @Nullable
  private final ClassLoader classLoader;

  private final HashMap<String, Class<?>> classMap = new HashMap<>();
  private final HashMap<String, String> classNameMap = new HashMap<>();

  private final HashSet<String> notAClass = new HashSet<>();
  private final ArrayList<String> packages = new ArrayList<>(1);

  /**
   * Create a {@code StandardTypeLocator} for the default {@link ClassLoader}
   * (typically, the thread context {@code ClassLoader}).
   * <p>Favor {@link #StandardTypeLocator(ClassLoader)} over this constructor
   * in order to provide a specific {@link ClassLoader} that is able to reliably
   * locate user types.
   *
   * @see ClassUtils#getDefaultClassLoader()
   */
  public StandardTypeLocator() {
    this(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a {@code StandardTypeLocator} for the given {@link ClassLoader}.
   * <p>Favor this constructor over {@link #StandardTypeLocator()} in order
   * to provide a specific {@link ClassLoader} that is able to reliably locate
   * user types.
   *
   * @param classLoader the {@code ClassLoader} to delegate to
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
    this.packages.add(prefix);
  }

  /**
   * Remove that specified prefix from this locator's list of imports.
   *
   * @param prefix the prefix to remove
   */
  public void removeImport(String prefix) {
    this.packages.remove(prefix);
  }

  /**
   * Return a list of all the import prefixes registered with this StandardTypeLocator.
   *
   * @return a list of registered import prefixes
   */
  public List<String> getImportPrefixes() {
    return Collections.unmodifiableList(this.packages);
  }

  /**
   * Import a class.
   *
   * @param name The full class name of the class to be imported
   * @throws ExpressionException if the name does not include a ".".
   */
  public void importClass(String name) throws ExpressionException {
    int i = name.lastIndexOf('.');
    if (i <= 0) {
      throw new ExpressionException("The name " + name + " is not a full class name");
    }
    String className = ClassUtils.getShortName(name);
    classNameMap.put(className, name);
  }

  /**
   * Import a class.
   *
   * @see ClassUtils#getSimpleName(String)
   */
  public void importClass(Class<?> classToImport) {
    String className = classToImport.getName();
    String simpleName = ClassUtils.getSimpleName(className);

    classMap.put(simpleName, classToImport);
    classMap.put(className, classToImport);

    notAClass.remove(simpleName);
    notAClass.remove(className);
  }

  public void registerAlias(Class<?> classToImport, String... alias) {
    importClass(classToImport);
    for (String alia : alias) {
      registerAlias(classToImport.getName(), alia);
    }
  }

  /**
   * Resolve a class name.
   *
   * @param typeName The name of the class (without package name) to be resolved.
   * @return If the class has been imported previously, with {@link #importClass}
   * or {@link #registerImport(String)}, then its Class instance. Otherwise
   * <code>null</code>.
   * @throws ExpressionException if the class is abstract or is an interface, or not public.
   */
  @Override
  public Class<?> findType(String typeName) throws EvaluationException {
    String className = classNameMap.get(typeName);
    if (className == null) {
      className = canonicalName(typeName);
    }

    Class<?> type = resolveClassFor(className);
    if (type != null) {
      return type;
    }

    for (String packageName : packages) {
      String fullClassName = packageName + "." + className;
      Class<?> c = resolveClassFor(fullClassName);
      if (c != null) {
        classNameMap.put(typeName, fullClassName);
        return c;
      }
    }
    throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
  }

  @Nullable
  private Class<?> resolveClassFor(String className) {
    Class<?> c = classMap.get(className);
    if (c != null) {
      return c;
    }
    c = getClassFor(className);
    if (c != null) {
      classMap.put(className, c);
    }
    return c;
  }

  @Nullable
  public Class<?> getClassFor(String className) {
    if (!notAClass.contains(className)) {
      String nameToLookup = className;
      try {
        return ClassUtils.forName(nameToLookup, classLoader);
      }
      catch (ClassNotFoundException ey) {
        // try any registered prefixes before giving up
      }
      for (String prefix : packages) {
        try {
          nameToLookup = prefix + '.' + className;
          return ClassUtils.forName(nameToLookup, classLoader);
        }
        catch (ClassNotFoundException ex) {
          // might be a different prefix
        }
      }
      notAClass.add(className);
    }
    return null;
  }

}
