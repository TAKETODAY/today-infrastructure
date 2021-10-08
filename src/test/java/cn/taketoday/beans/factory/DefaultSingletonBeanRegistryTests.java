/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/10/2 15:51
 */
class DefaultSingletonBeanRegistryTests {

  @Test
  public void testSingletons() {
    DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

    TestBean tb = new TestBean();
    beanRegistry.registerSingleton("tb", tb);
    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

    TestBean tb2 = beanRegistry.getSingleton("tb2", TestBean::new);
    assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);

    assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
    assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
    assertThat(beanRegistry.getSingletonCount()).isEqualTo(2);
    Set<String> names = beanRegistry.getSingletonNames();
    assertThat(names).hasSize(2).contains("tb", "tb2");

    assertThat(beanRegistry.getSingletonCount()).isEqualTo(0);
    assertThat(beanRegistry.getSingletonNames().size()).isEqualTo(0);
  }

}
