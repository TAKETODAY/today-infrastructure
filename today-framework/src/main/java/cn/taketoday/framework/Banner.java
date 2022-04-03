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

package cn.taketoday.framework;

import java.io.PrintStream;

import cn.taketoday.core.env.Environment;

/**
 * Interface class for writing a banner programmatically.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Jeremy Rickard
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:56
 */
@FunctionalInterface
public interface Banner {
  String BEAN_NAME = "applicationBanner";

  /**
   * Print the banner to the specified print stream.
   *
   * @param environment the spring environment
   * @param sourceClass the source class for the application
   * @param out the output print stream
   */
  void printBanner(Environment environment, Class<?> sourceClass, PrintStream out);

  /**
   * An enumeration of possible values for configuring the Banner.
   */
  enum Mode {

    /**
     * Disable printing of the banner.
     */
    OFF,

    /**
     * Print the banner to System.out.
     */
    CONSOLE,

    /**
     * Print the banner to the log file.
     */
    LOG

  }

}
