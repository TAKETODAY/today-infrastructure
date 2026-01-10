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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;

import static infra.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests covering cases where a user defines an invalid Configuration
 * class, e.g.: forgets to annotate with {@link Configuration} or declares
 * a Configuration class as final.
 *
 * @author Chris Beams
 */
class InvalidConfigurationClassDefinitionTests {

  private BootstrapContext loadingContext;

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    loadingContext = context.getBootstrapContext();
  }

  @Test
  void configurationClassesMayNotBeFinal() {
    @Configuration
    final class Config {
      @Bean
      String dummy() { return "dummy"; }
    }

    BeanDefinition configBeanDef = rootBeanDefinition(Config.class).getBeanDefinition();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", configBeanDef);

    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    assertThatExceptionOfType(BeanDefinitionParsingException.class)
            .isThrownBy(() -> pp.postProcessBeanFactory(beanFactory))
            .withMessageContaining("Remove the final modifier");
  }
}
