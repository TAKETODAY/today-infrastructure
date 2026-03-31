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

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.atomic.AtomicBoolean;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test {@link MockitoBean @MockitoBean} for a factory bean configuration.
 *
 * @author Simon Baslé
 */
@JUnitConfig
@TestMethodOrder(OrderAnnotation.class)
public class MockitoBeanForFactoryBeanIntegrationTests {

  @MockitoBean
  private TestBean testBean;

  @Autowired
  private ApplicationContext applicationContext;

  @Order(1)
  @Test
  void beanReturnedByFactoryIsMocked() {
    TestBean bean = this.applicationContext.getBean(TestBean.class);
    assertThat(bean).isSameAs(this.testBean);

    when(this.testBean.hello()).thenReturn("amock");
    assertThat(bean.hello()).isEqualTo("amock");

    assertThat(TestFactoryBean.USED).isFalse();
  }

  @Order(2)
  @Test
  void beanReturnedByFactoryIsReset() {
    assertThat(this.testBean.hello()).isNull();
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    TestFactoryBean testFactoryBean() {
      return new TestFactoryBean();
    }

  }

  static class TestFactoryBean implements FactoryBean<TestBean> {

    static final AtomicBoolean USED = new AtomicBoolean(false);

    @Override
    public TestBean getObject() {
      USED.set(true);
      return () -> "normal";
    }

    @Override
    public Class<?> getObjectType() {
      return TestBean.class;
    }
  }

  public interface TestBean {

    String hello();
  }

}
