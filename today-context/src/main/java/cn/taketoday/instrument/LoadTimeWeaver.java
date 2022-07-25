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

package cn.taketoday.instrument;

import java.lang.instrument.ClassFileTransformer;

/**
 * Defines the contract for adding one or more
 * {@link ClassFileTransformer ClassFileTransformers} to a {@link ClassLoader}.
 *
 * <p>Implementations may operate on the current context {@code ClassLoader}
 * or expose their own instrumentable {@code ClassLoader}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @see ClassFileTransformer
 * @since 4.0
 */
public interface LoadTimeWeaver {

  /**
   * Add a {@code ClassFileTransformer} to be applied by this
   * {@code LoadTimeWeaver}.
   *
   * @param transformer the {@code ClassFileTransformer} to add
   */
  void addTransformer(ClassFileTransformer transformer);

  /**
   * Return a {@code ClassLoader} that supports instrumentation
   * through AspectJ-style load-time weaving based on user-defined
   * {@link ClassFileTransformer ClassFileTransformers}.
   * <p>May be the current {@code ClassLoader}, or a {@code ClassLoader}
   * created by this {@link LoadTimeWeaver} instance.
   *
   * @return the {@code ClassLoader} which will expose
   * instrumented classes according to the registered transformers
   */
  ClassLoader getInstrumentableClassLoader();

  /**
   * Return a throwaway {@code ClassLoader}, enabling classes to be
   * loaded and inspected without affecting the parent {@code ClassLoader}.
   * <p>Should <i>not</i> return the same instance of the {@link ClassLoader}
   * returned from an invocation of {@link #getInstrumentableClassLoader()}.
   *
   * @return a temporary throwaway {@code ClassLoader}; should return
   * a new instance for each call, with no existing state
   */
  ClassLoader getThrowawayClassLoader();

}
