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

package cn.taketoday.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.gradle.junit.GradleMultiDslExtension;
import cn.taketoday.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the managing dependencies documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class ManagingDependenciesDocumentationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void dependenciesExampleEvaluatesSuccessfully() {
    this.gradleBuild.script("src/docs/gradle/managing-dependencies/dependencies").build();
  }

  @TestTemplate
  void customManagedVersions() {
    assertThat(this.gradleBuild.script("src/docs/gradle/managing-dependencies/custom-version")
            .build("slf4jVersion")
            .getOutput()).contains("1.7.20");
  }

  @TestTemplate
  void dependencyManagementInIsolation() {
    assertThat(this.gradleBuild.script("src/docs/gradle/managing-dependencies/configure-bom")
            .build("dependencyManagement")
            .getOutput()).contains("cn.taketoday:today-starter ");
  }

  @TestTemplate
  void configurePlatform() {
    assertThat(this.gradleBuild.script("src/docs/gradle/managing-dependencies/configure-platform")
            .build("dependencies", "--configuration", "compileClasspath")
            .getOutput()).contains("cn.taketoday:today-starter ");
  }

  @TestTemplate
  void customManagedVersionsWithPlatform() {
    assertThat(this.gradleBuild.script("src/docs/gradle/managing-dependencies/custom-version-with-platform")
            .build("dependencies", "--configuration", "compileClasspath")
            .getOutput()).contains("1.7.20");
  }

}
