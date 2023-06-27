/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.web.client;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.test.context.InfraTest.WebEnvironment;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.web.client.TestRestTemplateContextCustomizer.TestRestTemplateRegistrar;
import cn.taketoday.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestRestTemplateContextCustomizer}.
 *
 * @author Andy Wilkinson
 */
class TestRestTemplateContextCustomizerTests {

  @Test
  void whenContextIsNotABeanDefinitionRegistryTestRestTemplateIsRegistered() {
    new ApplicationContextRunner(TestApplicationContext::new)
            .withInitializer(this::applyTestRestTemplateContextCustomizer)
            .run((context) -> assertThat(context).hasSingleBean(TestRestTemplate.class));
  }

//  @Test
//  void whenUsingAotGeneratedArtifactsTestRestTemplateIsNotRegistered() {
//    new ApplicationContextRunner().withSystemProperties("infra.aot.enabled:true")
//            .withInitializer(this::applyTestRestTemplateContextCustomizer)
//            .run((context) -> {
//              assertThat(context).doesNotHaveBean(TestRestTemplateRegistrar.class);
//              assertThat(context).doesNotHaveBean(TestRestTemplate.class);
//            });
//  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  void applyTestRestTemplateContextCustomizer(ConfigurableApplicationContext context) {
    MergedContextConfiguration configuration = mock(MergedContextConfiguration.class);
    given(configuration.getTestClass()).willReturn((Class) TestClass.class);
    new TestRestTemplateContextCustomizer().customizeContext(context, configuration);
  }

  @InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  static class TestClass {

  }

  static class TestApplicationContext extends AbstractApplicationContext {

    private final StandardBeanFactory beanFactory = new StandardBeanFactory();

    @Override
    protected void refreshBeanFactory() {
    }

    @Override
    protected void closeBeanFactory() {

    }

    @Override
    public StandardBeanFactory getBeanFactory() {
      return this.beanFactory;
    }

  }

}
