/*
 * Copyright 2017 - 2025 the original author or authors.
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
