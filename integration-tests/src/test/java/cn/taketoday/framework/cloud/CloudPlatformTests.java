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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 11:20
 */
class CloudPlatformTests {

  @Test
  void getActiveWhenEnvironmentIsNullShouldReturnNull() {
    CloudPlatform platform = CloudPlatform.getActive(null);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenNotInCloudShouldReturnNull() {
    Environment environment = new MockEnvironment();
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasVcapApplicationShouldReturnCloudFoundry() {
    Environment environment = new MockEnvironment().withProperty("VCAP_APPLICATION", "---");
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.CLOUD_FOUNDRY);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasVcapServicesShouldReturnCloudFoundry() {
    Environment environment = new MockEnvironment().withProperty("VCAP_SERVICES", "---");
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.CLOUD_FOUNDRY);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasDynoShouldReturnHeroku() {
    Environment environment = new MockEnvironment().withProperty("DYNO", "---");
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.HEROKU);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasHcLandscapeShouldReturnSap() {
    Environment environment = new MockEnvironment().withProperty("HC_LANDSCAPE", "---");
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.SAP);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasKubernetesServiceHostAndPortShouldReturnKubernetes() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("KUBERNETES_SERVICE_HOST", "---");
    envVars.put("KUBERNETES_SERVICE_PORT", "8080");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasKubernetesServiceHostAndNoKubernetesServicePortShouldNotReturnKubernetes() {
    Environment environment = getEnvironmentWithEnvVariables(
            Collections.singletonMap("KUBERNETES_SERVICE_HOST", "---"));
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasKubernetesServicePortAndNoKubernetesServiceHostShouldNotReturnKubernetes() {
    Environment environment = getEnvironmentWithEnvVariables(
            Collections.singletonMap("KUBERNETES_SERVICE_PORT", "8080"));
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasServiceHostAndServicePortShouldReturnKubernetes() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("EXAMPLE_SERVICE_HOST", "---");
    envVars.put("EXAMPLE_SERVICE_PORT", "8080");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasServiceHostAndNoServicePortShouldNotReturnKubernetes() {
    Environment environment = getEnvironmentWithEnvVariables(
            Collections.singletonMap("EXAMPLE_SERVICE_HOST", "---"));
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasAllAzureEnvVariablesShouldReturnAzureAppService() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("WEBSITE_SITE_NAME", "---");
    envVars.put("WEBSITE_INSTANCE_ID", "1234");
    envVars.put("WEBSITE_RESOURCE_GROUP", "test");
    envVars.put("WEBSITE_SKU", "1234");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.AZURE_APP_SERVICE);
    assertThat(platform.isActive(environment)).isTrue();
  }

  @Test
  void getActiveWhenHasMissingWebsiteSiteNameShouldNotReturnAzureAppService() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("WEBSITE_INSTANCE_ID", "1234");
    envVars.put("WEBSITE_RESOURCE_GROUP", "test");
    envVars.put("WEBSITE_SKU", "1234");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasMissingWebsiteInstanceIdShouldNotReturnAzureAppService() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("WEBSITE_SITE_NAME", "---");
    envVars.put("WEBSITE_RESOURCE_GROUP", "test");
    envVars.put("WEBSITE_SKU", "1234");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasMissingWebsiteResourceGroupShouldNotReturnAzureAppService() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("WEBSITE_SITE_NAME", "---");
    envVars.put("WEBSITE_INSTANCE_ID", "1234");
    envVars.put("WEBSITE_SKU", "1234");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasMissingWebsiteSkuShouldNotReturnAzureAppService() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("WEBSITE_SITE_NAME", "---");
    envVars.put("WEBSITE_INSTANCE_ID", "1234");
    envVars.put("WEBSITE_RESOURCE_GROUP", "test");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isNull();
  }

  @Test
  void getActiveWhenHasEnforcedCloudPlatform() {
    Environment environment = getEnvironmentWithEnvVariables(
            Collections.singletonMap("context.main.cloud-platform", "kubernetes"));
    CloudPlatform platform = CloudPlatform.getActive(environment);
    assertThat(platform).isEqualTo(CloudPlatform.KUBERNETES);
  }

  @Test
  void isEnforcedWhenEnvironmentPropertyMatchesReturnsTrue() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.main.cloud-platform", "kubernetes");
    assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isTrue();
  }

  @Test
  void isEnforcedWhenEnvironmentPropertyDoesNotMatchReturnsFalse() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.main.cloud-platform", "heroku");
    assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isFalse();
  }

  @Test
  void isEnforcedWhenEnvironmentPropertyIsMissingReturnsFalse() {
    MockEnvironment environment = new MockEnvironment();
    assertThat(CloudPlatform.KUBERNETES.isEnforced(environment)).isFalse();
  }

  @Test
  void isEnforcedWhenBinderPropertyMatchesReturnsTrue() {
    Binder binder = new Binder(new MockConfigurationPropertySource("context.main.cloud-platform", "kubernetes"));
    assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isTrue();
  }

  @Test
  void isEnforcedWhenBinderPropertyDoesNotMatchReturnsFalse() {
    Binder binder = new Binder(new MockConfigurationPropertySource("context.main.cloud-platform", "heroku"));
    assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isFalse();
  }

  @Test
  void isEnforcedWhenBinderPropertyIsMissingReturnsFalse() {
    Binder binder = new Binder(new MockConfigurationPropertySource());
    assertThat(CloudPlatform.KUBERNETES.isEnforced(binder)).isFalse();
  }

  void isActiveWhenNoCloudPlatformIsEnforcedAndHasKubernetesServiceHostAndKubernetesServicePort() {
    Map<String, Object> envVars = new HashMap<>();
    envVars.put("EXAMPLE_SERVICE_HOST", "---");
    envVars.put("EXAMPLE_SERVICE_PORT", "8080");
    Environment environment = getEnvironmentWithEnvVariables(envVars);
    ((MockEnvironment) environment).setProperty("context.main.cloud-platform", "none");
    assertThat(Stream.of(CloudPlatform.values()).filter((platform) -> platform.isActive(environment)))
            .containsExactly(CloudPlatform.NONE);
  }

  private Environment getEnvironmentWithEnvVariables(Map<String, Object> environmentVariables) {
    MockEnvironment environment = new MockEnvironment();
    PropertySource<?> propertySource = new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environmentVariables);
    environment.getPropertySources().addFirst(propertySource);
    return environment;
  }

}
