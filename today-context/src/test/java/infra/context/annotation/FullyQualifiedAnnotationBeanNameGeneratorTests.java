/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.SimpleBeanDefinitionRegistry;

import static infra.context.annotation.AnnotationBeanNameGeneratorTests.annotatedBeanDef;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/3 12:03
 */
class FullyQualifiedAnnotationBeanNameGeneratorTests {

  private final BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  private final FullyQualifiedAnnotationBeanNameGenerator beanNameGenerator = new FullyQualifiedAnnotationBeanNameGenerator();

  @Test
  void buildDefaultBeanName() {
    BeanDefinition bd = annotatedBeanDef(AnnotationBeanNameGeneratorTests.ComponentFromNonStringMeta.class);
    assertThat(this.beanNameGenerator.buildDefaultBeanName(bd, this.registry))
            .isEqualTo(AnnotationBeanNameGeneratorTests.class.getName() + "$ComponentFromNonStringMeta");
  }

}