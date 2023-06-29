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

package cn.taketoday.test.context.aot;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.test.context.TestExecutionListener;

/**
 * {@code AotTestExecutionListener} is an extension of the {@link TestExecutionListener}
 * SPI that allows a listener to optionally provide ahead-of-time (AOT) support.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface AotTestExecutionListener extends TestExecutionListener {

  /**
   * Process the supplied test class ahead-of-time using the given
   * {@link RuntimeHints} instance.
   * <p>If possible, implementations should use the specified {@link ClassLoader}
   * to determine if hints have to be contributed.
   *
   * @param runtimeHints the {@code RuntimeHints} to use
   * @param testClass the test class to process
   * @param classLoader the classloader to use
   */
  void processAheadOfTime(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader);

}
