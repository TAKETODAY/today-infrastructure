/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to work with {@code reachability-metadata.properties}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ReachabilityMetadataProperties {

  /**
   * Location of the properties file. Must be formatted using
   * {@link String#format(String, Object...)} with the group id, artifact id and version
   * of the dependency.
   */
  public static final String REACHABILITY_METADATA_PROPERTIES_LOCATION_TEMPLATE = "META-INF/native-image/%s/%s/%s/reachability-metadata.properties";

  private final Properties properties;

  private ReachabilityMetadataProperties(Properties properties) {
    this.properties = properties;
  }

  /**
   * Returns if the dependency has been overridden.
   *
   * @return true if the dependency has been overridden
   */
  public boolean isOverridden() {
    return Boolean.parseBoolean(this.properties.getProperty("override"));
  }

  /**
   * Constructs a new instance from the given {@code InputStream}.
   *
   * @param inputStream {@code InputStream} to load the properties from
   * @return loaded properties
   * @throws IOException if loading from the {@code InputStream} went wrong
   */
  public static ReachabilityMetadataProperties fromInputStream(InputStream inputStream) throws IOException {
    Properties properties = new Properties();
    properties.load(inputStream);
    return new ReachabilityMetadataProperties(properties);
  }

  /**
   * Returns the location of the properties for the given coordinates.
   *
   * @param coordinates library coordinates for which the property file location should
   * be returned
   * @return location of the properties
   */
  public static String getLocation(LibraryCoordinates coordinates) {
    return REACHABILITY_METADATA_PROPERTIES_LOCATION_TEMPLATE.formatted(coordinates.getGroupId(),
            coordinates.getArtifactId(), coordinates.getVersion());
  }

}
