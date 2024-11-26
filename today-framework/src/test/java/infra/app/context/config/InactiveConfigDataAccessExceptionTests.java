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

import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.mock.env.MockPropertySource;
import infra.origin.Origin;
import infra.origin.PropertySourceOrigin;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InactiveConfigDataAccessException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class InactiveConfigDataAccessExceptionTests {

  private MockPropertySource propertySource = new MockPropertySource();

  private ConfigDataResource resource = new TestConfigDataResource();

  private String propertyName = "spring";

  private Origin origin = new PropertySourceOrigin(this.propertySource, this.propertyName);

  @Test
  void createHasCorrectMessage() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, this.origin);
    assertThat(exception).hasMessage("Inactive property source 'mockProperties' imported from location 'test' "
            + "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
  }

  @Test
  void createWhenNoLocationHasCorrectMessage() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource, null,
            this.propertyName, this.origin);
    assertThat(exception).hasMessage("Inactive property source 'mockProperties' "
            + "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
  }

  @Test
  void createWhenNoOriginHasCorrectMessage() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, null);
    assertThat(exception).hasMessage("Inactive property source 'mockProperties' imported from location 'test' "
            + "cannot contain property 'spring'");
  }

  @Test
  void getPropertySourceReturnsPropertySource() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, this.origin);
    assertThat(exception.getPropertySource()).isSameAs(this.propertySource);
  }

  @Test
  void getLocationReturnsLocation() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, this.origin);
    assertThat(exception.getLocation()).isSameAs(this.resource);
  }

  @Test
  void getPropertyNameReturnsPropertyName() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, this.origin);
    assertThat(exception.getPropertyName()).isSameAs(this.propertyName);
  }

  @Test
  void getOriginReturnsOrigin() {
    InactiveConfigDataAccessException exception = new InactiveConfigDataAccessException(this.propertySource,
            this.resource, this.propertyName, this.origin);
    assertThat(exception.getOrigin()).isSameAs(this.origin);
  }

  @Test
  void throwIfPropertyFoundWhenSourceIsNullDoesNothing() {
    ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
    ReflectionTestUtils.setField(contributor, "configurationPropertySource", null);
    InactiveConfigDataAccessException.throwIfPropertyFound(contributor, ConfigurationPropertyName.of("spring"));
  }

  @Test
  void throwIfPropertyFoundWhenPropertyNotFoundDoesNothing() {
    ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
    ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(this.propertySource);
    ReflectionTestUtils.setField(contributor, "configurationPropertySource", configurationPropertySource);
    InactiveConfigDataAccessException.throwIfPropertyFound(contributor, ConfigurationPropertyName.of("spring"));
  }

  @Test
  void throwIfPropertyFoundWhenPropertyFoundThrowsException() {
    this.propertySource.setProperty("spring", "test");
    ConfigDataEnvironmentContributor contributor = mock(ConfigDataEnvironmentContributor.class);
    ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(this.propertySource);
    ReflectionTestUtils.setField(contributor, "configurationPropertySource", configurationPropertySource);
    ReflectionTestUtils.setField(contributor, "propertySource", this.propertySource);
    ReflectionTestUtils.setField(contributor, "resource", this.resource);

    assertThatExceptionOfType(InactiveConfigDataAccessException.class)
            .isThrownBy(() -> InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
                    ConfigurationPropertyName.of("spring")))
            .withMessage("Inactive property source 'mockProperties' imported from location 'test' "
                    + "cannot contain property 'spring' [origin: \"spring\" from property source \"mockProperties\"]");
  }

  private static class TestConfigDataResource extends ConfigDataResource {

    @Override
    public String toString() {
      return "test";
    }

  }

}
