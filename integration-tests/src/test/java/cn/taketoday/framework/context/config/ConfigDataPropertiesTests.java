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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.context.config.ConfigDataProperties.Activate;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataProperties}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataPropertiesTests {

  private static final CloudPlatform NULL_CLOUD_PLATFORM = null;

  private static final Profiles NULL_PROFILES = null;

  private static final List<ConfigDataLocation> NO_IMPORTS = Collections.emptyList();

  @Test
  void getImportsReturnsImports() {
    ConfigDataLocation l1 = ConfigDataLocation.valueOf("one");
    ConfigDataLocation l2 = ConfigDataLocation.valueOf("two");
    ConfigDataLocation l3 = ConfigDataLocation.valueOf("three");
    List<ConfigDataLocation> imports = Arrays.asList(l1, l2, l3);
    ConfigDataProperties properties = new ConfigDataProperties(imports, null);
    assertThat(properties.getImports()).containsExactly(l1, l2, l3);
  }

  @Test
  void getImportsWhenImportsAreNullReturnsEmptyList() {
    ConfigDataProperties properties = new ConfigDataProperties(null, null);
    assertThat(properties.getImports()).isEmpty();
  }

  @Test
  void isActiveWhenNullCloudPlatformAgainstNullCloudPlatform() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenNullCloudPlatformAgainstSpecificCloudPlatform() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, NULL_PROFILES);
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenSpecificCloudPlatformAgainstNullCloudPlatform() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(CloudPlatform.KUBERNETES, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void isActiveWhenSpecificCloudPlatformAgainstMatchingSpecificCloudPlatform() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(CloudPlatform.KUBERNETES, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, NULL_PROFILES);
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenSpecificCloudPlatformAgainstDifferentSpecificCloudPlatform() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(CloudPlatform.KUBERNETES, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.HEROKU, NULL_PROFILES);
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void isActiveWhenNullProfilesAgainstNullProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, NULL_PROFILES);
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenNullProfilesAgainstSpecificProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, new Activate(null, null));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
            createTestProfiles());
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenSpecificProfilesAgainstNullProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(null, new String[] { "a" }));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM, null);
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void isActiveWhenSpecificProfilesAgainstMatchingSpecificProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(null, new String[] { "a" }));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
            createTestProfiles());
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenSpecificProfilesAgainstMissingSpecificProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(null, new String[] { "x" }));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
            createTestProfiles());
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void isActiveWhenProfileExpressionAgainstSpecificProfiles() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS,
            new Activate(null, new String[] { "a | b" }));
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
            createTestProfiles());
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveWhenActivateIsNull() {
    ConfigDataProperties properties = new ConfigDataProperties(NO_IMPORTS, null);
    ConfigDataActivationContext context = new ConfigDataActivationContext(NULL_CLOUD_PLATFORM,
            createTestProfiles());
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveAgainstBoundData() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("app.config.activate.on-cloud-platform", "kubernetes");
    source.put("app.config.activate.on-profile", "a | b");
    Binder binder = new Binder(source);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
            createTestProfiles());
    assertThat(properties.isActive(context)).isTrue();
  }

  @Test
  void isActiveAgainstBoundDataWhenProfilesDontMatch() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("app.config.activate.on-cloud-platform", "kubernetes");
    source.put("app.config.activate.on-profile", "x | z");
    Binder binder = new Binder(source);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
            createTestProfiles());
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void isActiveAgainstBoundDataWhenCloudPlatformDoesntMatch() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("app.config.activate.on-cloud-platform", "cloud-foundry");
    source.put("app.config.activate.on-profile", "a | b");
    Binder binder = new Binder(source);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    ConfigDataActivationContext context = new ConfigDataActivationContext(CloudPlatform.KUBERNETES,
            createTestProfiles());
    assertThat(properties.isActive(context)).isFalse();
  }

  @Test
  void getImportOriginWhenCommaListReturnsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("app.config.import", "one,two,three");
    Binder binder = new Binder(source);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    assertThat(properties.getImports().get(1).getOrigin())
            .hasToString("\"app.config.import\" from property source \"source\"");
  }

  @Test
  void getImportOriginWhenBracketListReturnsOrigin() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("app.config.import[0]", "one");
    source.put("app.config.import[1]", "two");
    source.put("app.config.import[2]", "three");
    Binder binder = new Binder(source);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    assertThat(properties.getImports().get(1).getOrigin())
            .hasToString("\"app.config.import[1]\" from property source \"source\"");
  }

  private Profiles createTestProfiles() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    environment.setDefaultProfiles("d", "e", "f");
    Binder binder = Binder.get(environment);
    return new Profiles(environment, binder, null);
  }

}
