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

package infra.app.test.web.reactive.server;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.AbstractApplicationContext;
import infra.app.test.context.InfraTest;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.test.context.MergedContextConfiguration;
import infra.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebTestClientContextCustomizer}.
 *
 * @author Moritz Halbritter
 */
class WebTestClientContextCustomizerTests {

  @Test
  void whenContextIsNotABeanDefinitionRegistryWebTestClientIsRegistered() {
    new ApplicationContextRunner(TestApplicationContext::new)
            .withInitializer(this::applyWebTestClientContextCustomizer)
            .run((context) -> Assertions.assertThat(context).hasSingleBean(WebTestClient.class));
  }

//  @Test
//  void whenUsingAotGeneratedArtifactsWebTestClientIsNotRegistered() {
//    new ApplicationContextRunner().withSystemProperties("infra.aot.enabled:true")
//            .withInitializer(this::applyWebTestClientContextCustomizer)
//            .run((context) -> {
//              assertThat(context).doesNotHaveBean(WebTestClientRegistrar.class);
//              assertThat(context).doesNotHaveBean(WebTestClient.class);
//            });
//  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  void applyWebTestClientContextCustomizer(ConfigurableApplicationContext context) {
    MergedContextConfiguration configuration = mock(MergedContextConfiguration.class);
    given(configuration.getTestClass()).willReturn((Class) TestClass.class);
    new WebTestClientContextCustomizer().customizeContext(context, configuration);
  }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT)
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
