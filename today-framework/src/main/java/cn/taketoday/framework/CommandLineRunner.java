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

package cn.taketoday.framework;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;

/**
 * Interface used to indicate that a bean should <em>run</em> when it is contained within
 * a {@link Application}. Multiple {@link CommandLineRunner} beans can be defined
 * within the same application context and can be ordered using the {@link Ordered}
 * interface or {@link Order @Order} annotation.
 * <p>
 * If you need access to {@link ApplicationArguments} instead of the raw String array
 * consider using {@link ApplicationRunner#run(ApplicationArguments)}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationRunner
 * @since 4.0 2022/1/16 20:54
 */
@FunctionalInterface
public interface CommandLineRunner extends ApplicationRunner {

  @Override
  default void run(ApplicationArguments args) throws Exception {
    run(args.getSourceArgs());
  }

  /**
   * Callback used to run the bean.
   *
   * @param args incoming main method arguments
   * @throws Exception on error
   */
  void run(String... args) throws Exception;

}

