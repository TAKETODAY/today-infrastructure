/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanFactoryUtils;
import infra.context.annotation.ImportSelector;
import infra.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImportSelector} to check no {@link TestRestTemplate} definition is registered
 * when config classes are processed.
 */
class NoTestRestTemplateBeanChecker implements ImportSelector, BeanFactoryAware {

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, TestRestTemplate.class))
            .isEmpty();
  }

  @Override
  public String[] selectImports(AnnotationMetadata importMetadata) {
    return new String[0];
  }

}
