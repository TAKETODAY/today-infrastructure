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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.report.IReportGroupVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/15 14:23
 */
@Mojo(name = "code-coverage", threadSafe = true, aggregator = true)
public class CodeCoverageMojo extends AbstractReportMojo {

  /**
   * Output directory for the reports. Note that this parameter is only
   * relevant if the goal is run from the command line or from the default
   * build lifecycle. If the goal is run indirectly as part of a site
   * generation, the output directory configured in the Maven Site Plugin is
   * used instead.
   */
  @Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco")
  private File outputDirectory;

  /**
   * File with execution data.
   */
  @Parameter(property = "jacoco.dataFile", defaultValue = "${project.build.directory}/jacoco.exec")
  private File dataFile;

  @Override
  boolean canGenerateReportRegardingDataFiles() {
    return dataFile.exists();
  }

  @Override
  boolean canGenerateReportRegardingClassesDirectory() {
    return new File(project.getBuild().getOutputDirectory()).exists();
  }

  @Override
  void loadExecutionData(final ReportSupport support) throws IOException {
    support.loadExecutionData(dataFile);
  }

  @Override
  File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  void createReport(final IReportGroupVisitor visitor,
          final ReportSupport support) throws IOException {
    support.processProject(visitor, title, project, getIncludes(),
            getExcludes(), sourceEncoding);
  }

  @Override
  public File getReportOutputDirectory() {
    return outputDirectory;
  }

  @Override
  public void setReportOutputDirectory(final File reportOutputDirectory) {
    if (reportOutputDirectory != null
            && !reportOutputDirectory.getAbsolutePath().endsWith("jacoco")) {
      outputDirectory = new File(reportOutputDirectory, "jacoco");
    }
    else {
      outputDirectory = reportOutputDirectory;
    }
  }

  @Override
  public String getOutputName() {
    return "jacoco/index";
  }

  @Override
  public String getName(final Locale locale) {
    return "JaCoCo";
  }

}
