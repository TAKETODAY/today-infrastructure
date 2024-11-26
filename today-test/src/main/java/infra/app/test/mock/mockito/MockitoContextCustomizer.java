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

package infra.app.test.mock.mockito;

import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;

/**
 * A {@link ContextCustomizer} to add Mockito support.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class MockitoContextCustomizer implements ContextCustomizer {

  private final Set<Definition> definitions;

  MockitoContextCustomizer(Set<? extends Definition> definitions) {
    this.definitions = new LinkedHashSet<>(definitions);
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context,
          MergedContextConfiguration mergedContextConfiguration) {
    if (context instanceof BeanDefinitionRegistry) {
      MockitoPostProcessor.register((BeanDefinitionRegistry) context, this.definitions);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    MockitoContextCustomizer other = (MockitoContextCustomizer) obj;
    return this.definitions.equals(other.definitions);
  }

  @Override
  public int hashCode() {
    return this.definitions.hashCode();
  }

}
