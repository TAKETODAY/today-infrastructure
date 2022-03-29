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

package cn.taketoday.framework.cloud;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.lang.Nullable;

/**
 * Simple detection for well known cloud platforms. Detection can be forced using the
 * {@code "context.main.cloud-platform"} configuration property.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Nguyen Sach
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/28 18:12
 */
public enum CloudPlatform {

  /**
   * No Cloud platform. Useful when false-positives are detected.
   */
  NONE {
    @Override
    public boolean isDetected(Environment environment) {
      return false;
    }

  },

  /**
   * Cloud Foundry platform.
   */
  CLOUD_FOUNDRY {
    @Override
    public boolean isDetected(Environment environment) {
      return environment.containsProperty("VCAP_APPLICATION") || environment.containsProperty("VCAP_SERVICES");
    }

  },

  /**
   * Heroku platform.
   */
  HEROKU {
    @Override
    public boolean isDetected(Environment environment) {
      return environment.containsProperty("DYNO");
    }

  },

  /**
   * SAP Cloud platform.
   */
  SAP {
    @Override
    public boolean isDetected(Environment environment) {
      return environment.containsProperty("HC_LANDSCAPE");
    }

  },

  /**
   * Kubernetes platform.
   */
  KUBERNETES {

    private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";

    private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";

    private static final String SERVICE_HOST_SUFFIX = "_SERVICE_HOST";

    private static final String SERVICE_PORT_SUFFIX = "_SERVICE_PORT";

    @Override
    public boolean isDetected(Environment environment) {
      if (environment instanceof ConfigurableEnvironment) {
        return isAutoDetected((ConfigurableEnvironment) environment);
      }
      return false;
    }

    private boolean isAutoDetected(ConfigurableEnvironment environment) {
      PropertySource<?> environmentPropertySource = environment.getPropertySources()
              .get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
      if (environmentPropertySource != null) {
        if (environmentPropertySource.containsProperty(KUBERNETES_SERVICE_HOST)
                && environmentPropertySource.containsProperty(KUBERNETES_SERVICE_PORT)) {
          return true;
        }
        if (environmentPropertySource instanceof EnumerablePropertySource) {
          return isAutoDetected((EnumerablePropertySource<?>) environmentPropertySource);
        }
      }
      return false;
    }

    private boolean isAutoDetected(EnumerablePropertySource<?> environmentPropertySource) {
      for (String propertyName : environmentPropertySource.getPropertyNames()) {
        if (propertyName.endsWith(SERVICE_HOST_SUFFIX)) {
          String serviceName = propertyName.substring(0,
                  propertyName.length() - SERVICE_HOST_SUFFIX.length());
          if (environmentPropertySource.getProperty(serviceName + SERVICE_PORT_SUFFIX) != null) {
            return true;
          }
        }
      }
      return false;
    }

  },

  /**
   * Azure App Service platform.
   */
  AZURE_APP_SERVICE {

    private final List<String> azureEnvVariables = Arrays.asList("WEBSITE_SITE_NAME", "WEBSITE_INSTANCE_ID",
            "WEBSITE_RESOURCE_GROUP", "WEBSITE_SKU");

    @Override
    public boolean isDetected(Environment environment) {
      return this.azureEnvVariables.stream().allMatch(environment::containsProperty);
    }

  };

  private static final String PROPERTY_NAME = "context.main.cloud-platform";

  /**
   * Determines if the platform is active (i.e. the application is running in it).
   *
   * @param environment the environment
   * @return if the platform is active.
   */
  public boolean isActive(Environment environment) {
    String platformProperty = environment.getProperty(PROPERTY_NAME);
    return isEnforced(platformProperty) || (platformProperty == null && isDetected(environment));
  }

  /**
   * Determines if the platform is enforced by looking at the
   * {@code "context.main.cloud-platform"} configuration property.
   *
   * @param environment the environment
   * @return if the platform is enforced
   */
  public boolean isEnforced(Environment environment) {
    return isEnforced(environment.getProperty(PROPERTY_NAME));
  }

  /**
   * Determines if the platform is enforced by looking at the
   * {@code "context.main.cloud-platform"} configuration property.
   *
   * @param binder the binder
   * @return if the platform is enforced
   */
  public boolean isEnforced(Binder binder) {
    return isEnforced(binder.bind(PROPERTY_NAME, String.class).orElse(null));
  }

  private boolean isEnforced(@Nullable String platform) {
    return name().equalsIgnoreCase(platform);
  }

  /**
   * Determines if the platform is detected by looking for platform-specific environment
   * variables.
   *
   * @param environment the environment
   * @return if the platform is auto-detected.
   */
  public abstract boolean isDetected(Environment environment);

  /**
   * Returns if the platform is behind a load balancer and uses
   * {@literal X-Forwarded-For} headers.
   *
   * @return if {@literal X-Forwarded-For} headers are used
   */
  public boolean isUsingForwardHeaders() {
    return true;
  }

  /**
   * Returns the active {@link CloudPlatform} or {@code null} if one is not active.
   *
   * @param environment the environment
   * @return the {@link CloudPlatform} or {@code null}
   */
  @Nullable
  public static CloudPlatform getActive(Environment environment) {
    if (environment != null) {
      for (CloudPlatform cloudPlatform : values()) {
        if (cloudPlatform.isActive(environment)) {
          return cloudPlatform;
        }
      }
    }
    return null;
  }

}
