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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's war support.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class StartStopIntegrationTests {

  @TestTemplate
  void startStopWaitsForApplicationToBeReadyAndThenRequestsShutdown(MavenBuild mavenBuild) {
    mavenBuild.project("start-stop")
            .goals("verify")
            .execute((project) -> assertThat(buildLog(project)).contains("isReady: true")
                    .contains("Shutdown requested"));
  }

  @TestTemplate
  void whenSkipIsTrueStartAndStopAreSkipped(MavenBuild mavenBuild) {
    mavenBuild.project("start-stop-skip")
            .goals("verify")
            .execute((project) -> assertThat(buildLog(project)).doesNotContain("Ooops, I haz been run")
                    .doesNotContain("Stopping application"));
  }

  private String buildLog(File project) {
    return contentOf(new File(project, "target/build.log"));
  }

}
