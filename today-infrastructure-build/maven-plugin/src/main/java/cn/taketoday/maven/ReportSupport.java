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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.check.IViolationsOutput;
import org.jacoco.report.check.Rule;
import org.jacoco.report.check.RulesChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

/**
 * Encapsulates the tasks to create reports for Maven projects. Instances are
 * supposed to be used in the following sequence:
 *
 * <ol>
 * <li>Create an instance</li>
 * <li>Load one or multiple exec files with
 * <code>loadExecutionData()</code></li>
 * <li>Add one or multiple formatters with <code>addXXX()</code> methods</li>
 * <li>Create the root visitor with <code>initRootVisitor()</code></li>
 * <li>Process one or multiple projects with <code>processProject()</code></li>
 * </ol>
 */
final class ReportSupport {

  private final Log log;
  private final ExecFileLoader loader;
  private final List<IReportVisitor> formatters;

  /**
   * A list of class files to include in the report. May use wildcard
   * characters (* and ?). When not specified everything will be included.
   */
  private List<String> includes;

  /**
   * A list of class files to exclude from the report. May use wildcard
   * characters (* and ?). When not specified nothing will be excluded.
   */
  private List<String> excludes;

  /**
   * Construct a new instance with the given log output.
   *
   * @param log for log output
   */
  public ReportSupport(Log log) {
    this.log = log;
    this.loader = new ExecFileLoader();
    this.formatters = new ArrayList<>();
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  /**
   * Loads the given execution data file.
   *
   * @param execFile execution data file to load
   * @throws IOException if the file can't be loaded
   */
  public void loadExecutionData(File execFile) throws IOException {
    log.info("Loading execution data file " + execFile);
    loader.load(execFile);
  }

  public void addVisitor(IReportVisitor visitor) {
    formatters.add(visitor);
  }

  public void addRulesChecker(List<Rule> rules,
          IViolationsOutput output) {
    RulesChecker checker = new RulesChecker();
    checker.setRules(rules);
    formatters.add(checker.createVisitor(output));
  }

  public IReportVisitor initRootVisitor() throws IOException {
    IReportVisitor visitor = new MultiReportVisitor(formatters);
    visitor.visitInfo(loader.getSessionInfoStore().getInfos(),
            loader.getExecutionDataStore().getContents());
    return visitor;
  }

  /**
   * Calculates coverage for the given project and emits it to the report
   * group including source references
   *
   * @param visitor group visitor to emit the project's coverage to
   * @throws IOException if class files can't be read
   */
  public void processAll(IReportGroupVisitor visitor,
          MavenProject rootProject, List<MavenProject> reactorProjects) throws IOException {
    RootSourceFileCollection locator = new RootSourceFileCollection(reactorProjects);
    for (MavenProject reactorProject : reactorProjects) {
      if ("jar".equals(reactorProject.getPackaging())) {
        CoverageBuilder builder = new CoverageBuilder();

        File classesDir = new File(
                reactorProject.getBuild().getOutputDirectory());
        if (classesDir.isDirectory()) {
          Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
          FileFilter filter = new FileFilter(includes, excludes);
          for (File file : filter.getFiles(classesDir)) {
            analyzer.analyzeAll(file);
          }
        }

        IBundleCoverage bundle = builder.getBundle(reactorProject.getArtifactId());
        logBundleInfo(bundle, builder.getNoMatchClasses());
        visitor.visitBundle(bundle, locator);
      }
    }
  }

  private void logBundleInfo(IBundleCoverage bundle, Collection<IClassCoverage> nomatch) {
    log.info(format("Analyzed bundle '%s' with %s classes", bundle.getName(), bundle.getClassCounter().getTotalCount()));
    if (!nomatch.isEmpty()) {
      log.warn(format("Classes in bundle '%s' do not match with execution data. For report generation the same class files must be used as at runtime.",
              bundle.getName()));
      for (IClassCoverage c : nomatch) {
        log.warn(format("Execution data for class %s does not match.", c.getName()));
      }
    }
    if (bundle.containsCode()
            && bundle.getLineCounter().getTotalCount() == 0) {
      log.warn("To enable source code annotation class files have to be compiled with debug information.");
    }
  }

  private static List<File> getCompileSourceRoots(MavenProject project) {
    List<File> result = new ArrayList<>();
    for (String path : project.getCompileSourceRoots()) {
      result.add(resolvePath(project, path));
    }
    return result;
  }

  private static File resolvePath(MavenProject project, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(project.getBasedir(), path);
    }
    return file;
  }

  private static class RootSourceFileCollection implements ISourceFileLocator {

    private final List<File> sourceRoots;

    public RootSourceFileCollection(List<MavenProject> reactorProjects) {
      var sourceRoots = new ArrayList<File>();
      for (MavenProject reactorProject : reactorProjects) {
        sourceRoots.addAll(getCompileSourceRoots(reactorProject));
      }
      this.sourceRoots = sourceRoots;
    }

    @Override
    public Reader getSourceFile(String packageName, String fileName) throws IOException {
      String r;
      if (packageName.length() > 0) {
        r = packageName + '/' + fileName;
      }
      else {
        r = fileName;
      }
      for (File sourceRoot : sourceRoots) {
        File file = new File(sourceRoot, r);
        if (file.exists() && file.isFile()) {
          return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        }
      }
      return null;
    }

    @Override
    public int getTabWidth() {
      return 2;
    }
  }

}
