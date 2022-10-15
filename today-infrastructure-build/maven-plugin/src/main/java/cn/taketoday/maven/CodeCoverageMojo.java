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

  /**
   * The projects in the reactor.
   */
  @Parameter(property = "reactorProjects", readonly = true)
  private List<MavenProject> reactorProjects;

  @Override
  boolean canGenerateReportRegardingDataFiles() {
    return dataFile.exists();
  }

  @Override
  boolean canGenerateReportRegardingClassesDirectory() {
    return new File(project.getBuild().getOutputDirectory()).exists();
  }

  @Override
  void loadExecutionData(ReportSupport support) throws IOException {
    support.loadExecutionData(dataFile);
  }

  @Override
  File getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  void createReport(IReportGroupVisitor visitor, ReportSupport support) throws IOException {
    IReportGroupVisitor group = visitor.visitGroup(title);
    for (MavenProject dependency : findDependencies(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_PROVIDED)) {
      processProject(support, group, dependency);
    }
  }

  private void processProject(ReportSupport support,
          IReportGroupVisitor group, MavenProject project) throws IOException {
    support.processAll(group, project, reactorProjects);
  }

  @Override
  public File getReportOutputDirectory() {
    return outputDirectory;
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
