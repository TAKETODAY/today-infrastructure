/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's {@code test-run} goal.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class TestRunIntegrationTests {

  @TestTemplate
  void whenTheTestRunGoalIsExecutedTheApplicationIsRunWithTestAndMainClassesAndTestClasspath(MavenBuild mavenBuild) {
    mavenBuild.project("test-run")
            .goals("infra:test-run", "-X")
            .execute((project) -> assertThat(buildLog(project))
                    .contains("Main class name = org.test.TestSampleApplication")
                    .contains("1. " + canonicalPathOf(project, "target/test-classes"))
                    .contains("2. " + canonicalPathOf(project, "target/classes"))
                    .containsPattern("3\\. .*today-core")
                    .containsPattern("4\\. .*spring-jcl"));
  }

  private String canonicalPathOf(File project, String path) throws IOException {
    return new File(project, path).getCanonicalPath();
  }

  private String buildLog(File project) {
    return contentOf(new File(project, "target/build.log"));
  }

}
