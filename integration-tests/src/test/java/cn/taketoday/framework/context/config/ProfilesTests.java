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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Profiles}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ProfilesTests {

  @Test
  void getActiveWhenNoEnvironmentProfilesAndNoPropertyReturnsEmptyArray() {
    Environment environment = new MockEnvironment();
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).isEmpty();
  }

  @Test
  void getActiveWhenNoEnvironmentProfilesAndBinderProperty() {
    Environment environment = new MockEnvironment();
    Binder binder = new Binder(
            new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.active", "a,b,c")));
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c");
  }

  @Test
  void getActiveWhenNoEnvironmentProfilesAndEnvironmentProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c");
  }

  @Test
  void getActiveWhenEnvironmentProfilesAndBinderProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    Binder binder = new Binder(
            new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.active", "d,e,f")));
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
  }

  @Test
  void getActiveWhenEnvironmentProfilesAndBinderPropertyShouldReturnEnvironmentProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    List<ConfigurationPropertySource> sources = new ArrayList<>();
    ConfigurationPropertySources.get(environment).forEach(sources::add);
    sources.add(new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.active", "d,e,f")));
    Binder binder = new Binder(sources);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c");
  }

  @Test
  void getActiveWhenEnvironmentProfilesAndEnvironmentProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    environment.setProperty("context.profiles.active", "d,e,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
  }

  @Test
  void getActiveWhenNoEnvironmentProfilesAndEnvironmentPropertyInBindNotation() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active[0]", "a");
    environment.setProperty("context.profiles.active[1]", "b");
    environment.setProperty("context.profiles.active[2]", "c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c");
  }

  @Test
  void getActiveWhenEnvironmentProfilesInBindNotationAndEnvironmentPropertyReturnsEnvironmentProfiles() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    environment.setProperty("context.profiles.active[0]", "d");
    environment.setProperty("context.profiles.active[1]", "e");
    environment.setProperty("context.profiles.active[2]", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
  }

  @Test
  void getActiveWhenHasDuplicatesReturnsUniqueElements() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,a,b,c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "b", "c");
  }

  @Test
  void getActiveWithProfileGroups() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "d,e");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getActive()).containsExactly("a", "d", "e", "b", "c");
  }

  @Test
  void getActiveWhenHasAdditionalIncludesAdditional() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "d,e,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, Arrays.asList("a", "b", "c"));
    assertThat(profiles.getActive()).containsExactly("a", "b", "c", "d", "e", "f");
  }

  @Test
  void getDefaultWhenNoEnvironmentProfilesAndNoPropertyReturnsEmptyArray() {
    Environment environment = new MockEnvironment();
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("default");
  }

  @Test
  void getDefaultWhenNoEnvironmentProfilesAndBinderProperty() {
    Environment environment = new MockEnvironment();
    Binder binder = new Binder(
            new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.default", "a,b,c")));
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWhenDefaultEnvironmentProfileAndBinderProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.default", "default");
    List<ConfigurationPropertySource> sources = new ArrayList<>();
    ConfigurationPropertySources.get(environment).forEach(sources::add);
    sources.add(new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.default", "a,b,c")));
    Binder binder = new Binder(sources);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("default");
  }

  @Test
  void getDefaultWhenNoEnvironmentProfilesAndEnvironmentProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.default", "a,b,c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWhenEnvironmentProfilesAndBinderProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("a", "b", "c");
    Binder binder = new Binder(
            new MapConfigurationPropertySource(Collections.singletonMap("context.profiles.default", "d,e,f")));
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWhenEnvironmentProfilesAndEnvironmentProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("a", "b", "c");
    environment.setProperty("context.profiles.default", "d,e,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWhenNoEnvironmentProfilesAndEnvironmentPropertyInBindNotation() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.default[0]", "a");
    environment.setProperty("context.profiles.default[1]", "b");
    environment.setProperty("context.profiles.default[2]", "c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWhenHasDuplicatesReturnsUniqueElements() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.default", "a,b,a,b,c");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void getDefaultWithProfileGroups() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.default", "a,b,c");
    environment.setProperty("context.profiles.group.a", "d,e");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "d", "e", "b", "c");
  }

  @Test
  void getDefaultWhenEnvironmentProfilesInBindNotationAndEnvironmentPropertyReturnsBoth() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("a", "b", "c");
    environment.setProperty("context.profiles.default[0]", "d");
    environment.setProperty("context.profiles.default[1]", "e");
    environment.setProperty("context.profiles.default[2]", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getDefault()).containsExactly("a", "b", "c");
  }

  @Test
  void iteratorIteratesAllActiveProfiles() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    environment.setDefaultProfiles("d", "e", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles).containsExactly("a", "b", "c");
  }

  @Test
  void iteratorIteratesAllDefaultProfilesWhenNoActive() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("d", "e", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles).containsExactly("d", "e", "f");
  }

  @Test
  void isActiveWhenActiveContainsProfileReturnsTrue() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles.isAccepted("a")).isTrue();
  }

  @Test
  void isActiveWhenActiveDoesNotContainProfileReturnsFalse() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles.isAccepted("x")).isFalse();
  }

  @Test
  void isActiveWhenNoActiveAndDefaultContainsProfileReturnsTrue() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("d", "e", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles.isAccepted("d")).isTrue();
  }

  @Test
  void isActiveWhenNoActiveAndDefaultDoesNotContainProfileReturnsFalse() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("d", "e", "f");
    Binder binder = Binder.get(environment);
    Profiles profiles1 = new Profiles(environment, binder, null);
    Profiles profiles = profiles1;
    assertThat(profiles.isAccepted("x")).isFalse();
  }

  @Test
  void iteratorWithProfileGroups() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "e,f");
    environment.setProperty("context.profiles.group.e", "x,y");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles).containsExactly("a", "e", "x", "y", "f", "b", "c");
  }

  @Test
  void iteratorWithProfileGroupsAndNoActive() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.group.a", "e,f");
    environment.setProperty("context.profiles.group.e", "x,y");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles).containsExactly("default");
  }

  @Test
  void iteratorWithProfileGroupsForDefault() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.group.default", "e,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles).containsExactly("default", "e", "f");
  }

  @Test
  void getAcceptedWithProfileGroups() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "e,f");
    environment.setProperty("context.profiles.group.e", "x,y");
    environment.setDefaultProfiles("g", "h", "i");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getAccepted()).containsExactly("a", "e", "x", "y", "f", "b", "c");
  }

  @Test
  void getAcceptedWhenNoActiveAndDefaultWithGroups() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("d", "e", "f");
    environment.setProperty("context.profiles.group.e", "x,y");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getAccepted()).containsExactly("d", "e", "x", "y", "f");
  }

  @Test
  void isAcceptedWithGroupsReturnsTrue() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "e,f");
    environment.setProperty("context.profiles.group.e", "x,y");
    environment.setDefaultProfiles("g", "h", "i");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.isAccepted("a")).isTrue();
    assertThat(profiles.isAccepted("e")).isTrue();
    assertThat(profiles.isAccepted("g")).isFalse();
  }

  @Test
  void isAcceptedWhenNoActiveAndDefaultWithGroupsContainsProfileReturnsTrue() {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("d", "e", "f");
    environment.setProperty("context.profiles.group.e", "x,y");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.isAccepted("d")).isTrue();
    assertThat(profiles.isAccepted("x")).isTrue();
  }

  @Test
  void simpleRecursiveReferenceInProfileGroupIgnoresDuplicates() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "a,e,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getAccepted()).containsExactly("a", "e", "f", "b", "c");
  }

  @Test
  void multipleRecursiveReferenceInProfileGroupIgnoresDuplicates() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "a,b,f");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getAccepted()).containsExactly("a", "b", "f", "c");
  }

  @Test
  void complexRecursiveReferenceInProfileGroupIgnoresDuplicates() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("context.profiles.active", "a,b,c");
    environment.setProperty("context.profiles.group.a", "e,f,g");
    environment.setProperty("context.profiles.group.e", "a,x,y,g");
    Binder binder = Binder.get(environment);
    Profiles profiles = new Profiles(environment, binder, null);
    assertThat(profiles.getAccepted()).containsExactly("a", "e", "x", "y", "g", "f", "b", "c");
  }

}
