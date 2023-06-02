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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test {@link MockBean @MockBean} for a factory bean.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
class MockBeanForBeanFactoryIntegrationTests {

  // gh-7439

  @MockBean
  private TestFactoryBean testFactoryBean;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void testName() {
    TestBean testBean = mock(TestBean.class);
    given(testBean.hello()).willReturn("amock");
    given(this.testFactoryBean.getObjectType()).willReturn((Class) TestBean.class);
    given(this.testFactoryBean.getObject()).willReturn(testBean);
    TestBean bean = this.applicationContext.getBean(TestBean.class);
    assertThat(bean.hello()).isEqualTo("amock");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    TestFactoryBean testFactoryBean() {
      return new TestFactoryBean();
    }

  }

  static class TestFactoryBean implements FactoryBean<TestBean> {

    @Override
    public TestBean getObject() {
      return () -> "normal";
    }

    @Override
    public Class<?> getObjectType() {
      return TestBean.class;
    }

    @Override
    public boolean isSingleton() {
      return false;
    }

  }

  interface TestBean {

    String hello();

  }

}
