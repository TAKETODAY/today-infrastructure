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
package cn.taketoday.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.util.ResourceUtils;

/**
 * @author Today <br>
 *
 * 2018-11-15 16:56
 */
class StandardEnvironmentTests {

  @Test
  void test_AutoloadProperties() throws Exception {
    try (ApplicationContext applicationContext = new StandardApplicationContext("")) {
      Environment environment = applicationContext.getEnvironment();
      assert "https://taketoday.cn".equals(environment.getProperty("site.host"));
    }
  }

  @Test
  void test_loadProperties() throws IOException {
    ConfigurableEnvironment environment = new StandardEnvironment();

    PropertySources propertySources = environment.getPropertySources();

    Properties properties = PropertiesUtils.loadProperties(
            ResourceUtils.getResource("classpath:info.properties"));

    propertySources.addLast(new PropertiesPropertySource("info", properties));

    assert "https://taketoday.cn".equals(environment.getProperty("site.host"));
  }

  @Test
  void test_ActiveProfile() throws IOException {

    try (ApplicationContext applicationContext
            = new StandardApplicationContext("", "cn.taketoday.context.env")) {
      Environment environment = applicationContext.getEnvironment();

      String[] activeProfiles = environment.getActiveProfiles();
      for (String string : activeProfiles) {
        System.err.println(string);
      }
      assert "test".equals(activeProfiles[0]);
    }
  }

  @Test
  void test_AddActiveProfile() throws IOException {

    try (ConfigurableApplicationContext applicationContext
            = new StandardApplicationContext("", "cn.taketoday.context.env")) {
      ConfigurableEnvironment environment = applicationContext.getEnvironment();

      environment.addActiveProfile("prod");
      String[] activeProfiles = environment.getActiveProfiles();
      assert activeProfiles.length == 3;
      assert environment.acceptsProfiles("prod");
    }
  }

  @Test
  void test_AcceptsProfiles() throws IOException {

    try (ConfigurableApplicationContext applicationContext
            = new StandardApplicationContext("", "cn.taketoday.context.env")) {
      ConfigurableEnvironment environment = applicationContext.getEnvironment();

      assert environment.acceptsProfiles("test");
    }
  }

}
