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
import cn.taketoday.context.Env;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.Value;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySources;

/**
 * @author Today <br>
 *
 * 2018-08-04 15:58
 */
class ValuePropertyResolveTests {

  @Value("#{site.host}")
  private String host = null;

  @Env("site.name")
  private String name = null;

  @Env
  private String test = null;

  @Test
  public void valuePropertyResolver() throws Exception {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {
      ValuePropertyResolver propertyResolver = new ValuePropertyResolver();
      PropertyResolvingContext resolvingContext = new PropertyResolvingContext(applicationContext);

      // host
      // ----------------------------
      DefaultPropertySetter host = (DefaultPropertySetter) propertyResolver.resolveProperty(
              resolvingContext, ValuePropertyResolveTests.class.getDeclaredField("host"));

      assert host.getValue() != null;

      System.out.println("Site -> " + host.getValue());

      // name
      // ----------------------------
      DefaultPropertySetter name = (DefaultPropertySetter) propertyResolver.resolveProperty(
              resolvingContext, ValuePropertyResolveTests.class.getDeclaredField("name"));

      assert name.getValue() != null;

      System.out.println("Name -> " + name.getValue());

      // test
      // ----------------------------
      ConfigurableEnvironment environment = applicationContext.getEnvironment();

      PropertySources propertySources = environment.getPropertySources();

      Properties properties = new Properties();
      propertySources.addLast(new PropertiesPropertySource("Test", properties));

      properties.put("cn.taketoday.context.loader.ValuePropertyResolveTest.test", "TEST");

      DefaultPropertySetter test = (DefaultPropertySetter) propertyResolver.resolveProperty(
              resolvingContext, ValuePropertyResolveTests.class.getDeclaredField("test"));
      assert "TEST".equals(test.getValue());

    }

  }

}
