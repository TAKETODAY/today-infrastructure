/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Encapsulates the configuration of the launch script for an executable jar or war.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LaunchScriptConfiguration implements Serializable {

  private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");

  private static final Pattern LINE_FEED_PATTERN = Pattern.compile("\n");

  // We don't care about the order, but Gradle's configuration cache currently does.
  // https://github.com/gradle/gradle/pull/17863
  private final Map<String, String> properties = new TreeMap<>();

  private File script;

  public LaunchScriptConfiguration() { }

  LaunchScriptConfiguration(AbstractArchiveTask archiveTask) {
    Project project = archiveTask.getProject();
    String baseName = archiveTask.getArchiveBaseName().get();
    putIfMissing(this.properties, "initInfoProvides", baseName);
    putIfMissing(this.properties, "initInfoShortDescription", removeLineBreaks(project.getDescription()), baseName);
    putIfMissing(this.properties, "initInfoDescription", augmentLineBreaks(project.getDescription()), baseName);
  }

  /**
   * Returns the properties that are applied to the launch script when it's being
   * including in the executable archive.
   *
   * @return the properties
   */
  @Input
  public Map<String, String> getProperties() {
    return this.properties;
  }

  /**
   * Sets the properties that are applied to the launch script when it's being including
   * in the executable archive.
   *
   * @param properties the properties
   */
  public void properties(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  /**
   * Returns the script {@link File} that will be included in the executable archive.
   * When {@code null}, the default launch script will be used.
   *
   * @return the script file
   */
  @Optional
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getScript() {
    return this.script;
  }

  /**
   * Sets the script {@link File} that will be included in the executable archive. When
   * {@code null}, the default launch script will be used.
   *
   * @param script the script file
   */
  public void setScript(File script) {
    this.script = script;
  }

  private String removeLineBreaks(String string) {
    return string != null ? WHITE_SPACE_PATTERN.matcher(string).replaceAll(" ") : null;
  }

  private String augmentLineBreaks(String string) {
    return string != null ? LINE_FEED_PATTERN.matcher(string).replaceAll("\n#  ") : null;
  }

  private void putIfMissing(Map<String, String> properties, String key, String... valueCandidates) {
    if (!properties.containsKey(key)) {
      for (String candidate : valueCandidates) {
        if (candidate != null && !candidate.isEmpty()) {
          properties.put(key, candidate);
          return;
        }
      }
    }
  }

}
