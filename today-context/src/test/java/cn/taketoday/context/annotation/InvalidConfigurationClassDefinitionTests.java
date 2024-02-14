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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.parsing.BeanDefinitionParsingException;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.BootstrapContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests covering cases where a user defines an invalid Configuration
 * class, e.g.: forgets to annotate with {@link Configuration} or declares
 * a Configuration class as final.
 *
 * @author Chris Beams
 */
public class InvalidConfigurationClassDefinitionTests {

  private BootstrapContext loadingContext;

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    loadingContext = context.getBootstrapContext();
  }

  @Test
  public void configurationClassesMayNotBeFinal() {
    @Configuration
    final class Config { }

    BeanDefinition configBeanDef = new RootBeanDefinition(Config.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", configBeanDef);

    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
                    pp.postProcessBeanFactory(beanFactory))
            .withMessageContaining("Remove the final modifier");
  }

}
