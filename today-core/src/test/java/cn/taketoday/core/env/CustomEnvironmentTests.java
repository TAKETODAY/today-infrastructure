/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests covering the extensibility of {@link AbstractEnvironment}.
 *
 * @author Chris Beams
 * @since 4.0
 */
class CustomEnvironmentTests {

  @Test
  void control() {
    Environment env = new AbstractEnvironment() { };
    assertThat(env.acceptsProfiles(defaultProfile())).isTrue();
  }

  @Test
  void withNoReservedDefaultProfile() {
    class CustomEnvironment extends AbstractEnvironment {
      @Override
      protected Set<String> getReservedDefaultProfiles() {
        return Collections.emptySet();
      }
    }

    Environment env = new CustomEnvironment();
    assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
  }

  @Test
  void withSingleCustomReservedDefaultProfile() {
    class CustomEnvironment extends AbstractEnvironment {
      @Override
      protected Set<String> getReservedDefaultProfiles() {
        return Collections.singleton("rd1");
      }
    }

    Environment env = new CustomEnvironment();
    assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("rd1"))).isTrue();
  }

  @Test
  void withMultiCustomReservedDefaultProfile() {
    class CustomEnvironment extends AbstractEnvironment {
      @Override
      @SuppressWarnings("serial")
      protected Set<String> getReservedDefaultProfiles() {
        return new HashSet<String>() {{
          add("rd1");
          add("rd2");
        }};
      }
    }

    ConfigurableEnvironment env = new CustomEnvironment();
    assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("rd1 | rd2"))).isTrue();

    // finally, issue additional assertions to cover all combinations of calling these
    // methods, however unlikely.
    env.setDefaultProfiles("d1");
    assertThat(env.acceptsProfiles(Profiles.parse("rd1 | rd2"))).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("d1"))).isTrue();

    env.setActiveProfiles("a1", "a2");
    assertThat(env.acceptsProfiles(Profiles.parse("d1"))).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("a1 | a2"))).isTrue();

    env.setActiveProfiles();
    assertThat(env.acceptsProfiles(Profiles.parse("d1"))).isTrue();
    assertThat(env.acceptsProfiles(Profiles.parse("a1 | a2"))).isFalse();

    env.setDefaultProfiles();
    assertThat(env.acceptsProfiles(defaultProfile())).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("rd1 | rd2"))).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("d1"))).isFalse();
    assertThat(env.acceptsProfiles(Profiles.parse("a1 | a2"))).isFalse();
  }

  @Test
  void withNoProfileProperties() {
    ConfigurableEnvironment env = new AbstractEnvironment() {
      @Override
      @Nullable
      protected String doGetActiveProfilesProperty() {
        return null;
      }

      @Override
      @Nullable
      protected String doGetDefaultProfilesProperty() {
        return null;
      }
    };
    Map<String, Object> values = new LinkedHashMap<>();
    values.put(AbstractEnvironment.KEY_ACTIVE_PROFILES, "a,b,c");
    values.put(AbstractEnvironment.KEY_DEFAULT_PROFILES, "d,e,f");
    PropertySource<?> propertySource = new MapPropertySource("test", values);
    env.getPropertySources().addFirst(propertySource);
    assertThat(env.getActiveProfiles()).isEmpty();
    assertThat(env.getDefaultProfiles()).containsExactly(AbstractEnvironment.DEFAULT_PROFILE);
  }

  @Test
  void withCustomPropertySources() {
    class CustomPropertySources extends PropertySources { }
    PropertySources propertySources = new CustomPropertySources();
    ConfigurableEnvironment env = new AbstractEnvironment(propertySources) { };
    assertThat(env.getPropertySources()).isInstanceOf(CustomPropertySources.class);
  }

  @Test
  void withCustomPropertyResolver() {
    class CustomPropertySourcesPropertyResolver extends PropertySourcesPropertyResolver {
      public CustomPropertySourcesPropertyResolver(PropertySources propertySources) {
        super(propertySources);
      }

      @Override
      @Nullable
      public String getProperty(String key) {
        return super.getProperty(key) + "-test";
      }
    }

    ConfigurableEnvironment env = new AbstractEnvironment() {
      @Override
      protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
        return new CustomPropertySourcesPropertyResolver(propertySources);
      }
    };

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("spring", "framework");
    PropertySource<?> propertySource = new MapPropertySource("test", values);
    env.getPropertySources().addFirst(propertySource);
    assertThat(env.getProperty("spring")).isEqualTo("framework-test");
  }

  private Profiles defaultProfile() {
    return Profiles.parse(AbstractEnvironment.DEFAULT_PROFILE);
  }

}
