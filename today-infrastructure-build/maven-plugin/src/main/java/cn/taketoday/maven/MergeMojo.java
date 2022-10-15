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
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Mojo for merging a set of execution data files (*.exec) into a single file
 *
 * @since 0.6.4
 */
@Mojo(name = "merge", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class MergeMojo extends AbstractJacocoMojo {

  private static final String MSG_SKIPPING = "Skipping JaCoCo merge execution due to missing execution data files";

  /**
   * Path to the output file for execution data.
   */
  @Parameter(property = "jacoco.destFile", defaultValue = "${project.build.directory}/jacoco.exec")
  private File destFile;

  /**
   * This mojo accepts any number of execution data file sets.
   *
   * <pre>
   * <code>
   * &lt;fileSets&gt;
   *   &lt;fileSet&gt;
   *     &lt;directory&gt;${project.build.directory}&lt;/directory&gt;
   *     &lt;includes&gt;
   *       &lt;include&gt;*.exec&lt;/include&gt;
   *     &lt;/includes&gt;
   *   &lt;/fileSet&gt;
   * &lt;/fileSets&gt;
   * </code>
   * </pre>
   */
  @Parameter(required = true)
  private List<FileSet> fileSets;

  @Override
  protected void executeMojo()
          throws MojoExecutionException, MojoFailureException {
    if (!canMergeReports()) {
      return;
    }
    executeMerge();
  }

  private boolean canMergeReports() {
    if (fileSets == null || fileSets.isEmpty()) {
      getLog().info(MSG_SKIPPING);
      return false;
    }
    return true;
  }

  private void executeMerge() throws MojoExecutionException {
    final ExecFileLoader loader = new ExecFileLoader();

    load(loader);
    save(loader);
  }

  private void load(final ExecFileLoader loader)
          throws MojoExecutionException {
    final FileSetManager fileSetManager = new FileSetManager(getLog());
    for (final FileSet fileSet : fileSets) {
      for (final String includedFilename : fileSetManager
              .getIncludedFiles(fileSet)) {
        final File inputFile = new File(fileSet.getDirectory(),
                includedFilename);
        if (inputFile.isDirectory()) {
          continue;
        }
        try {
          getLog().info("Loading execution data file "
                  + inputFile.getAbsolutePath());
          loader.load(inputFile);
        }
        catch (final IOException e) {
          throw new MojoExecutionException(
                  "Unable to read " + inputFile.getAbsolutePath(), e);
        }
      }
    }
  }

  private void save(final ExecFileLoader loader)
          throws MojoExecutionException {
    if (loader.getExecutionDataStore().getContents().isEmpty()) {
      getLog().info(MSG_SKIPPING);
      return;
    }
    getLog().info("Writing merged execution data to "
            + destFile.getAbsolutePath());
    try {
      loader.save(destFile, false);
    }
    catch (final IOException e) {
      throw new MojoExecutionException(
              "Unable to write merged file " + destFile.getAbsolutePath(),
              e);
    }
  }

}
