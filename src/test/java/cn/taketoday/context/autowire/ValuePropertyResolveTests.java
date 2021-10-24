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

import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Value;

/**
 * @author Today <br>
 *
 * 2018-08-04 15:58
 */
class ValuePropertyResolveTests {

  @Value("${site.host}")
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
      applicationContext.setPropertiesLocation("info.properties");
      applicationContext.refresh();
      // host
      // ----------------------------
      DefaultPropertySetter host = propertyResolver.resolveProperty(
              resolvingContext, BeanProperty.valueOf(getClass(), "host"));

      assert host.getValue() != null;

      System.out.println("Site -> " + host.getValue());

      // name
      // ----------------------------

      BeanProperty property = BeanProperty.valueOf(getClass(), "name");

      DefaultPropertySetter name = propertyResolver.resolveProperty(resolvingContext, property);

      assert name.getValue() != null;

      System.out.println("Name -> " + name.getValue());

      // test
      // ----------------------------
      ConfigurableEnvironment environment = applicationContext.getEnvironment();

      PropertySources propertySources = environment.getPropertySources();

      Properties properties = new Properties();
      propertySources.addLast(new PropertiesPropertySource("Test", properties));

      properties.put("cn.taketoday.context.autowire.ValuePropertyResolveTests.test", "TEST");

      DefaultPropertySetter test = propertyResolver.resolveProperty(
              resolvingContext, BeanProperty.valueOf(getClass(), "test"));
      assert "TEST".equals(test.getValue());

    }

  }

}
