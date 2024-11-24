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

package infra.app.test.web.client;

import java.util.List;

import infra.app.test.context.InfraTest;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestRestTemplateContextCustomizer
 */
class TestRestTemplateContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(
          Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
    InfraTest infraTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, InfraTest.class);
    return infraTest != null ? new TestRestTemplateContextCustomizer() : null;
  }

}
