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

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

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

  @Nullable
  private final ClassLoader classLoader;

  @Nullable
  private final GeneratorStrategy delegate;

  /**
   * Create a default GeneratorStrategy, exposing the given ClassLoader.
   *
   * @param classLoader the ClassLoader to expose as current thread context ClassLoader
   */
  public ClassLoaderAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
    this(classLoader, null);
  }

  /**
   * Create a decorator for the given GeneratorStrategy delegate, exposing the given ClassLoader.
   *
   * @param classLoader the ClassLoader to expose as current thread context ClassLoader
   * @since 5.0
   */
  public ClassLoaderAwareGeneratorStrategy(@Nullable ClassLoader classLoader, @Nullable GeneratorStrategy delegate) {
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
