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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Import;
import infra.stereotype.Component;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link ImportsContextCustomizerFactory} and
 * {@link ImportsContextCustomizer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@Import(ImportsContextCustomizerFactoryIntegrationTests.ImportedBean.class)
class ImportsContextCustomizerFactoryIntegrationTests {

  @Autowired
  private ApplicationContext context;

  @Autowired
  private ImportedBean bean;

  @Test
  void beanWasImported() {
    assertThat(this.bean).isNotNull();
  }

  @Test
  void testItselfIsNotABean() {
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.context.getBean(getClass()));
  }

  @Component
  static class ImportedBean {

  }

}