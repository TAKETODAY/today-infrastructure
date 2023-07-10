/*
 * Copyright 2012 - 2023 the original author or authors.
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

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's run goal.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class RunIntegrationTests {

  @TestTemplate
  void whenTheRunGoalIsExecutedTheApplicationIsForkedWithOptimizedJvmArguments(MavenBuild mavenBuild) {
    mavenBuild.project("run").goals("infra:run", "-X").execute((project) -> {
      String jvmArguments = "JVM argument(s): -XX:TieredStopAtLevel=1";
      assertThat(buildLog(project)).contains("I haz been run").contains(jvmArguments);
    });
  }

  @TestTemplate
  void whenEnvironmentVariablesAreConfiguredTheyAreAvailableToTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-envargs")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenExclusionsAreConfiguredExcludedDependenciesDoNotAppearOnTheClasspath(MavenBuild mavenBuild) {
    mavenBuild.project("run-exclude")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenSystemPropertiesAndJvmArgumentsAreConfiguredTheyAreAvailableToTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-jvm-system-props")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenJvmArgumentsAreConfiguredTheyAreAvailableToTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-jvmargs")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenCommandLineSpecifiesJvmArgumentsTheyAreAvailableToTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-jvmargs-commandline")
            .goals("infra:run")
            .systemProperty("infra.run.jvmArguments", "-Dfoo=value-from-cmd")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenPomAndCommandLineSpecifyJvmArgumentsThenPomOverrides(MavenBuild mavenBuild) {
    mavenBuild.project("run-jvmargs")
            .goals("infra:run")
            .systemProperty("infra.run.jvmArguments", "-Dfoo=value-from-cmd")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenProfilesAreConfiguredTheyArePassedToTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-profiles")
            .goals("infra:run", "-X")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run with profile(s) 'foo,bar'"));
  }

  @TestTemplate
  void whenUseTestClasspathIsEnabledTheApplicationHasTestDependenciesOnItsClasspath(MavenBuild mavenBuild) {
    mavenBuild.project("run-use-test-classpath")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
  }

  @TestTemplate
  void whenAWorkingDirectoryIsConfiguredTheApplicationIsRunFromThatDirectory(MavenBuild mavenBuild) {
    mavenBuild.project("run-working-directory")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project)).containsPattern("I haz been run from.*src.main.java"));
  }

  @TestTemplate
  @DisabledOnOs(OS.WINDOWS)
  void whenAToolchainIsConfiguredItIsUsedToRunTheApplication(MavenBuild mavenBuild) {
    mavenBuild.project("run-toolchains")
            .goals("verify", "-t", "toolchains.xml")
            .execute((project) -> assertThat(buildLog(project)).contains("The Maven Toolchains is awesome!"));
  }

  @TestTemplate
  void whenPomSpecifiesRunArgumentsContainingCommasTheyArePassedToTheApplicationCorrectly(MavenBuild mavenBuild) {
    mavenBuild.project("run-arguments")
            .goals("infra:run")
            .execute((project) -> assertThat(buildLog(project))
                    .contains("I haz been run with profile(s) 'foo,bar' and endpoint(s) 'prometheus,info'"));
  }

  @TestTemplate
  void whenCommandLineSpecifiesRunArgumentsContainingCommasTheyArePassedToTheApplicationCorrectly(
          MavenBuild mavenBuild) {
    mavenBuild.project("run-arguments-commandline")
            .goals("infra:run")
            .systemProperty("infra.run.arguments",
                    "--management.endpoints.web.exposure.include=prometheus,info,health,metrics --infra.profiles.active=foo,bar")
            .execute((project) -> assertThat(buildLog(project))
                    .contains("I haz been run with profile(s) 'foo,bar' and endpoint(s) 'prometheus,info,health,metrics'"));
  }

  @TestTemplate
  void whenPomAndCommandLineSpecifyRunArgumentsThenPomOverrides(MavenBuild mavenBuild) {
    mavenBuild.project("run-arguments")
            .goals("infra:run")
            .systemProperty("infra.run.arguments",
                    "--management.endpoints.web.exposure.include=one,two,three --infra.profiles.active=test")
            .execute((project) -> assertThat(buildLog(project))
                    .contains("I haz been run with profile(s) 'foo,bar' and endpoint(s) 'prometheus,info'"));
  }

  private String buildLog(File project) {
    return contentOf(new File(project, "target/build.log"));
  }

}
