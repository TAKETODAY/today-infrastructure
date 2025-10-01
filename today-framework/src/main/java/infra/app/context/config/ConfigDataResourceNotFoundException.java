/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.core.io.Resource;
import infra.lang.Assert;
import infra.origin.Origin;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigDataResource} cannot be
 * found.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ConfigDataResourceNotFoundException extends ConfigDataNotFoundException {

  private final ConfigDataResource resource;

  private final @Nullable ConfigDataLocation location;

  /**
   * Create a new {@link ConfigDataResourceNotFoundException} instance.
   *
   * @param resource the resource that could not be found
   */
  public ConfigDataResourceNotFoundException(ConfigDataResource resource) {
    this(resource, null);
  }

  /**
   * Create a new {@link ConfigDataResourceNotFoundException} instance.
   *
   * @param resource the resource that could not be found
   * @param cause the exception cause
   */
  public ConfigDataResourceNotFoundException(ConfigDataResource resource, @Nullable Throwable cause) {
    this(resource, null, cause);
  }

  private ConfigDataResourceNotFoundException(ConfigDataResource resource, @Nullable ConfigDataLocation location,
          @Nullable Throwable cause) {
    super(getMessage(resource, location), cause);
    Assert.notNull(resource, "Resource is required");
    this.resource = resource;
    this.location = location;
  }

  /**
   * Return the resource that could not be found.
   *
   * @return the resource
   */
  public ConfigDataResource getResource() {
    return this.resource;
  }

  /**
   * Return the original location that was resolved to determine the resource.
   *
   * @return the location or {@code null} if no location is available
   */
  @Nullable
  public ConfigDataLocation getLocation() {
    return this.location;
  }

  @Nullable
  @Override
  public Origin getOrigin() {
    return Origin.from(this.location);
  }

  @Override
  public String getReferenceDescription() {
    return getReferenceDescription(this.resource, this.location);
  }

  /**
   * Create a new {@link ConfigDataResourceNotFoundException} instance with a location.
   *
   * @param location the location to set
   * @return a new {@link ConfigDataResourceNotFoundException} instance
   */
  ConfigDataResourceNotFoundException withLocation(ConfigDataLocation location) {
    return new ConfigDataResourceNotFoundException(this.resource, location, getCause());
  }

  private static String getMessage(ConfigDataResource resource, @Nullable ConfigDataLocation location) {
    return String.format("Config data %s cannot be found", getReferenceDescription(resource, location));
  }

  private static String getReferenceDescription(ConfigDataResource resource, @Nullable ConfigDataLocation location) {
    String description = String.format("resource '%s'", resource);
    if (location != null) {
      description += String.format(" via location '%s'", location);
    }
    return description;
  }

  /**
   * Throw a {@link ConfigDataNotFoundException} if the specified {@link Path} does not
   * exist.
   *
   * @param resource the config data resource
   * @param pathToCheck the path to check
   */
  public static void throwIfDoesNotExist(ConfigDataResource resource, Path pathToCheck) {
    throwIfDoesNotExist(resource, Files.exists(pathToCheck));
  }

  /**
   * Throw a {@link ConfigDataNotFoundException} if the specified {@link File} does not
   * exist.
   *
   * @param resource the config data resource
   * @param fileToCheck the file to check
   */
  public static void throwIfDoesNotExist(ConfigDataResource resource, File fileToCheck) {
    throwIfDoesNotExist(resource, fileToCheck.exists());
  }

  /**
   * Throw a {@link ConfigDataNotFoundException} if the specified {@link Resource} does
   * not exist.
   *
   * @param resource the config data resource
   * @param resourceToCheck the resource to check
   */
  public static void throwIfDoesNotExist(ConfigDataResource resource, Resource resourceToCheck) {
    throwIfDoesNotExist(resource, resourceToCheck.exists());
  }

  private static void throwIfDoesNotExist(ConfigDataResource resource, boolean exists) {
    if (!exists) {
      throw new ConfigDataResourceNotFoundException(resource);
    }
  }

}
