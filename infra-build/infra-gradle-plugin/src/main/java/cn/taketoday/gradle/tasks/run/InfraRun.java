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

package cn.taketoday.gradle.tasks.run;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.Set;

/**
 * Custom {@link JavaExec} task for running a Infra application.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Application should always run")
public abstract class InfraRun extends JavaExec {

  public InfraRun() {
    getOptimizedLaunch().convention(true);
  }

  /**
   * Returns the property for whether the JVM's launch should be optimized. The property
   * defaults to {@code true}.
   *
   * @return whether the JVM's launch should be optimized
   */
  @Input
  public abstract Property<Boolean> getOptimizedLaunch();

  /**
   * Adds the {@link SourceDirectorySet#getSrcDirs() source directories} of the given
   * {@code sourceSet's} {@link SourceSet#getResources() resources} to the start of the
   * classpath in place of the {@link SourceSet#getOutput output's}
   * {@link SourceSetOutput#getResourcesDir() resources directory}.
   *
   * @param sourceSet the source set
   */
  public void sourceResources(SourceSet sourceSet) {
    File resourcesDir = sourceSet.getOutput().getResourcesDir();
    Set<File> srcDirs = sourceSet.getResources().getSrcDirs();
    setClasspath(getProject().files(srcDirs, getClasspath()).filter((file) -> !file.equals(resourcesDir)));
  }

  @Override
  public void exec() {
    if (getOptimizedLaunch().get()) {
      setJvmArgs(getJvmArgs());
      jvmArgs("-XX:TieredStopAtLevel=1");
    }
    if (System.console() != null) {
      // Record that the console is available here for AnsiOutput to detect later
      getEnvironment().put("infra.output.ansi.console-available", true);
    }
    super.exec();
  }

}
