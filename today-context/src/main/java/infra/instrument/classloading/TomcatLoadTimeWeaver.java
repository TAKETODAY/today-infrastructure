/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import infra.core.OverridingClassLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * {@link LoadTimeWeaver} implementation
 * for Tomcat's new {@code org.apache.tomcat.InstrumentableClassLoader}.
 * Also capable of handling Framework's TomcatInstrumentableClassLoader when encountered.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TomcatLoadTimeWeaver implements LoadTimeWeaver {

  static final String INSTRUMENTABLE_LOADER_CLASS_NAME = "org.apache.tomcat.InstrumentableClassLoader";

  private final ClassLoader classLoader;
  private final Method copyMethod;
  private final Method addTransformerMethod;

  /**
   * Create a new instance of the {@link TomcatLoadTimeWeaver} class using
   * the default {@link ClassLoader class loader}.
   *
   * @see ClassUtils#getDefaultClassLoader()
   */
  public TomcatLoadTimeWeaver() {
    this(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new instance of the {@link TomcatLoadTimeWeaver} class using
   * the supplied {@link ClassLoader}.
   *
   * @param classLoader the {@code ClassLoader} to delegate to for weaving
   */
  public TomcatLoadTimeWeaver(@Nullable ClassLoader classLoader) {
    Assert.notNull(classLoader, "ClassLoader is required");
    this.classLoader = classLoader;

    Class<?> instrumentableLoaderClass;
    try {
      instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_LOADER_CLASS_NAME);
      if (!instrumentableLoaderClass.isInstance(classLoader)) {
        // Could still be a custom variant of a convention-compatible ClassLoader
        instrumentableLoaderClass = classLoader.getClass();
      }
    }
    catch (ClassNotFoundException ex) {
      // We're on an earlier version of Tomcat, probably with Framework's TomcatInstrumentableClassLoader
      instrumentableLoaderClass = classLoader.getClass();
    }

    try {
      this.addTransformerMethod = instrumentableLoaderClass.getMethod("addTransformer", ClassFileTransformer.class);
      // Check for Tomcat's new copyWithoutTransformers on InstrumentableClassLoader first
      Method copyMethod = ReflectionUtils.getMethodIfAvailable(instrumentableLoaderClass, "copyWithoutTransformers");
      if (copyMethod == null) {
        // Fallback: expecting TomcatInstrumentableClassLoader's getThrowawayClassLoader
        copyMethod = instrumentableLoaderClass.getMethod("getThrowawayClassLoader");
      }
      this.copyMethod = copyMethod;
    }
    catch (Throwable ex) {
      throw new IllegalStateException(
              "Could not initialize TomcatLoadTimeWeaver because Tomcat API classes are not available", ex);
    }
  }

  @Override
  public void addTransformer(ClassFileTransformer transformer) {
    try {
      this.addTransformerMethod.invoke(this.classLoader, transformer);
    }
    catch (InvocationTargetException ex) {
      throw new IllegalStateException("Tomcat addTransformer method threw exception", ex.getCause());
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not invoke Tomcat addTransformer method", ex);
    }
  }

  @Override
  public ClassLoader getInstrumentableClassLoader() {
    return this.classLoader;
  }

  @Override
  public ClassLoader getThrowawayClassLoader() {
    try {
      return new OverridingClassLoader(this.classLoader, (ClassLoader) this.copyMethod.invoke(this.classLoader));
    }
    catch (InvocationTargetException ex) {
      throw new IllegalStateException("Tomcat copy method threw exception", ex.getCause());
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not invoke Tomcat copy method", ex);
    }
  }

}
