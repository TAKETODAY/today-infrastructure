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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Andy Wilkinson
 */
public class ImportVersusDirectRegistrationTests {

  @Test
  public void thingIsNotAvailableWhenOuterConfigurationIsRegisteredDirectly() {
    try (StandardApplicationContext directRegistration = new StandardApplicationContext()) {
      directRegistration.register(AccidentalLiteConfiguration.class);
      directRegistration.refresh();
      Thing bean = directRegistration.getBean(Thing.class);
      assertThat(bean).isNull();
    }
  }

  @Test
  public void thingIsNotAvailableWhenOuterConfigurationIsRegisteredWithClassName() {
    try (StandardApplicationContext directRegistration = new StandardApplicationContext()) {
      directRegistration.registerBeanDefinition("config",
              new BeanDefinition(AccidentalLiteConfiguration.class.getName()));
      directRegistration.refresh();

      assertThat(directRegistration.getBean(Thing.class)).isNull();
    }
  }

  @Test
  public void thingIsNotAvailableWhenOuterConfigurationIsImported() {
    try (StandardApplicationContext viaImport = new StandardApplicationContext()) {
      viaImport.register(Importer.class);
      viaImport.refresh();
      assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
              BeanFactoryUtils.requiredBean(viaImport, Thing.class));
    }
  }

}

@Import(AccidentalLiteConfiguration.class)
class Importer {
}

class AccidentalLiteConfiguration {

  @Configuration
  class InnerConfiguration {

    @Bean
    public Thing thing() {
      return new Thing();
    }
  }
}

class Thing {
}
