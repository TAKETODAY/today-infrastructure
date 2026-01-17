/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.test.context;

import infra.app.test.context.InfraTest.WebEnvironment;
import infra.context.ConfigurableApplicationContext;
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
