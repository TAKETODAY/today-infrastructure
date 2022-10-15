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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Restores original classes as they were before offline instrumentation.
 *
 * @since 0.6.2
 */
@Mojo(name = "restore-instrumented-classes", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class RestoreMojo extends AbstractJacocoMojo {

  @Override
  protected void executeMojo()
          throws MojoExecutionException, MojoFailureException {
    final File originalClassesDir = new File(
            getProject().getBuild().getDirectory(),
            "generated-classes/jacoco");
    final File classesDir = new File(
            getProject().getBuild().getOutputDirectory());
    try {
      FileUtils.copyDirectoryStructure(originalClassesDir, classesDir);
    }
    catch (final IOException e) {
      throw new MojoFailureException("Unable to restore classes.", e);
    }
  }

}
