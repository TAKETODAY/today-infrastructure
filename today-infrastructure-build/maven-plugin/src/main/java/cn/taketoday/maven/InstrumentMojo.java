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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Performs offline instrumentation. Note that after execution of test you must
 * restore original classes with help of "restore-instrumented-classes" goal.
 * <p>
 * <strong>Warning:</strong> The preferred way for code coverage analysis with
 * JaCoCo is on-the-fly instrumentation. Offline instrumentation has several
 * drawbacks and should only be used if a specific scenario explicitly requires
 * this mode. Please consult <a href="offline.html">documentation</a> about
 * offline instrumentation before using this mode.
 * </p>
 *
 * @since 0.6.2
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public class InstrumentMojo extends AbstractJacocoMojo {

  /**
   * A list of class files to include in instrumentation. May use wildcard
   * characters (* and ?). When not specified everything will be included.
   */
  @Parameter
  private List<String> includes;

  /**
   * A list of class files to exclude from instrumentation. May use wildcard
   * characters (* and ?). When not specified nothing will be excluded. Except
   * for performance optimization or technical corner cases this option is
   * normally not required. If you want to exclude classes from the report
   * please configure the <code>report</code> goal accordingly.
   */
  @Parameter
  private List<String> excludes;

  @Override
  public void executeMojo()
          throws MojoExecutionException, MojoFailureException {
    MavenProject project = getProject();

    final File originalClassesDir = new File(project.getBuild().getDirectory(), "generated-classes/jacoco");
    originalClassesDir.mkdirs();
    final File classesDir = new File(
            project.getBuild().getOutputDirectory());
    if (!classesDir.exists()) {
      getLog().info("Skipping JaCoCo execution due to missing classes directory:" + classesDir);
      return;
    }

    final List<String> fileNames;
    try {
      fileNames = new FileFilter(includes, excludes)
              .getFileNames(classesDir);
    }
    catch (final IOException e1) {
      throw new MojoExecutionException(
              "Unable to get list of files to instrument.", e1);
    }

    final Instrumenter instrumenter = new Instrumenter(
            new OfflineInstrumentationAccessGenerator());
    for (final String fileName : fileNames) {
      if (fileName.endsWith(".class")) {
        final File source = new File(classesDir, fileName);
        final File backup = new File(originalClassesDir, fileName);
        InputStream input = null;
        OutputStream output = null;
        try {
          FileUtils.copyFile(source, backup);
          input = new FileInputStream(backup);
          output = new FileOutputStream(source);
          instrumenter.instrument(input, output, source.getPath());
        }
        catch (final IOException e2) {
          throw new MojoExecutionException(
                  "Unable to instrument file.", e2);
        }
        finally {
          IOUtil.close(input);
          IOUtil.close(output);
        }
      }
    }
  }

}
