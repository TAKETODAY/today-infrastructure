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
package cn.taketoday.context.autowire;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.beans.factory.support.DependencyResolvingContext;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Today <br>
 *
 * 2018-08-04 16:01
 */
class PropsPropertyResolverTests {

  @Props(value = "info", prefix = "site")
  private Properties properties;

  @Props(value = "info", prefix = "site")
  private String name;

  @Test
  public void propsPropertyResolver() throws Throwable {

    try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.refresh();
      PropsDependencyResolver strategy = new PropsDependencyResolver(applicationContext);

      BeanProperty property1 = BeanProperty.valueOf(getClass(), "properties");

      DependencyDescriptor injectionPoint = new DependencyDescriptor(property1.getField(), false);

      ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();

      DependencyResolvingContext context = new DependencyResolvingContext(null, beanFactory);
      strategy.resolveDependency(injectionPoint, context);
      Object resolveProperty = context.getDependency();

      assertThat(resolveProperty).isNotNull();

      assertThat(resolveProperty).isNotNull()
              .isInstanceOf(Properties.class);

      Properties properties = (Properties) resolveProperty;
      assertThat(properties).isNotEmpty();
      for (String stringPropertyName : properties.stringPropertyNames()) {
        assertThat(stringPropertyName).startsWith("site.");
      }
    }
  }

  @Test
  void errorPropsPropertyResolver() throws Throwable {

  }

}
