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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@code BuildPropertiesWriter} writes the {@code build-info.properties} for
 * consumption by the Actuator.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class BuildPropertiesWriter {

  private final File outputFile;

  /**
   * Creates a new {@code BuildPropertiesWriter} that will write to the given
   * {@code outputFile}.
   *
   * @param outputFile the output file
   */
  public BuildPropertiesWriter(File outputFile) {
    this.outputFile = outputFile;
  }

  public void writeBuildProperties(ProjectDetails projectDetails) throws IOException {
    Properties properties = createBuildInfo(projectDetails);
    createFileIfNecessary(this.outputFile);
    try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
      properties.store(outputStream, "Properties");
    }
  }

  private void createFileIfNecessary(File file) throws IOException {
    if (file.exists()) {
      return;
    }
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IllegalStateException(
              "Cannot create parent directory for '" + this.outputFile.getAbsolutePath() + "'");
    }
    if (!file.createNewFile()) {
      throw new IllegalStateException("Cannot create target file '" + this.outputFile.getAbsolutePath() + "'");
    }
  }

  protected Properties createBuildInfo(ProjectDetails project) {
    Properties properties = CollectionUtils.createSortedProperties(true);
    addIfHasValue(properties, "build.group", project.getGroup());
    addIfHasValue(properties, "build.artifact", project.getArtifact());
    addIfHasValue(properties, "build.name", project.getName());
    addIfHasValue(properties, "build.version", project.getVersion());
    if (project.getTime() != null) {
      properties.put("build.time", DateTimeFormatter.ISO_INSTANT.format(project.getTime()));
    }
    if (project.getAdditionalProperties() != null) {
      project.getAdditionalProperties().forEach((name, value) -> properties.put("build." + name, value));
    }
    return properties;
  }

  private void addIfHasValue(Properties properties, String name, String value) {
    if (StringUtils.hasText(value)) {
      properties.put(name, value);
    }
  }

  /**
   * Build-system agnostic details of a project.
   */
  public static final class ProjectDetails {

    private final String group;

    private final String artifact;

    private final String name;

    private final String version;

    private final Instant time;

    private final Map<String, String> additionalProperties;

    public ProjectDetails(String group, String artifact, String version, String name, Instant time,
            Map<String, String> additionalProperties) {
      this.group = group;
      this.artifact = artifact;
      this.name = name;
      this.version = version;
      this.time = time;
      validateAdditionalProperties(additionalProperties);
      this.additionalProperties = additionalProperties;
    }

    private static void validateAdditionalProperties(Map<String, String> additionalProperties) {
      if (additionalProperties != null) {
        additionalProperties.forEach((name, value) -> {
          if (value == null) {
            throw new NullAdditionalPropertyValueException(name);
          }
        });
      }
    }

    public String getGroup() {
      return this.group;
    }

    public String getArtifact() {
      return this.artifact;
    }

    public String getName() {
      return this.name;
    }

    public String getVersion() {
      return this.version;
    }

    public Instant getTime() {
      return this.time;
    }

    public Map<String, String> getAdditionalProperties() {
      return this.additionalProperties;
    }

  }

  /**
   * Exception thrown when an additional property with a null value is encountered.
   */
  public static class NullAdditionalPropertyValueException extends IllegalArgumentException {

    public NullAdditionalPropertyValueException(String name) {
      super("Additional property '" + name + "' is illegal as its value is null");
    }

  }

}
