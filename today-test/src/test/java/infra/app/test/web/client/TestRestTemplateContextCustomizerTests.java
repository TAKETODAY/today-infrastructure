/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.test.web.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.app.test.context.InfraTest;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.AbstractApplicationContext;
import infra.test.context.MergedContextConfiguration;

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
            .run((context) -> Assertions.assertThat(context).hasSingleBean(TestRestTemplate.class));
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
