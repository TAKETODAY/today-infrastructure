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

package cn.taketoday.framework.test.mock.mockito;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;

/**
 * A {@link ContextCustomizer} to add Mockito support.
 *
 * @author Phillip Webb
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
