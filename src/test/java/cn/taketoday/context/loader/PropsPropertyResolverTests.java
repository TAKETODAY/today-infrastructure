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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.loader;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.StandardApplicationContext;

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
      PropsPropertyResolver propertyResolver = new PropsPropertyResolver();
      PropertyResolvingContext resolvingContext = new PropertyResolvingContext(applicationContext);

      DefaultPropertySetter resolveProperty = //
              (DefaultPropertySetter) propertyResolver.resolveProperty(resolvingContext, PropsPropertyResolverTests.class.getDeclaredField("properties"));

      assert resolveProperty.getValue() != null;

      System.out.println("====================");
      System.out.println(resolveProperty.getValue());
    }
  }

  @Test
  void errorPropsPropertyResolver() throws Throwable {

    try (ApplicationContext applicationContext = new StandardApplicationContext()) {
      PropsPropertyResolver propertyResolver = new PropsPropertyResolver();
      PropertyResolvingContext resolvingContext = new PropertyResolvingContext(applicationContext);

      propertyResolver.resolveProperty(resolvingContext, PropsPropertyResolverTests.class.getDeclaredField("name"));
    }

  }

}
