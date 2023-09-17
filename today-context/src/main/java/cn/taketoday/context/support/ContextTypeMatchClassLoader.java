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

package cn.taketoday.context.support;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.DecoratingClassLoader;
import cn.taketoday.core.OverridingClassLoader;
import cn.taketoday.core.SmartClassLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * Special variant of an overriding ClassLoader, used for temporary type
 * matching in {@link AbstractApplicationContext}. Redefines classes from
 * a cached byte array for every {@code loadClass} call in order to
 * pick up recently loaded types in the parent ClassLoader.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/30 17:50
 * @see AbstractApplicationContext
 * @see ConfigurableBeanFactory#setTempClassLoader
 * @since 4.0
 */
final class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  @Nullable
  private static final Method findLoadedClassMethod;

  static {
    // Try to enable findLoadedClass optimization which allows us to selectively
    // override classes that have not been loaded yet. If not accessible, we will
    // always override requested classes, even when the classes have been loaded
    // by the parent ClassLoader already and cannot be transformed anymore anyway.
    Method method;
    try {
      method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
      ReflectionUtils.makeAccessible(method);
    }
    catch (Throwable ex) {
      method = null;
      // Typically a JDK 9+ InaccessibleObjectException...
      // Avoid through JVM startup with --add-opens=java.base/java.lang=ALL-UNNAMED
      LoggerFactory.getLogger(ContextTypeMatchClassLoader.class)
              .debug("ClassLoader.findLoadedClass not accessible -> will always override requested class", ex);
    }
    findLoadedClassMethod = method;
  }

  /** Cache for byte array per class name. */
  private final ConcurrentHashMap<String, byte[]> bytesCache = new ConcurrentHashMap<>(256);

  public ContextTypeMatchClassLoader(@Nullable ClassLoader parent) {
    super(parent);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return new ContextOverridingClassLoader(getParent()).loadClass(name);
  }

  @Override
  public boolean isClassReloadable(Class<?> clazz) {
    return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
  }

  @Override
  public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
    return defineClass(name, b, 0, b.length, protectionDomain);
  }

  /**
   * ClassLoader to be created for each loaded class.
   * Caches class file content but redefines class for each call.
   */
  private class ContextOverridingClassLoader extends OverridingClassLoader {

    public ContextOverridingClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected boolean isEligibleForOverriding(String className) {
      if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
        return false;
      }
      if (findLoadedClassMethod != null) {
        ClassLoader parent = getParent();
        while (parent != null) {
          if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
            return false;
          }
          parent = parent.getParent();
        }
      }
      return true;
    }

    @Override
    protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
      byte[] bytes = bytesCache.get(name);
      if (bytes == null) {
        bytes = loadBytesForClass(name);
        if (bytes != null) {
          bytesCache.put(name, bytes);
        }
        else {
          return null;
        }
      }
      return defineClass(name, bytes, 0, bytes.length);
    }
  }

}
