/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.scan.valid;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.ConfigurationPropertiesScan;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.properties.scan.valid.b.BScanConfiguration;

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

  @ConfigurationPropertiesScan(basePackages = "infra.context.properties.scan.valid.a",
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
