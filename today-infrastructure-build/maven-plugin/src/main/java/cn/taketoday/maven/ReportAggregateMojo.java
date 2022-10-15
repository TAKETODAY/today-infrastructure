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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jacoco.report.IReportGroupVisitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * Creates a structured code coverage report (HTML, XML, and CSV) from multiple
 * projects within reactor. The report is created from all modules this project
 * depends on, and optionally this project itself. From those projects class and
 * source files as well as JaCoCo execution data files will be collected. In
 * addition execution data is collected from the project itself. This also
 * allows to create coverage reports when tests are in separate projects than
 * the code under test, for example in case of integration tests.
 * </p>
 *
 * <p>
 * Using the dependency scope allows to distinguish projects which contribute
 * execution data but should not become part of the report:
 * </p>
 *
 * <ul>
 * <li><code>compile</code>, <code>runtime</code>, <code>provided</code>:
 * Project source and execution data is included in the report.</li>
 * <li><code>test</code>: Only execution data is considered for the report.</li>
 * </ul>
 *
 * @since 0.7.7
 */
@Mojo(name = "report-aggregate", threadSafe = true)
public class ReportAggregateMojo extends AbstractReportMojo {

  /**
   * A list of execution data files to include in the report from each
   * project. May use wildcard characters (* and ?). When not specified all
   * *.exec files from the target folder will be included.
   */
  @Parameter
  List<String> dataFileIncludes;

  /**
   * A list of execution data files to exclude from the report. May use
   * wildcard characters (* and ?). When not specified nothing will be
   * excluded.
   */
  @Parameter
  List<String> dataFileExcludes;

  /**
   * Output directory for the reports. Note that this parameter is only
   * relevant if the goal is run from the command line or from the default
   * build lifecycle. If the goal is run indirectly as part of a site
   * generation, the output directory configured in the Maven Site Plugin is
   * used instead.
   */
  @Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco-aggregate")
  private File outputDirectory;

  /**
   * Include this project in the report. If true then this projects class and
   * source files as well as JaCoCo execution data files will be collected.
   */
  @Parameter(defaultValue = "false")
  private boolean includeCurrentProject;

  /**
   * The projects in the reactor.
   */
  @Parameter(property = "reactorProjects", readonly = true)
  private List<MavenProject> reactorProjects;

  @Override
  boolean canGenerateReportRegardingDataFiles() {
    return true;
  }

  @Override
  void loadExecutionData(ReportSupport support) throws IOException {
    // https://issues.apache.org/jira/browse/MNG-5440
    if (dataFileIncludes == null) {
      dataFileIncludes = Arrays.asList("target/*.exec");
    }

    FileFilter filter = new FileFilter(dataFileIncludes, dataFileExcludes);
    loadExecutionData(support, filter, project.getBasedir());
    for (MavenProject dependency : findDependencies(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_TEST)) {
      loadExecutionData(support, filter, dependency.getBasedir());
    }
  }

  private void loadExecutionData(ReportSupport support, FileFilter filter, File basedir) throws IOException {
    for (File execFile : filter.getFiles(basedir)) {
      support.loadExecutionData(execFile);
    }
  }

  @Override
  File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  void createReport(IReportGroupVisitor visitor, ReportSupport support) throws IOException {
    IReportGroupVisitor group = visitor.visitGroup(title);
    if (includeCurrentProject) {
      processProject(support, group, project);
    }
    for (MavenProject dependency : findDependencies(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_PROVIDED)) {
      processProject(support, group, dependency);
    }
  }

  private void processProject(
          ReportSupport support, IReportGroupVisitor group, MavenProject project) throws IOException {
    support.processProject(group, project.getArtifactId(), project, sourceEncoding);
  }

  public File getReportOutputDirectory() {
    return outputDirectory;
  }

  public void setReportOutputDirectory(File reportOutputDirectory) {
    if (reportOutputDirectory != null
            && !reportOutputDirectory.getAbsolutePath().endsWith("jacoco-aggregate")) {
      outputDirectory = new File(reportOutputDirectory, "jacoco-aggregate");
    }
    else {
      outputDirectory = reportOutputDirectory;
    }
  }

  public String getOutputName() {
    return "jacoco-aggregate/index";
  }

  public String getName(Locale locale) {
    return "JaCoCo Aggregate";
  }

  private List<MavenProject> findDependencies(String... scopes) {
    var result = new ArrayList<MavenProject>();
    List<String> scopeList = Arrays.asList(scopes);
    for (Dependency dependency : project.getDependencies()) {
      if (scopeList.contains(dependency.getScope())) {
        MavenProject project = findProjectFromReactor(dependency);
        if (project != null) {
          result.add(project);
        }
      }
    }
    return result;
  }

  /**
   * Note that if dependency specified using version range and reactor
   * contains multiple modules with same artifactId and groupId but of
   * different versions, then first dependency which matches range will be
   * selected. For example in case of range <code>[0,2]</code> if version 1 is
   * before version 2 in reactor, then version 1 will be selected.
   */
  private MavenProject findProjectFromReactor(Dependency d) {
    VersionRange depVersionAsRange;
    try {
      depVersionAsRange = VersionRange
              .createFromVersionSpec(d.getVersion());
    }
    catch (InvalidVersionSpecificationException e) {
      throw new AssertionError(e);
    }

    getLog().info("reactorProjects: " + reactorProjects);
    for (MavenProject p : reactorProjects) {
      DefaultArtifactVersion pv = new DefaultArtifactVersion(p.getVersion());
      if (p.getGroupId().equals(d.getGroupId())
              && p.getArtifactId().equals(d.getArtifactId())
              && depVersionAsRange.containsVersion(pv)) {
        return p;
      }
    }
    return null;
  }

}
