/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.build;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testretry.TestRetryPlugin;
import org.gradle.testretry.TestRetryTaskExtension;

import java.util.Map;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 * <ul>
 * <li>The {@link TestRetryPlugin Test Retry} plugin is applied so that flaky tests
 * are retried 3 times when running on the CI server.
 * </ul>
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TestConventions {

  void apply(Project project) {
    project.getPlugins().withType(JavaBasePlugin.class, (java) -> configureTestConventions(project));
  }

  private void configureTestConventions(Project project) {
    project.getTasks().withType(Test.class,
            test -> {
              configureTests(project, test);
              configureTestRetryPlugin(project, test);
            });
  }

  private void configureTests(Project project, Test test) {
    test.useJUnitPlatform();
    test.include("**/*Tests.class", "**/*Test.class");
    test.setSystemProperties(Map.of(
            "java.awt.headless", "true",
            "io.netty.leakDetection.level", "paranoid",
            "io.netty5.leakDetectionLevel", "paranoid",
            "io.netty5.leakDetection.targetRecords", "32",
            "io.netty5.buffer.lifecycleTracingEnabled", "true"
    ));
    if (project.hasProperty("testGroups")) {
      test.systemProperty("testGroups", project.getProperties().get("testGroups"));
    }
    test.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "-Djava.locale.providers=COMPAT");
  }

  private void configureTestRetryPlugin(Project project, Test test) {
    project.getPlugins().withType(TestRetryPlugin.class, testRetryPlugin -> {
      TestRetryTaskExtension testRetry = test.getExtensions().getByType(TestRetryTaskExtension.class);
      testRetry.getFailOnPassedAfterRetry().set(true);
      testRetry.getMaxRetries().set(isCi() ? 3 : 0);
    });
  }

  private boolean isCi() {
    return Boolean.parseBoolean(System.getenv("CI"));
  }

}
