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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.check.IViolationsOutput;
import org.jacoco.report.check.Limit;
import org.jacoco.report.check.Rule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the code coverage metrics are being met.
 *
 * @since 0.6.1
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractJacocoMojo implements IViolationsOutput {

  private static final String MSG_SKIPPING = "Skipping JaCoCo execution due to missing execution data file:";
  private static final String CHECK_SUCCESS = "All coverage checks have been met.";
  private static final String CHECK_FAILED = "Coverage checks have not been met. See log for details.";

  /**
   * <p>
   * Check configuration used to specify rules on element types (BUNDLE,
   * PACKAGE, CLASS, SOURCEFILE or METHOD) with a list of limits. Each limit
   * applies to a certain counter (INSTRUCTION, LINE, BRANCH, COMPLEXITY,
   * METHOD, CLASS) and defines a minimum or maximum for the corresponding
   * value (TOTALCOUNT, COVEREDCOUNT, MISSEDCOUNT, COVEREDRATIO, MISSEDRATIO).
   * If a limit refers to a ratio it must be in the range from 0.0 to 1.0
   * where the number of decimal places will also determine the precision in
   * error messages. A limit ratio may optionally be declared as a percentage
   * where 0.80 and 80% represent the same value.
   * </p>
   *
   * <p>
   * If not specified the following defaults are assumed:
   * </p>
   *
   * <ul>
   * <li>rule element: BUNDLE</li>
   * <li>limit counter: INSTRUCTION</li>
   * <li>limit value: COVEREDRATIO</li>
   * </ul>
   *
   * <p>
   * This example requires an overall instruction coverage of 80% and no class
   * must be missed:
   * </p>
   *
   * <pre>
   * {@code
   * <rules>
   *   <rule>
   *     <element>BUNDLE</element>
   *     <limits>
   *       <limit>
   *         <counter>INSTRUCTION</counter>
   *         <value>COVEREDRATIO</value>
   *         <minimum>0.80</minimum>
   *       </limit>
   *       <limit>
   *         <counter>CLASS</counter>
   *         <value>MISSEDCOUNT</value>
   *         <maximum>0</maximum>
   *       </limit>
   *     </limits>
   *   </rule>
   * </rules>}
   * </pre>
   *
   * <p>
   * This example requires a line coverage minimum of 50% for every class
   * except test classes:
   * </p>
   *
   * <pre>
   * {@code
   * <rules>
   *   <rule>
   *     <element>CLASS</element>
   *     <excludes>
   *       <exclude>*Test</exclude>
   *     </excludes>
   *     <limits>
   *       <limit>
   *         <counter>LINE</counter>
   *         <value>COVEREDRATIO</value>
   *         <minimum>50%</minimum>
   *       </limit>
   *     </limits>
   *   </rule>
   * </rules>}
   * </pre>
   */
  @Parameter(required = true)
  private List<RuleConfiguration> rules;

  /**
   * Halt the build if any of the checks fail.
   */
  @Parameter(property = "jacoco.haltOnFailure", defaultValue = "true", required = true)
  private boolean haltOnFailure;

  /**
   * File with execution data.
   */
  @Parameter(defaultValue = "${project.build.directory}/jacoco.exec")
  private File dataFile;

  /**
   * A list of class files to include into analysis. May use wildcard
   * characters (* and ?). When not specified everything will be included.
   */
  @Parameter
  private List<String> includes;

  /**
   * A list of class files to exclude from analysis. May use wildcard
   * characters (* and ?). When not specified nothing will be excluded.
   */
  @Parameter
  private List<String> excludes;

  private boolean violations;

  private boolean canCheckCoverage() {
    if (!dataFile.exists()) {
      getLog().info(MSG_SKIPPING + dataFile);
      return false;
    }
    final File classesDirectory = new File(
            getProject().getBuild().getOutputDirectory());
    if (!classesDirectory.exists()) {
      getLog().info(
              "Skipping JaCoCo execution due to missing classes directory:"
                      + classesDirectory);
      return false;
    }
    return true;
  }

  @Override
  public void executeMojo() throws MojoExecutionException {
    if (!canCheckCoverage()) {
      return;
    }
    executeCheck();
  }

  private void executeCheck() throws MojoExecutionException {
    violations = false;

    final ReportSupport support = new ReportSupport(getLog());

    final List<Rule> checkerrules = new ArrayList<Rule>();
    for (final RuleConfiguration r : rules) {
      checkerrules.add(r.rule);
    }
    support.addRulesChecker(checkerrules, this);

    try {
      final IReportVisitor visitor = support.initRootVisitor();
      support.loadExecutionData(dataFile);
      support.processProject(visitor, getProject(), includes, excludes);
      visitor.visitEnd();
    }
    catch (final IOException e) {
      throw new MojoExecutionException(
              "Error while checking code coverage: " + e.getMessage(), e);
    }
    if (violations) {
      if (this.haltOnFailure) {
        throw new MojoExecutionException(CHECK_FAILED);
      }
      else {
        this.getLog().warn(CHECK_FAILED);
      }
    }
    else {
      this.getLog().info(CHECK_SUCCESS);
    }
  }

  public void onViolation(final ICoverageNode node, final Rule rule,
          final Limit limit, final String message) {
    this.getLog().warn(message);
    violations = true;
  }

}
