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

package cn.taketoday.framework.test.context;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.framework.test.context.ApplicationTest.WebEnvironment;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} to track the web environment that is used in a
 * {@link ApplicationTest}. The web environment is taken into account when evaluating a
 * {@link MergedContextConfiguration} to determine if a context can be shared between
 * tests.
 *
 * @author Andy Wilkinson
 */
class ApplicationTestWebEnvironment implements ContextCustomizer {

  private final WebEnvironment webEnvironment;

  ApplicationTestWebEnvironment(Class<?> testClass) {
    ApplicationTest sprintBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
            ApplicationTest.class);
    this.webEnvironment = (sprintBootTest != null) ? sprintBootTest.webEnvironment() : null;
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null) && (getClass() == obj.getClass())
            && this.webEnvironment == ((ApplicationTestWebEnvironment) obj).webEnvironment;
  }

  @Override
  public int hashCode() {
    return (this.webEnvironment != null) ? this.webEnvironment.hashCode() : 0;
  }

}
