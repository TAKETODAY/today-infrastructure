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

package cn.taketoday.test.context.junit4.spr9051;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify proper scoping of beans created in
 * <em>{@code @Bean} Lite Mode</em>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(classes = AtBeanLiteModeScopeTests.LiteBeans.class)
public class AtBeanLiteModeScopeTests {

  /**
   * This is intentionally <b>not</b> annotated with {@code @Configuration}.
   */
  static class LiteBeans {

    @Bean
    public LifecycleBean singleton() {
      LifecycleBean bean = new LifecycleBean("singleton");
      assertThat(bean.isInitialized()).isFalse();
      return bean;
    }

    @Bean
    @Scope("prototype")
    public LifecycleBean prototype() {
      LifecycleBean bean = new LifecycleBean("prototype");
      assertThat(bean.isInitialized()).isFalse();
      return bean;
    }
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  @Qualifier("singleton")
  private LifecycleBean injectedSingletonBean;

  @Autowired
  @Qualifier("prototype")
  private LifecycleBean injectedPrototypeBean;

  @Test
  public void singletonLiteBean() {
    assertThat(injectedSingletonBean).isNotNull();
    assertThat(injectedSingletonBean.isInitialized()).isTrue();

    LifecycleBean retrievedSingletonBean = applicationContext.getBean("singleton", LifecycleBean.class);
    assertThat(retrievedSingletonBean).isNotNull();
    assertThat(retrievedSingletonBean.isInitialized()).isTrue();

    assertThat(retrievedSingletonBean).isSameAs(injectedSingletonBean);
  }

  @Test
  public void prototypeLiteBean() {
    assertThat(injectedPrototypeBean).isNotNull();
    assertThat(injectedPrototypeBean.isInitialized()).isTrue();

    LifecycleBean retrievedPrototypeBean = applicationContext.getBean("prototype", LifecycleBean.class);
    assertThat(retrievedPrototypeBean).isNotNull();
    assertThat(retrievedPrototypeBean.isInitialized()).isTrue();

    assertThat(retrievedPrototypeBean).isNotSameAs(injectedPrototypeBean);
  }

}
