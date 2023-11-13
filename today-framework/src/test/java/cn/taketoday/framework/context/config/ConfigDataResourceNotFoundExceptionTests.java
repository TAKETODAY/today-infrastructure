/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigDataResourceNotFoundException}.
 *
 * @author Phillip Webb
 */
class ConfigDataResourceNotFoundExceptionTests {

  private ConfigDataResource resource = new TestConfigDataResource();

  private ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test");

  private Throwable cause = new RuntimeException();

  private File exists;

  private File missing;

  @TempDir
  File temp;

  @BeforeEach
  void setup() throws IOException {
    this.exists = new File(this.temp, "exists");
    this.missing = new File(this.temp, "missing");
    try (OutputStream out = new FileOutputStream(this.exists)) {
      out.write("test".getBytes());
    }
  }

  @Test
  void createWhenResourceIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigDataResourceNotFoundException(null))
            .withMessage("Resource is required");
  }

  @Test
  void createWithResourceCreatesInstance() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
    assertThat(exception.getResource()).isSameAs(this.resource);
  }

  @Test
  void createWithResourceAndCauseCreatesInstance() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource,
            this.cause);
    assertThat(exception.getResource()).isSameAs(this.resource);
    assertThat(exception.getCause()).isSameAs(this.cause);
  }

  @Test
  void getResourceReturnsResource() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
    assertThat(exception.getResource()).isSameAs(this.resource);
  }

  @Test
  void getLocationWhenHasNoLocationReturnsNull() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
    assertThat(exception.getLocation()).isNull();
  }

  @Test
  void getLocationWhenHasLocationReturnsLocation() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
            .withLocation(this.location);
    assertThat(exception.getLocation()).isSameAs(this.location);
  }

  @Test
  void getReferenceDescriptionWhenHasNoLocationReturnsDescription() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
    assertThat(exception.getReferenceDescription()).isEqualTo("resource 'mytestresource'");
  }

  @Test
  void getReferenceDescriptionWhenHasLocationReturnsDescription() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
            .withLocation(this.location);
    assertThat(exception.getReferenceDescription())
            .isEqualTo("resource 'mytestresource' via location 'optional:test'");
  }

  @Test
  void withLocationReturnsNewInstanceWithLocation() {
    ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
            .withLocation(this.location);
    assertThat(exception.getLocation()).isSameAs(this.location);
  }

  @Test
  void throwIfDoesNotExistWhenPathExistsDoesNothing() {
    ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.exists.toPath());
  }

  @Test
  void throwIfDoesNotExistWhenPathDoesNotExistThrowsException() {
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(
            () -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.missing.toPath()));
  }

  @Test
  void throwIfDoesNotExistWhenFileExistsDoesNothing() {
    ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.exists);

  }

  @Test
  void throwIfDoesNotExistWhenFileDoesNotExistThrowsException() {
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
            .isThrownBy(() -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.missing));
  }

  @Test
  void throwIfDoesNotExistWhenResourceExistsDoesNothing() {
    ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, new FileSystemResource(this.exists));
  }

  @Test
  void throwIfDoesNotExistWhenResourceDoesNotExistThrowsException() {
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
            .isThrownBy(() -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource,
                    new FileSystemResource(this.missing)));
  }

  static class TestConfigDataResource extends ConfigDataResource {

    @Override
    public String toString() {
      return "mytestresource";
    }

  }

}
