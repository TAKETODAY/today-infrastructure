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

package infra.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import infra.core.OverridingClassLoader;
import infra.lang.Nullable;

/**
 * Simplistic implementation of an instrumentable {@code ClassLoader}.
 *
 * <p>Usable in tests and standalone environments.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleInstrumentableClassLoader extends OverridingClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private final WeavingTransformer weavingTransformer;

  /**
   * Create a new SimpleInstrumentableClassLoader for the given ClassLoader.
   *
   * @param parent the ClassLoader to build an instrumentable ClassLoader for
   */
  public SimpleInstrumentableClassLoader(@Nullable ClassLoader parent) {
    super(parent);
    this.weavingTransformer = new WeavingTransformer(parent);
  }

  /**
   * Add a {@link ClassFileTransformer} to be applied by this ClassLoader.
   *
   * @param transformer the {@link ClassFileTransformer} to register
   */
  public void addTransformer(ClassFileTransformer transformer) {
    this.weavingTransformer.addTransformer(transformer);
  }

  @Override
  protected byte[] transformIfNecessary(String name, byte[] bytes) {
    return this.weavingTransformer.transformIfNecessary(name, bytes);
  }

}
