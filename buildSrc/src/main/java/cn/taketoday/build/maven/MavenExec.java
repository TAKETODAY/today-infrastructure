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

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.internal.ExecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A custom {@link JavaExec} {@link Task task} for running Maven.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MavenExec extends JavaExec {

  private final Logger log = LoggerFactory.getLogger(MavenExec.class);

  private File projectDir;

  public MavenExec() {
    setClasspath(mavenConfiguration(getProject()));
    args("--batch-mode");
    getMainClass().set("org.apache.maven.cli.MavenCli");
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
    getInputs().file(new File(projectDir, "pom.xml"))
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("pom");
  }

  @Override
  public void exec() {
    workingDir(this.projectDir);
    systemProperty("maven.multiModuleProjectDirectory", this.projectDir.getAbsolutePath());
    try {
      Path logFile = Files.createTempFile(getName(), ".log");
      try {
        args("--log-file", logFile.toFile().getAbsolutePath());
        super.exec();
        if (this.log.isInfoEnabled()) {
          Files.readAllLines(logFile).forEach(this.log::info);
        }
      }
      catch (ExecException ex) {
        System.out.println("Exec exception! Dumping log");
        Files.readAllLines(logFile).forEach(System.out::println);
        throw ex;
      }
    }
    catch (IOException ex) {
      throw new TaskExecutionException(this, ex);
    }
  }

  private Configuration mavenConfiguration(Project project) {
    Configuration existing = project.getConfigurations().findByName("maven");
    if (existing != null) {
      return existing;
    }
    return project.getConfigurations().create("maven", (maven) -> {
      maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-embedder:3.6.3"));
      maven.getDependencies().add(project.getDependencies().create("org.apache.maven:maven-compat:3.6.3"));
      maven.getDependencies().add(project.getDependencies().create("org.slf4j:slf4j-simple:1.7.5"));
      maven.getDependencies()
              .add(project.getDependencies()
                      .create("org.apache.maven.resolver:maven-resolver-connector-basic:1.4.1"));
      maven.getDependencies()
              .add(project.getDependencies().create("org.apache.maven.resolver:maven-resolver-transport-http:1.4.1"));
    });
  }

  @Internal
  public File getProjectDir() {
    return this.projectDir;
  }

}
