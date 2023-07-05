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

package cn.taketoday.build.maven;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link Task} to make Maven binaries available for integration testing.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PrepareMavenBinaries extends DefaultTask {

  private final Set<String> versions = new LinkedHashSet<>();

  private File outputDir;

  @OutputDirectory
  public File getOutputDir() {
    return this.outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  @Input
  public Set<String> getVersions() {
    return this.versions;
  }

  public void versions(String... versions) {
    this.versions.addAll(Arrays.asList(versions));
  }

  @TaskAction
  public void prepareBinaries() {
    for (String version : this.versions) {
      Configuration configuration = getProject().getConfigurations()
              .detachedConfiguration(
                      getProject().getDependencies().create("org.apache.maven:apache-maven:" + version + ":bin@zip"));
      getProject()
              .copy((copy) -> copy.into(this.outputDir).from(getProject().zipTree(configuration.getSingleFile())));
    }
  }

}
