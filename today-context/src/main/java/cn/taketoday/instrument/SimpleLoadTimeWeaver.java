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

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * {@code LoadTimeWeaver} that builds and exposes a
 * {@link SimpleInstrumentableClassLoader}.
 *
 * <p>Mainly intended for testing environments, where it is sufficient to
 * perform all class transformation on a newly created
 * {@code ClassLoader} instance.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getInstrumentableClassLoader()
 * @see SimpleInstrumentableClassLoader
 * @see ReflectiveLoadTimeWeaver
 * @since 4.0
 */
public class SimpleLoadTimeWeaver implements LoadTimeWeaver {

  private final SimpleInstrumentableClassLoader classLoader;

  /**
   * Create a new {@code SimpleLoadTimeWeaver} for the current context
   * {@code ClassLoader}.
   *
   * @see SimpleInstrumentableClassLoader
   */
  public SimpleLoadTimeWeaver() {
    this.classLoader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
  }

  /**
   * Create a new {@code SimpleLoadTimeWeaver} for the given
   * {@code ClassLoader}.
   *
   * @param classLoader the {@code ClassLoader} to build a simple
   * instrumentable {@code ClassLoader} on top of
   */
  public SimpleLoadTimeWeaver(SimpleInstrumentableClassLoader classLoader) {
    Assert.notNull(classLoader, "ClassLoader must not be null");
    this.classLoader = classLoader;
  }

  @Override
  public void addTransformer(ClassFileTransformer transformer) {
    this.classLoader.addTransformer(transformer);
  }

  @Override
  public ClassLoader getInstrumentableClassLoader() {
    return this.classLoader;
  }

  /**
   * This implementation builds a {@link SimpleThrowawayClassLoader}.
   */
  @Override
  public ClassLoader getThrowawayClassLoader() {
    return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
  }

}
