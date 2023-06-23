/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint;

import cn.taketoday.lang.Nullable;

/**
 * Contract for registering {@link RuntimeHints} based on the {@link ClassLoader}
 * of the deployment unit. Implementations should, if possible, use the specified
 * {@link ClassLoader} to determine if hints have to be contributed.
 *
 * <p>Implementations of this interface can be registered dynamically by using
 * {@link cn.taketoday.context.annotation.ImportRuntimeHints @ImportRuntimeHints}
 * or statically in {@code META-INF/config/aot.factories} by using the FQN of this
 * interface as the key. A standard no-arg constructor is required for implementations.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 */
@FunctionalInterface
public interface RuntimeHintsRegistrar {

  /**
   * Contribute hints to the given {@link RuntimeHints} instance.
   *
   * @param hints the hints contributed so far for the deployment unit
   * @param classLoader the classloader, or {@code null} if even the system ClassLoader isn't accessible
   */
  void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);

}
