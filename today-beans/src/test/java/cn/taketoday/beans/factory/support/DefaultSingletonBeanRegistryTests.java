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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.testfixture.beans.DerivedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/10/2 15:51
 */
class DefaultSingletonBeanRegistryTests {

  private final DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

  @Test
  void singletons() {
    TestBean tb = new TestBean();
    beanRegistry.registerSingleton("tb", tb);
    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

    TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", TestBean::new);
    assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);

    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
    assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
    assertThat(beanRegistry.getSingletonCount()).isEqualTo(2);
    assertThat(beanRegistry.getSingletonNames()).containsExactly("tb", "tb2");

    beanRegistry.destroySingletons();
    assertThat(beanRegistry.getSingletonCount()).isZero();
    assertThat(beanRegistry.getSingletonNames()).isEmpty();
  }

  @Test
  void disposableBean() {
    DerivedTestBean tb = new DerivedTestBean();
    beanRegistry.registerSingleton("tb", tb);
    beanRegistry.registerDisposableBean("tb", tb);
    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
    assertThat(beanRegistry.getSingletonCount()).isEqualTo(1);
    assertThat(beanRegistry.getSingletonNames()).containsExactly("tb");
    assertThat(tb.wasDestroyed()).isFalse();

    beanRegistry.destroySingletons();
    assertThat(beanRegistry.getSingletonCount()).isZero();
    assertThat(beanRegistry.getSingletonNames()).isEmpty();
    assertThat(tb.wasDestroyed()).isTrue();
  }

  @Test
  void dependentRegistration() {
    beanRegistry.registerDependentBean("a", "b");
    beanRegistry.registerDependentBean("b", "c");
    beanRegistry.registerDependentBean("c", "b");
    assertThat(beanRegistry.isDependent("a", "b")).isTrue();
    assertThat(beanRegistry.isDependent("b", "c")).isTrue();
    assertThat(beanRegistry.isDependent("c", "b")).isTrue();
    assertThat(beanRegistry.isDependent("a", "c")).isTrue();
    assertThat(beanRegistry.isDependent("c", "a")).isFalse();
    assertThat(beanRegistry.isDependent("b", "a")).isFalse();
    assertThat(beanRegistry.isDependent("a", "a")).isFalse();
    assertThat(beanRegistry.isDependent("b", "b")).isTrue();
    assertThat(beanRegistry.isDependent("c", "c")).isTrue();
  }

}
