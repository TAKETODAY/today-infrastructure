/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test {@link MockitoBean @MockitoBean} for a {@link FactoryBean} that is
 * "broken" or not able to be eagerly initialized.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 */
@JUnitConfig
public class MockitoBeanForBrokenFactoryBeanIntegrationTests {

  @MockitoBean
  TestBean testBean;

  @Test
  void beanReturnedByFactoryIsMocked(@Autowired TestBean autowiredTestBean) {
    assertThat(autowiredTestBean).isSameAs(testBean);

    when(testBean.hello()).thenReturn("mock");

    assertThat(testBean.hello()).isEqualTo("mock");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    TestFactoryBean testFactoryBean() {
      return new TestFactoryBean();
    }
  }

  static class TestFactoryBean implements FactoryBean<TestBean> {

    TestFactoryBean() {
      throw new BeanCreationException("simulating missing dependencies");
    }

    @Override
    public TestBean getObject() {
      return () -> "prod";
    }

    @Override
    public Class<?> getObjectType() {
      return TestBean.class;
    }
  }

  interface TestBean {

    String hello();
  }

}
