/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.properties.scan.valid;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.ConfigurationPropertiesScan;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.context.properties.scan.valid.b.BScanConfiguration;

/**
 * Used for testing {@link ConfigurationProperties @ConfigurationProperties} scanning.
 *
 * @author Madhura Bhave
 */
@ConfigurationPropertiesScan
public class ConfigurationPropertiesScanConfiguration {

  @ConfigurationPropertiesScan
  @EnableConfigurationProperties({ FooProperties.class })
  public static class TestConfiguration {

  }

  @ConfigurationPropertiesScan(basePackages = "cn.taketoday.context.properties.scan.valid.a",
                               basePackageClasses = BScanConfiguration.class)
  public static class DifferentPackageConfiguration {

  }

  @ConfigurationProperties(prefix = "foo")
  static class FooProperties {

  }

  @ConfigurationProperties(prefix = "bar")
  static class BarProperties {

    BarProperties(String foo) {
    }

  }

  @ConfigurationProperties(prefix = "bing")
  static class BingProperties {

    BingProperties() {
    }

    BingProperties(String foo) {
    }

  }

}
