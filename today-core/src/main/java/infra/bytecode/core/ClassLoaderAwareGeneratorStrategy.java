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

package infra.bytecode.core;

import infra.lang.Nullable;

/**
 * GeneratorStrategy variant which exposes the application ClassLoader
 * as current thread context ClassLoader for the time of class generation.
 * The ASM ClassWriter in ASM variant will pick it up when doing
 * common superclass resolution.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0 2021/10/30 17:59
 */
public class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

  private final ClassLoader classLoader;

  @Nullable
  private final GeneratorStrategy delegate;

  /**
   * Create a default GeneratorStrategy, exposing the given ClassLoader.
   *
   * @param classLoader the ClassLoader to expose as current thread context ClassLoader
   */
  public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader) {
    this(classLoader, null);
  }

  /**
   * Create a decorator for the given GeneratorStrategy delegate, exposing the given ClassLoader.
   *
   * @param classLoader the ClassLoader to expose as current thread context ClassLoader
   * @since 5.0
   */
  public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader, @Nullable GeneratorStrategy delegate) {
    this.classLoader = classLoader;
    this.delegate = delegate;
  }

  @Override
  public byte[] generate(ClassGenerator cg) throws Exception {
    if (this.classLoader == null) {
      return generateInternal(cg);
    }

    Thread currentThread = Thread.currentThread();
    ClassLoader threadContextClassLoader;
    try {
      threadContextClassLoader = currentThread.getContextClassLoader();
    }
    catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
      return generateInternal(cg);
    }

    boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
    if (overrideClassLoader) {
      currentThread.setContextClassLoader(this.classLoader);
    }
    try {
      return generateInternal(cg);
    }
    finally {
      if (overrideClassLoader) {
        // Reset original thread context ClassLoader.
        currentThread.setContextClassLoader(threadContextClassLoader);
      }
    }
  }

  private byte[] generateInternal(ClassGenerator cg) throws Exception {
    if (delegate != null) {
      return delegate.generate(cg);
    }
    return super.generate(cg);
  }

}
