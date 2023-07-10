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

package cn.taketoday.gradle.tasks.buildinfo;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;

import cn.taketoday.app.loader.tools.BuildPropertiesWriter;
import cn.taketoday.app.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * {@link Task} for generating a {@code build-info.properties} file from a
 * {@code Project}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class BuildInfo extends DefaultTask {

  private final BuildInfoProperties properties;

  public BuildInfo() {
    this.properties = getProject().getObjects().newInstance(BuildInfoProperties.class, getExcludes());
    getDestinationDir().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
  }

  /**
   * Returns the names of the properties to exclude from the output.
   *
   * @return names of the properties to exclude
   */
  @Internal
  public abstract SetProperty<String> getExcludes();

  /**
   * Generates the {@code build-info.properties} file in the configured
   * {@link #getDestinationDir destination}.
   */
  @TaskAction
  public void generateBuildProperties() {
    try {
      ProjectDetails details = new ProjectDetails(this.properties.getGroupIfNotExcluded(),
              this.properties.getArtifactIfNotExcluded(), this.properties.getVersionIfNotExcluded(),
              this.properties.getNameIfNotExcluded(), this.properties.getTimeIfNotExcluded(),
              this.properties.getAdditionalIfNotExcluded());
      new BuildPropertiesWriter(new File(getDestinationDir().get().getAsFile(), "build-info.properties"))
              .writeBuildProperties(details);
    }
    catch (IOException ex) {
      throw new TaskExecutionException(this, ex);
    }
  }

  /**
   * Returns the directory to which the {@code build-info.properties} file will be
   * written.
   *
   * @return the destination directory
   */
  @OutputDirectory
  public abstract DirectoryProperty getDestinationDir();

  /**
   * Returns the {@link BuildInfoProperties properties} that will be included in the
   * {@code build-info.properties} file.
   *
   * @return the properties
   */
  @Nested
  public BuildInfoProperties getProperties() {
    return this.properties;
  }

  /**
   * Executes the given {@code action} on the {@link #getProperties()} properties.
   *
   * @param action the action
   */
  public void properties(Action<BuildInfoProperties> action) {
    action.execute(this.properties);
  }

}
