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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.origin.MockOrigin;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.Kind;
import cn.taketoday.logging.Logger;
import cn.taketoday.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InvalidConfigDataPropertyException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class InvalidConfigDataPropertyExceptionTests {

  private ConfigDataResource resource = new TestConfigDataResource();

  private ConfigurationPropertyName replacement = ConfigurationPropertyName.of("replacement");

  private ConfigurationPropertyName invalid = ConfigurationPropertyName.of("invalid");

  private ConfigurationProperty property = new ConfigurationProperty(this.invalid, "bad", MockOrigin.of("origin"));

  private Logger logger = mock(Logger.class);

  @Test
  void createHasCorrectMessage() {
    assertThat(new InvalidConfigDataPropertyException(this.property, false, this.replacement, this.resource))
            .hasMessage(
                    "Property 'invalid' imported from location 'test' is invalid and should be replaced with 'replacement' [origin: origin]");
  }

  @Test
  void createWhenNoLocationHasCorrectMessage() {
    assertThat(new InvalidConfigDataPropertyException(this.property, false, this.replacement, null))
            .hasMessage("Property 'invalid' is invalid and should be replaced with 'replacement' [origin: origin]");
  }

  @Test
  void createWhenNoReplacementHasCorrectMessage() {
    assertThat(new InvalidConfigDataPropertyException(this.property, false, null, this.resource))
            .hasMessage("Property 'invalid' imported from location 'test' is invalid [origin: origin]");
  }

  @Test
  void createWhenNoOriginHasCorrectMessage() {
    ConfigurationProperty property = new ConfigurationProperty(this.invalid, "bad", null);
    assertThat(new InvalidConfigDataPropertyException(property, false, this.replacement, this.resource)).hasMessage(
            "Property 'invalid' imported from location 'test' is invalid and should be replaced with 'replacement'");
  }

  @Test
  void createWhenProfileSpecificHasCorrectMessage() {
    ConfigurationProperty property = new ConfigurationProperty(this.invalid, "bad", null);
    assertThat(new InvalidConfigDataPropertyException(property, true, null, this.resource)).hasMessage(
            "Property 'invalid' imported from location 'test' is invalid in a profile specific resource");
  }

  @Test
  void getPropertyReturnsProperty() {
    InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property, false,
            this.replacement, this.resource);
    assertThat(exception.getProperty()).isEqualTo(this.property);
  }

  @Test
  void getLocationReturnsLocation() {
    InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property, false,
            this.replacement, this.resource);
    assertThat(exception.getLocation()).isEqualTo(this.resource);
  }

  @Test
  void getReplacementReturnsReplacement() {
    InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property, false,
            this.replacement, this.resource);
    assertThat(exception.getReplacement()).isEqualTo(this.replacement);
  }

  @Test
  @Disabled("Disabled until context.profiles support is dropped")
  void throwOrWarnWhenHasInvalidPropertyThrowsException() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("context.profiles", "a");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
            .isThrownBy(() -> InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor))
            .withMessageStartingWith("Property 'context.profiles' is invalid and should be replaced with "
                    + "'context.config.activate.on-profile'");
  }

  @Test
  void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException() {
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("context.profiles.include");
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("context.profiles.active");
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("context.profiles.default");
  }

  @Test
  void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyOnIgnoringProfilesContributorDoesNotThrowException() {
    ConfigDataEnvironmentContributor contributor = createInvalidProfileSpecificPropertyContributor(
            "context.profiles.active", ConfigData.Option.IGNORE_PROFILES);
    assertThatNoException()
            .isThrownBy(() -> InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor));
  }

  private void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException(String name) {
    ConfigDataEnvironmentContributor contributor = createInvalidProfileSpecificPropertyContributor(name);
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
            .isThrownBy(() -> InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor))
            .withMessageStartingWith("Property '" + name + "' is invalid in a profile specific resource");
  }

  private ConfigDataEnvironmentContributor createInvalidProfileSpecificPropertyContributor(String name,
          ConfigData.Option... configDataOptions) {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty(name, "a");
    ConfigDataEnvironmentContributor contributor = new ConfigDataEnvironmentContributor(Kind.BOUND_IMPORT, null,
            null, true, propertySource, ConfigurationPropertySource.from(propertySource), null,
            ConfigData.Options.of(configDataOptions), null);
    return contributor;
  }

  @Test
  void throwOrWarnWhenHasNoInvalidPropertyDoesNothing() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
            .ofExisting(new MockPropertySource());
    InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
  }

  @Test
  void throwOrWarnWhenHasWarningPropertyLogsWarning() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("context.profiles", "a");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
    InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
    then(this.logger).should().warn("Property 'context.profiles' is invalid and should be replaced with "
            + "'context.config.activate.on-profile' [origin: \"context.profiles\" from property source \"mockProperties\"]");
  }

  @Test
  void throwOrWarnWhenHasWarningPropertyWithListSyntaxLogsWarning() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("context.profiles[0]", "a");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
    InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
    then(this.logger).should().warn("Property 'context.profiles[0]' is invalid and should be replaced with "
            + "'context.config.activate.on-profile' [origin: \"context.profiles[0]\" from property source \"mockProperties\"]");
  }

  private static class TestConfigDataResource extends ConfigDataResource {

    @Override
    public String toString() {
      return "test";
    }

  }

}
