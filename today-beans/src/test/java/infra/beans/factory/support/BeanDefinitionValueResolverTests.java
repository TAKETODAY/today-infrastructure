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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.BeanDefinitionValueResolver;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionValueResolver}.
 *
 * @author Stephane Nicoll
 */
class BeanDefinitionValueResolverTests {

  @Test
  void resolveInnerBean() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition parentBd = new RootBeanDefinition();
    GenericBeanDefinition innerBd = new GenericBeanDefinition();
    innerBd.setAttribute("test", 42);
    BeanDefinitionValueResolver bdvr = new BeanDefinitionValueResolver(beanFactory, "test", parentBd);
    RootBeanDefinition resolvedInnerBd = bdvr.resolveInnerBean(null, innerBd, (name, mbd) -> {
      assertThat(name).isNotNull().startsWith("(inner bean");
      return mbd;
    });
    assertThat(resolvedInnerBd.getAttribute("test")).isEqualTo(42);
  }

}
