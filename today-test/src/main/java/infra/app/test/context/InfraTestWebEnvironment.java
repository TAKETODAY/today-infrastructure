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

package infra.app.test.context;

import infra.context.ConfigurableApplicationContext;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} to track the web environment that is used in a
 * {@link InfraTest}. The web environment is taken into account when evaluating a
 * {@link MergedContextConfiguration} to determine if a context can be shared between
 * tests.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class InfraTestWebEnvironment implements ContextCustomizer {

  private final WebEnvironment webEnvironment;

  InfraTestWebEnvironment(Class<?> testClass) {
    InfraTest infraTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, InfraTest.class);
    this.webEnvironment = infraTest != null ? infraTest.webEnvironment() : null;
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
  }

  @Override
  public boolean equals(Object obj) {
    return (obj != null) && (getClass() == obj.getClass())
            && this.webEnvironment == ((InfraTestWebEnvironment) obj).webEnvironment;
  }

  @Override
  public int hashCode() {
    return (this.webEnvironment != null) ? this.webEnvironment.hashCode() : 0;
  }

}
