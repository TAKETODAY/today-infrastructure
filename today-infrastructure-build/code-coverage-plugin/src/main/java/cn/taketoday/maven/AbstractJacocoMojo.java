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
package cn.taketoday.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Base class for JaCoCo Mojos.
 */
public abstract class AbstractJacocoMojo extends AbstractMojo {

  /**
   * Maven project.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /**
   * Flag used to suppress execution.
   */
  @Parameter(property = "jacoco.skip", defaultValue = "false")
  private boolean skip;

  public final void execute()
          throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info(
              "Skipping JaCoCo execution because property jacoco.skip is set.");
      skipMojo();
      return;
    }
    executeMojo();
  }

  /**
   * Executes Mojo.
   *
   * @throws MojoExecutionException if an unexpected problem occurs. Throwing this exception
   * causes a "BUILD ERROR" message to be displayed.
   * @throws MojoFailureException if an expected problem (such as a compilation failure)
   * occurs. Throwing this exception causes a "BUILD FAILURE"
   * message to be displayed.
   */
  protected abstract void executeMojo()
          throws MojoExecutionException, MojoFailureException;

  /**
   * Skips Mojo.
   */
  protected void skipMojo() {
  }

  /**
   * @return Maven project
   */
  protected final MavenProject getProject() {
    return project;
  }

}
