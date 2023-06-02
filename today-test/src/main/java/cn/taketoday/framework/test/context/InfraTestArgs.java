/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context;

import java.util.Arrays;
import java.util.Set;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to track application arguments that are used in a
 * {@link InfraTest}. The application arguments are taken into account when
 * evaluating a {@link MergedContextConfiguration} to determine if a context can be shared
 * between tests.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class InfraTestArgs implements ContextCustomizer {

  private static final String[] NO_ARGS = new String[0];

  private final String[] args;

  InfraTestArgs(Class<?> testClass) {
    this.args = MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
            .get(InfraTest.class).getValue("args", String[].class).orElse(NO_ARGS);
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
  }

  String[] getArgs() {
    return this.args;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null) && (getClass() == obj.getClass())
            && Arrays.equals(this.args, ((InfraTestArgs) obj).args);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.args);
  }

  /**
   * Return the application arguments from the given customizers.
   *
   * @param customizers the customizers to check
   * @return the application args or an empty array
   */
  static String[] get(Set<ContextCustomizer> customizers) {
    for (ContextCustomizer customizer : customizers) {
      if (customizer instanceof InfraTestArgs) {
        return ((InfraTestArgs) customizer).args;
      }
    }
    return NO_ARGS;
  }

}
