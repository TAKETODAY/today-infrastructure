/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.loader.DefinitionLoadingContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests covering cases where a user defines an invalid Configuration
 * class, e.g.: forgets to annotate with {@link Configuration} or declares
 * a Configuration class as final.
 *
 * @author Chris Beams
 */
public class InvalidConfigurationClassDefinitionTests {

  private StandardBeanFactory beanFactory;

  private DefinitionLoadingContext loadingContext;

  @BeforeEach
  void setup() {
    StandardApplicationContext context = new StandardApplicationContext();
    loadingContext = new DefinitionLoadingContext(beanFactory, context);
    beanFactory = context.getBeanFactory();
  }

  @Test
  public void configurationClassesMayNotBeFinal() {
    @Configuration
    final class Config { }

    BeanDefinition configBeanDef = new BeanDefinition(Config.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", configBeanDef);

    ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor(loadingContext);
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
                    pp.postProcessBeanFactory(beanFactory))
            .withMessageContaining("Remove the final modifier");
  }

}
