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
import org.apache.maven.project.MavenProject;
import org.jacoco.report.IReportGroupVisitor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/15 14:23
 */
@Mojo(name = "analysis", threadSafe = true, aggregator = true)
public class CodeCoverageMojo extends AbstractReportMojo {

  public static final String DATA_FILE = "/jacoco/jacoco-unit.exec";
  public static final String output_Directory = "/jacoco";

  /**
   * The projects in the reactor.
   */
  @Parameter(property = "reactorProjects", readonly = true)
  private List<MavenProject> reactorProjects;

  @Parameter(defaultValue = "today-infrastructure")
  private String rootProjectName;

  /**
   * Output directory for the reports. Note that this parameter is only
   * relevant if the goal is run from the command line or from the default
   * build lifecycle. If the goal is run indirectly as part of a site
   * generation, the output directory configured in the Maven Site Plugin is
   * used instead.
   */
  private File outputDirectory;

  private File workDirectory;

  public File getWorkDirectory() {
    if (workDirectory == null) {
      if (!Objects.equals(rootProjectName, project.getName())) {
        MavenProject parent = project.getParent();
        if (parent != null) {
          workDirectory = new File(parent.getBuild().getDirectory());
        }
      }

      if (workDirectory == null) {
        workDirectory = new File(project.getBuild().getDirectory());
      }
    }
    return workDirectory;
  }

  @Override
  void loadExecutionData(ReportSupport support) throws IOException {
    File dataFile = new File(getWorkDirectory(), DATA_FILE);
    support.loadExecutionData(dataFile);
  }

  @Override
  File getOutputDirectory() {
    if (outputDirectory == null) {
      this.outputDirectory = new File(getWorkDirectory(), output_Directory);
    }
    return outputDirectory;
  }

  @Override
  void createReport(IReportGroupVisitor visitor, ReportSupport support) throws IOException {
    IReportGroupVisitor group = visitor.visitGroup(title);
    support.processAll(group, project, reactorProjects);
  }

  @Override
  public File getReportOutputDirectory() {
    return getOutputDirectory();
  }

  @Override
  public void setReportOutputDirectory(File reportOutputDirectory) {
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
  public String getName(Locale locale) {
    return "JaCoCo";
  }

}
