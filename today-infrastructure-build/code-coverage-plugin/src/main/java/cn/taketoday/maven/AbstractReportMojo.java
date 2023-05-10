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

import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReportException;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Base class for creating a code coverage report for tests of a single project
 * in multiple formats (HTML, XML, and CSV).
 */
public abstract class AbstractReportMojo extends AbstractMojo
        implements MavenMultiPageReport {

  /**
   * Encoding of the generated reports.
   */
  @Parameter(property = "project.reporting.outputEncoding", defaultValue = "UTF-8")
  String outputEncoding;

  /**
   * A list of report formats to generate. Supported formats are HTML, XML and
   * CSV. Defaults to all formats if no values are given.
   *
   * @since 0.8.7
   */
  @Parameter(defaultValue = "HTML,XML,CSV")
  List<ReportFormat> formats;

  /**
   * Name of the root node HTML report pages.
   *
   * @since 0.7.7
   */
  @Parameter(defaultValue = "${project.name}")
  String title;

  /**
   * Footer text used in HTML report pages.
   *
   * @since 0.7.7
   */
  @Parameter
  String footer;

  /**
   * Encoding of the source files.
   */
  @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
  String sourceEncoding;

  /**
   * A list of class files to include in the report. May use wildcard
   * characters (* and ?). When not specified everything will be included.
   */
  @Parameter
  List<String> includes;

  /**
   * A list of class files to exclude from the report. May use wildcard
   * characters (* and ?). When not specified nothing will be excluded.
   */
  @Parameter
  List<String> excludes;

  /**
   * Maven project.
   */
  @Parameter(property = "project", readonly = true)
  MavenProject project;

  public String getDescription(final Locale locale) {
    return getName(locale) + " Coverage Report.";
  }

  public boolean isExternalReport() {
    return true;
  }

  public String getCategoryName() {
    return CATEGORY_PROJECT_REPORTS;
  }

  /**
   * Returns the list of class files to include in the report.
   *
   * @return class files to include, may contain wildcard characters
   */
  List<String> getIncludes() {
    return includes;
  }

  /**
   * Returns the list of class files to exclude from the report.
   *
   * @return class files to exclude, may contain wildcard characters
   */
  List<String> getExcludes() {
    return excludes;
  }

  @Override
  public boolean canGenerateReport() {
    return true;
  }

  abstract File getOutputDirectory();

  public void generate(
          @SuppressWarnings("deprecation") final org.codehaus.doxia.sink.Sink sink,
          final Locale locale) throws MavenReportException {
    generate(sink, null, locale);
  }

  public void generate(final org.apache.maven.doxia.sink.Sink sink,
          final SinkFactory sinkFactory, final Locale locale)
          throws MavenReportException {
    if (!canGenerateReport()) {
      return;
    }
    executeReport(locale);
  }

  /**
   * This method is called when the report generation is invoked directly as a
   * standalone Mojo.
   */
  public void execute() throws MojoExecutionException {
    if (!canGenerateReport()) {
      return;
    }
    try {
      executeReport(Locale.getDefault());
    }
    catch (final MavenReportException e) {
      throw new MojoExecutionException("An error has occurred in "
              + getName(Locale.ENGLISH) + " report generation.", e);
    }
  }

  private void executeReport(final Locale locale) throws MavenReportException {
    try {
      final ReportSupport support = new ReportSupport(getLog());
      support.setExcludes(getExcludes());
      support.setIncludes(getIncludes());
      loadExecutionData(support);
      addFormatters(support, locale);
      final IReportVisitor visitor = support.initRootVisitor();
      createReport(visitor, support);
      visitor.visitEnd();
    }
    catch (final IOException e) {
      throw new MavenReportException(
              "Error while creating report: " + e.getMessage(), e);
    }
  }

  private void addFormatters(final ReportSupport support, final Locale locale)
          throws IOException {
    getOutputDirectory().mkdirs();
    for (final ReportFormat f : formats) {
      support.addVisitor(f.createVisitor(this, locale));
    }
  }

  abstract void loadExecutionData(final ReportSupport support)
          throws IOException;

  abstract void createReport(final IReportGroupVisitor visitor,
          final ReportSupport support) throws IOException;

}
