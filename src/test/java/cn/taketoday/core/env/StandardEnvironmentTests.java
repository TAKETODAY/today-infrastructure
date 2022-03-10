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

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import cn.taketoday.lang.TodayStrategies;

import static cn.taketoday.core.env.AbstractEnvironment.DEFAULT_PROFILE;
import static cn.taketoday.core.env.AbstractEnvironment.KEY_ACTIVE_PROFILES;
import static cn.taketoday.core.env.AbstractEnvironment.KEY_DEFAULT_PROFILES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link StandardEnvironment}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class StandardEnvironmentTests {

  private static final String ALLOWED_PROPERTY_NAME = "theanswer";
  private static final String ALLOWED_PROPERTY_VALUE = "42";

  private static final String DISALLOWED_PROPERTY_NAME = "verboten";
  private static final String DISALLOWED_PROPERTY_VALUE = "secret";

  private static final String STRING_PROPERTY_NAME = "stringPropName";
  private static final String STRING_PROPERTY_VALUE = "stringPropValue";
  private static final Object NON_STRING_PROPERTY_NAME = new Object();
  private static final Object NON_STRING_PROPERTY_VALUE = new Object();

  private final ConfigurableEnvironment environment = new StandardEnvironment();

  @Test
  void merge() {
    ConfigurableEnvironment child = new StandardEnvironment();
    child.setActiveProfiles("c1", "c2");
    child.getPropertySources().addLast(
            new MockPropertySource("childMock")
                    .withProperty("childKey", "childVal")
                    .withProperty("bothKey", "childBothVal"));

    ConfigurableEnvironment parent = new StandardEnvironment();
    parent.setActiveProfiles("p1", "p2");
    parent.getPropertySources().addLast(
            new MockPropertySource("parentMock")
                    .withProperty("parentKey", "parentVal")
                    .withProperty("bothKey", "parentBothVal"));

    assertThat(child.getProperty("childKey")).isEqualTo("childVal");
    assertThat(child.getProperty("parentKey")).isNull();
    assertThat(child.getProperty("bothKey")).isEqualTo("childBothVal");

    assertThat(parent.getProperty("childKey")).isNull();
    assertThat(parent.getProperty("parentKey")).isEqualTo("parentVal");
    assertThat(parent.getProperty("bothKey")).isEqualTo("parentBothVal");

    assertThat(child.getActiveProfiles()).isEqualTo(new String[] { "c1", "c2" });
    assertThat(parent.getActiveProfiles()).isEqualTo(new String[] { "p1", "p2" });

    child.merge(parent);

    assertThat(child.getProperty("childKey")).isEqualTo("childVal");
    assertThat(child.getProperty("parentKey")).isEqualTo("parentVal");
    assertThat(child.getProperty("bothKey")).isEqualTo("childBothVal");

    assertThat(parent.getProperty("childKey")).isNull();
    assertThat(parent.getProperty("parentKey")).isEqualTo("parentVal");
    assertThat(parent.getProperty("bothKey")).isEqualTo("parentBothVal");

    assertThat(child.getActiveProfiles()).isEqualTo(new String[] { "c1", "c2", "p1", "p2" });
    assertThat(parent.getActiveProfiles()).isEqualTo(new String[] { "p1", "p2" });
  }

  @Test
  void propertySourceOrder() {
    ConfigurableEnvironment env = new StandardEnvironment();
    PropertySources sources = env.getPropertySources();
    assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))).isEqualTo(1);
    assertThat(sources).hasSize(2);
  }

  @Test
  void propertySourceTypes() {
    ConfigurableEnvironment env = new StandardEnvironment();
    PropertySources sources = env.getPropertySources();
    assertThat(sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)).isInstanceOf(SystemEnvironmentPropertySource.class);
  }

  @Test
  void activeProfilesIsEmptyByDefault() {
    assertThat(environment.getActiveProfiles().length).isEqualTo(0);
  }

  @Test
  void defaultProfilesContainsDefaultProfileByDefault() {
    assertThat(environment.getDefaultProfiles()).hasSize(1);
    assertThat(environment.getDefaultProfiles()[0]).isEqualTo("default");
  }

  @Test
  void setActiveProfiles() {
    environment.setActiveProfiles("local", "embedded");
    String[] activeProfiles = environment.getActiveProfiles();
    assertThat(activeProfiles).contains("local", "embedded");
    assertThat(activeProfiles).hasSize(2);
  }

  @Test
  void setActiveProfiles_withNullProfileArray() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles((String[]) null));
  }

  @Test
  void setActiveProfiles_withNullProfile() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles((String) null));
  }

  @Test
  void setActiveProfiles_withEmptyProfile() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles(""));
  }

  @Test
  void setActiveProfiles_withNotOperator() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setActiveProfiles("p1", "!p2"));
  }

  @Test
  void setDefaultProfiles_withNullProfileArray() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles((String[]) null));
  }

  @Test
  void setDefaultProfiles_withNullProfile() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles((String) null));
  }

  @Test
  void setDefaultProfiles_withEmptyProfile() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles(""));
  }

  @Test
  void setDefaultProfiles_withNotOperator() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.setDefaultProfiles("d1", "!d2"));
  }

  @Test
  void addActiveProfile() {
    assertThat(environment.getActiveProfiles().length).isZero();
    environment.setActiveProfiles("local", "embedded");
    assertThat(environment.getActiveProfiles()).contains("local", "embedded");
    assertThat(environment.getActiveProfiles()).hasSize(2);
    environment.addActiveProfile("p1");
    assertThat(environment.getActiveProfiles()).contains("p1");
    assertThat(environment.getActiveProfiles()).hasSize(3);
    environment.addActiveProfile("p2");
    environment.addActiveProfile("p3");
    assertThat(environment.getActiveProfiles()).contains("p2", "p3");
    assertThat(environment.getActiveProfiles()).hasSize(5);
  }

  @Test
  void addActiveProfile_whenActiveProfilesPropertyIsAlreadySet() {
    ConfigurableEnvironment env = new StandardEnvironment();
    assertThat(env.getProperty(KEY_ACTIVE_PROFILES)).isNull();
    env.getPropertySources().addFirst(new MockPropertySource().withProperty(KEY_ACTIVE_PROFILES, "p1"));
    assertThat(env.getProperty(KEY_ACTIVE_PROFILES)).isEqualTo("p1");
    env.addActiveProfile("p2");
    assertThat(env.getActiveProfiles()).contains("p1", "p2");
  }

  @Test
  void reservedDefaultProfile() {
    assertThat(environment.getDefaultProfiles()).isEqualTo(new String[] { DEFAULT_PROFILE });
    System.setProperty(KEY_DEFAULT_PROFILES, "d0");
    assertThat(environment.getDefaultProfiles()).isEqualTo(new String[] { "d0" });
    environment.setDefaultProfiles("d1", "d2");
    assertThat(environment.getDefaultProfiles()).isEqualTo(new String[] { "d1", "d2" });
    System.clearProperty(KEY_DEFAULT_PROFILES);
  }

  @Test
  void defaultProfileWithCircularPlaceholder() {
    try {
      System.setProperty(KEY_DEFAULT_PROFILES, "${context.profiles.default}");
      assertThatIllegalArgumentException().isThrownBy(environment::getDefaultProfiles);
    }
    finally {
      System.clearProperty(KEY_DEFAULT_PROFILES);
    }
  }

  @Test
  void getActiveProfiles_systemPropertiesEmpty() {
    assertThat(environment.getActiveProfiles().length).isZero();
    System.setProperty(KEY_ACTIVE_PROFILES, "");
    assertThat(environment.getActiveProfiles().length).isZero();
    System.clearProperty(KEY_ACTIVE_PROFILES);
  }

  @Test
  void getActiveProfiles_fromSystemProperties() {
    System.setProperty(KEY_ACTIVE_PROFILES, "foo");
    assertThat(Arrays.asList(environment.getActiveProfiles())).contains("foo");
    System.clearProperty(KEY_ACTIVE_PROFILES);
  }

  @Test
  void getActiveProfiles_fromSystemProperties_withMultipleProfiles() {
    System.setProperty(KEY_ACTIVE_PROFILES, "foo,bar");
    assertThat(environment.getActiveProfiles()).contains("foo", "bar");
    System.clearProperty(KEY_ACTIVE_PROFILES);
  }

  @Test
  void getActiveProfiles_fromSystemProperties_withMulitpleProfiles_withWhitespace() {
    System.setProperty(KEY_ACTIVE_PROFILES, " bar , baz "); // notice whitespace
    assertThat(environment.getActiveProfiles()).contains("bar", "baz");
    System.clearProperty(KEY_ACTIVE_PROFILES);
  }

  @Test
  void getDefaultProfiles() {
    assertThat(environment.getDefaultProfiles()).isEqualTo(new String[] { DEFAULT_PROFILE });
    environment.getPropertySources().addFirst(new MockPropertySource().withProperty(KEY_DEFAULT_PROFILES, "pd1"));
    assertThat(environment.getDefaultProfiles()).hasSize(1);
    assertThat(Arrays.asList(environment.getDefaultProfiles())).contains("pd1");
  }

  @Test
  void setDefaultProfiles() {
    environment.setDefaultProfiles();
    assertThat(environment.getDefaultProfiles().length).isZero();
    environment.setDefaultProfiles("pd1");
    assertThat(Arrays.asList(environment.getDefaultProfiles())).contains("pd1");
    environment.setDefaultProfiles("pd2", "pd3");
    assertThat(environment.getDefaultProfiles()).doesNotContain("pd1");
    assertThat(environment.getDefaultProfiles()).contains("pd2", "pd3");
  }

  @Test
  void acceptsProfiles_withEmptyArgumentList() {
    assertThatIllegalArgumentException().isThrownBy(
            environment::acceptsProfiles);
  }

  @Test
  void acceptsProfiles_withNullArgumentList() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles((String[]) null));
  }

  @Test
  void acceptsProfiles_withNullArgument() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles((String) null));
  }

  @Test
  void acceptsProfiles_withEmptyArgument() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles(""));
  }

  @Test
  void acceptsProfiles_activeProfileSetProgrammatically() {
    assertThat(environment.acceptsProfiles("p1", "p2")).isFalse();
    environment.setActiveProfiles("p1");
    assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
    environment.setActiveProfiles("p2");
    assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
    environment.setActiveProfiles("p1", "p2");
    assertThat(environment.acceptsProfiles("p1", "p2")).isTrue();
  }

  @Test
  void acceptsProfiles_activeProfileSetViaProperty() {
    assertThat(environment.acceptsProfiles("p1")).isFalse();
    environment.getPropertySources().addFirst(new MockPropertySource().withProperty(KEY_ACTIVE_PROFILES, "p1"));
    assertThat(environment.acceptsProfiles("p1")).isTrue();
  }

  @Test
  void acceptsProfiles_defaultProfile() {
    assertThat(environment.acceptsProfiles("pd")).isFalse();
    environment.setDefaultProfiles("pd");
    assertThat(environment.acceptsProfiles("pd")).isTrue();
    environment.setActiveProfiles("p1");
    assertThat(environment.acceptsProfiles("pd")).isFalse();
    assertThat(environment.acceptsProfiles("p1")).isTrue();
  }

  @Test
  void acceptsProfiles_withNotOperator() {
    assertThat(environment.acceptsProfiles("p1")).isFalse();
    assertThat(environment.acceptsProfiles("!p1")).isTrue();
    environment.addActiveProfile("p1");
    assertThat(environment.acceptsProfiles("p1")).isTrue();
    assertThat(environment.acceptsProfiles("!p1")).isFalse();
  }

  @Test
  void acceptsProfiles_withInvalidNotOperator() {
    assertThatIllegalArgumentException().isThrownBy(() -> environment.acceptsProfiles("p1", "!"));
  }

  @Test
  void acceptsProfiles_withProfileExpression() {
    assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isFalse();
    environment.addActiveProfile("p1");
    assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isFalse();
    environment.addActiveProfile("p2");
    assertThat(environment.acceptsProfiles(Profiles.of("p1 & p2"))).isTrue();
  }

  @Test
  void environmentSubclass_withCustomProfileValidation() {
    ConfigurableEnvironment env = new AbstractEnvironment() {
      @Override
      protected void validateProfile(String profile) {
        super.validateProfile(profile);
        if (profile.contains("-")) {
          throw new IllegalArgumentException(
                  "Invalid profile [" + profile + "]: must not contain dash character");
        }
      }
    };

    env.addActiveProfile("validProfile"); // succeeds

    assertThatIllegalArgumentException().isThrownBy(() ->
                    env.addActiveProfile("invalid-profile"))
            .withMessage("Invalid profile [invalid-profile]: must not contain dash character");
  }

  @Test
  void suppressGetenvAccessThroughSystemProperty() {
    System.setProperty("context.getenv.ignore", "true");
    assertThat(environment.getSystemEnvironment()).isEmpty();
    System.clearProperty("context.getenv.ignore");
  }

  @Test
  void suppressGetenvAccessThroughSpringProperty() {
    TodayStrategies.setProperty("context.getenv.ignore", "true");
    assertThat(environment.getSystemEnvironment()).isEmpty();
    TodayStrategies.setProperty("context.getenv.ignore", null);
  }

  @Test
  void suppressGetenvAccessThroughSpringFlag() {
    TodayStrategies.setFlag("context.getenv.ignore");
    assertThat(environment.getSystemEnvironment()).isEmpty();
    TodayStrategies.setProperty("context.getenv.ignore", null);
  }

  @Test
  void getSystemProperties() {
    System.setProperty(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
    System.setProperty(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);
    System.getProperties().put(STRING_PROPERTY_NAME, NON_STRING_PROPERTY_VALUE);
    System.getProperties().put(NON_STRING_PROPERTY_NAME, STRING_PROPERTY_VALUE);

    try {
      Map<?, ?> systemProperties = environment.getSystemProperties();
      assertThat(systemProperties).isNotNull();
      assertThat(System.getProperties()).isSameAs(systemProperties);
      assertThat(systemProperties.get(ALLOWED_PROPERTY_NAME)).isEqualTo(ALLOWED_PROPERTY_VALUE);
      assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME)).isEqualTo(DISALLOWED_PROPERTY_VALUE);
      assertThat(systemProperties.get(STRING_PROPERTY_NAME)).isEqualTo(NON_STRING_PROPERTY_VALUE);
      assertThat(systemProperties.get(NON_STRING_PROPERTY_NAME)).isEqualTo(STRING_PROPERTY_VALUE);
    }
    finally {
      System.clearProperty(ALLOWED_PROPERTY_NAME);
      System.clearProperty(DISALLOWED_PROPERTY_NAME);
      System.getProperties().remove(STRING_PROPERTY_NAME);
      System.getProperties().remove(NON_STRING_PROPERTY_NAME);
    }
  }

  @Test
  void getSystemEnvironment() {
    Map<String, Object> systemEnvironment = environment.getSystemEnvironment();
    assertThat(systemEnvironment).isNotNull();
    assertThat(System.getenv()).isSameAs(systemEnvironment);
  }

}
