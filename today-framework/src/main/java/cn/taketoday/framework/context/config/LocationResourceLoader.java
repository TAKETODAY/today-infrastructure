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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Strategy interface for loading resources from a location. Supports single resource and
 * simple wildcard directory patterns.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class LocationResourceLoader {

  private static final Resource[] EMPTY_RESOURCES = {};

  private static final Comparator<File> FILE_PATH_COMPARATOR = Comparator.comparing(File::getAbsolutePath);

  private static final Comparator<File> FILE_NAME_COMPARATOR = Comparator.comparing(File::getName);

  private final ResourceLoader resourceLoader;

  /**
   * Create a new {@link LocationResourceLoader} instance.
   *
   * @param resourceLoader the underlying resource loader
   */
  LocationResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Returns if the location contains a pattern.
   *
   * @param location the location to check
   * @return if the location is a pattern
   */
  boolean isPattern(@Nullable String location) {
    return StringUtils.isNotEmpty(location) && location.contains("*");
  }

  /**
   * Get a single resource from a non-pattern location.
   *
   * @param location the location
   * @return the resource
   * @see #isPattern(String)
   */
  Resource getResource(@Nullable String location) {
    validateNonPattern(location);
    location = StringUtils.cleanPath(location);
    if (!ResourceUtils.isUrl(location)) {
      location = ResourceUtils.FILE_URL_PREFIX + location;
    }
    return this.resourceLoader.getResource(location);
  }

  private void validateNonPattern(@Nullable String location) {
    Assert.state(!isPattern(location), () -> String.format("Location '%s' must not be a pattern", location));
  }

  /**
   * Get a multiple resources from a location pattern.
   *
   * @param location the location pattern
   * @param type the type of resource to return
   * @return the resources
   * @see #isPattern(String)
   */
  Resource[] getResources(@Nullable String location, ResourceType type) {
    validatePattern(location, type);
    String directoryPath = location.substring(0, location.indexOf("*/"));
    String fileName = location.substring(location.lastIndexOf("/") + 1);
    Resource resource = getResource(directoryPath);
    if (!resource.exists()) {
      return EMPTY_RESOURCES;
    }
    File file = getFile(location, resource);
    if (!file.isDirectory()) {
      return EMPTY_RESOURCES;
    }
    File[] subDirectories = file.listFiles(this::isVisibleDirectory);
    if (subDirectories == null) {
      return EMPTY_RESOURCES;
    }
    Arrays.sort(subDirectories, FILE_PATH_COMPARATOR);
    if (type == ResourceType.DIRECTORY) {
      return Arrays.stream(subDirectories).map(FileSystemResource::new).toArray(Resource[]::new);
    }
    List<Resource> resources = new ArrayList<>();
    FilenameFilter filter = (dir, name) -> name.equals(fileName);
    for (File subDirectory : subDirectories) {
      File[] files = subDirectory.listFiles(filter);
      if (files != null) {
        Arrays.sort(files, FILE_NAME_COMPARATOR);
        Arrays.stream(files).map(FileSystemResource::new).forEach(resources::add);
      }
    }
    return resources.toArray(EMPTY_RESOURCES);
  }

  private void validatePattern(String location, ResourceType type) {
    Assert.state(isPattern(location), () -> String.format("Location '%s' must be a pattern", location));
    Assert.state(!location.startsWith(PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX),
            () -> String.format("Location '%s' cannot use classpath wildcards", location));
    Assert.state(StringUtils.countOccurrencesOf(location, "*") == 1,
            () -> String.format("Location '%s' cannot contain multiple wildcards", location));
    String directoryPath = (type != ResourceType.DIRECTORY) ? location.substring(0, location.lastIndexOf("/") + 1)
                                                            : location;
    Assert.state(directoryPath.endsWith("*/"), () -> String.format("Location '%s' must end with '*/'", location));
  }

  private File getFile(String patternLocation, Resource resource) {
    try {
      return resource.getFile();
    }
    catch (Exception ex) {
      throw new IllegalStateException(
              "Unable to load config data resource from pattern '" + patternLocation + "'", ex);
    }
  }

  private boolean isVisibleDirectory(File file) {
    return file.isDirectory() && !file.getName().startsWith("..");
  }

  /**
   * Resource types that can be returned.
   */
  enum ResourceType {

    /**
     * Return file resources.
     */
    FILE,

    /**
     * Return directory resources.
     */
    DIRECTORY

  }

}
