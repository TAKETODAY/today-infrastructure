/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.context.ApplicationContext;

/**
 * Interface used to generate an 'exit code' from a running command line
 * {@link Application}. Can be used on exceptions as well as directly on beans.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Application#exit(ApplicationContext, ExitCodeGenerator...)
 * @since 4.0 2022/1/16 20:36
 */
@FunctionalInterface
public interface ExitCodeGenerator {

  /**
   * Returns the exit code that should be returned from the application.
   *
   * @return the exit code.
   */
  int getExitCode();

}

