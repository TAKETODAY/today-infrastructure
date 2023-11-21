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

package cn.taketoday.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

import cn.taketoday.core.DecoratingClassLoader;
import cn.taketoday.core.OverridingClassLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link LoadTimeWeaver} which uses reflection to delegate to an underlying ClassLoader
 * with well-known transformation hooks. The underlying ClassLoader is expected to
 * support the following weaving methods (as defined in the {@link LoadTimeWeaver}
 * interface):
 * <ul>
 * <li>{@code public void addTransformer(java.lang.instrument.ClassFileTransformer)}:
 * for registering the given ClassFileTransformer on this ClassLoader
 * <li>{@code public ClassLoader getThrowawayClassLoader()}:
 * for obtaining a throwaway class loader for this ClassLoader (optional;
 * ReflectiveLoadTimeWeaver will fall back to a SimpleThrowawayClassLoader if
 * that method isn't available)
 * </ul>
 *
 * <p>Please note that the above methods <i>must</i> reside in a class that is
 * publicly accessible, although the class itself does not have to be visible
 * to the application's class loader.
 *
 * <p>The reflective nature of this LoadTimeWeaver is particularly useful when the
 * underlying ClassLoader implementation is loaded in a different class loader itself
 * (such as the application server's class loader which is not visible to the
 * web application). There is no direct API dependency between this LoadTimeWeaver
 * adapter and the underlying ClassLoader, just a 'loose' method contract.
 *
 * <p>This is the LoadTimeWeaver to use e.g. with the Resin application server
 * version 3.1+.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #addTransformer(ClassFileTransformer)
 * @see #getThrowawayClassLoader()
 * @see SimpleThrowawayClassLoader
 * @since 4.0
 */
public class ReflectiveLoadTimeWeaver implements LoadTimeWeaver {
  private static final Logger log = LoggerFactory.getLogger(ReflectiveLoadTimeWeaver.class);

  private static final String ADD_TRANSFORMER_METHOD_NAME = "addTransformer";
  private static final String GET_THROWAWAY_CLASS_LOADER_METHOD_NAME = "getThrowawayClassLoader";

  private final ClassLoader classLoader;

  private final Method addTransformerMethod;

  @Nullable
  private final Method getThrowawayClassLoaderMethod;

  /**
   * Create a new ReflectiveLoadTimeWeaver for the current context class
   * loader, <i>which needs to support the required weaving methods</i>.
   */
  public ReflectiveLoadTimeWeaver() {
    this(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new SimpleLoadTimeWeaver for the given class loader.
   *
   * @param classLoader the {@code ClassLoader} to delegate to for
   * weaving (<i>must</i> support the required weaving methods).
   * @throws IllegalStateException if the supplied {@code ClassLoader}
   * does not support the required weaving methods
   */
  public ReflectiveLoadTimeWeaver(@Nullable ClassLoader classLoader) {
    Assert.notNull(classLoader, "ClassLoader is required");
    this.classLoader = classLoader;

    Method addTransformerMethod = ReflectionUtils.getMethodIfAvailable(
            this.classLoader.getClass(), ADD_TRANSFORMER_METHOD_NAME, ClassFileTransformer.class);
    if (addTransformerMethod == null) {
      throw new IllegalStateException(
              "ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide an " +
                      "'addTransformer(ClassFileTransformer)' method.");
    }
    this.addTransformerMethod = addTransformerMethod;

    Method getThrowawayClassLoaderMethod = ReflectionUtils.getMethodIfAvailable(
            this.classLoader.getClass(), GET_THROWAWAY_CLASS_LOADER_METHOD_NAME);
    // getThrowawayClassLoader method is optional
    if (getThrowawayClassLoaderMethod == null) {
      if (log.isDebugEnabled()) {
        log.debug("The ClassLoader [{}] does NOT provide a " +
                        "'getThrowawayClassLoader()' method; SimpleThrowawayClassLoader will be used instead.",
                classLoader.getClass().getName());
      }
    }
    this.getThrowawayClassLoaderMethod = getThrowawayClassLoaderMethod;
  }

  @Override
  public void addTransformer(ClassFileTransformer transformer) {
    Assert.notNull(transformer, "Transformer is required");
    ReflectionUtils.invokeMethod(this.addTransformerMethod, this.classLoader, transformer);
  }

  @Override
  public ClassLoader getInstrumentableClassLoader() {
    return this.classLoader;
  }

  @Override
  public ClassLoader getThrowawayClassLoader() {
    if (this.getThrowawayClassLoaderMethod != null) {
      ClassLoader target = (ClassLoader)
              ReflectionUtils.invokeMethod(this.getThrowawayClassLoaderMethod, this.classLoader);
      return (target instanceof DecoratingClassLoader ? target :
              new OverridingClassLoader(this.classLoader, target));
    }
    else {
      return new SimpleThrowawayClassLoader(this.classLoader);
    }
  }

}
