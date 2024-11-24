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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import infra.app.context.config.ConfigData;
import infra.app.context.config.ConfigDataEnvironmentContributor;
import infra.app.context.config.ConfigDataResource;
import infra.app.context.config.InvalidConfigDataPropertyException;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.testfixture.origin.MockOrigin;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.app.context.config.ConfigDataEnvironmentContributor.Kind;
import infra.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

  private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

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
  void throwOrWarnWhenHasInvalidPropertyThrowsException() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("infra.profiles", "a");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource, conversionService);
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
            .isThrownBy(() -> InvalidConfigDataPropertyException.throwIfPropertyFound(contributor))
            .withMessageStartingWith("Property 'infra.profiles' is invalid and should be replaced with "
                    + "'app.config.activate.on-profile'");
  }

  @Test
  void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException() {
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("infra.profiles.include");
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("infra.profiles.active");
    throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException("infra.profiles.default");
  }

  @Test
  void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyOnIgnoringProfilesContributorDoesNotThrowException() {
    ConfigDataEnvironmentContributor contributor = createInvalidProfileSpecificPropertyContributor(
            "infra.profiles.active", ConfigData.Option.IGNORE_PROFILES);
    assertThatNoException().isThrownBy(() -> InvalidConfigDataPropertyException.throwIfPropertyFound(contributor));
  }

  private void throwOrWarnWhenWhenHasInvalidProfileSpecificPropertyThrowsException(String name) {
    ConfigDataEnvironmentContributor contributor = createInvalidProfileSpecificPropertyContributor(name);
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
            .isThrownBy(() -> InvalidConfigDataPropertyException.throwIfPropertyFound(contributor))
            .withMessageStartingWith("Property '" + name + "' is invalid in a profile specific resource");
  }

  private ConfigDataEnvironmentContributor createInvalidProfileSpecificPropertyContributor(String name,
          ConfigData.Option... configDataOptions) {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty(name, "a");
    ConfigDataEnvironmentContributor contributor = new ConfigDataEnvironmentContributor(Kind.BOUND_IMPORT, null,
            null, true, propertySource, ConfigurationPropertySource.from(propertySource), null,
            ConfigData.Options.of(configDataOptions), null, conversionService);
    return contributor;
  }

  @Test
  void throwOrWarnWhenHasNoInvalidPropertyDoesNothing() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
            .ofExisting(new MockPropertySource(), conversionService);
    InvalidConfigDataPropertyException.throwIfPropertyFound(contributor);
  }

  private static class TestConfigDataResource extends ConfigDataResource {

    @Override
    public String toString() {
      return "test";
    }

  }

}
