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
 * Tests for the publishing documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class PublishingDocumentationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void mavenPublish() {
    assertThat(this.gradleBuild.script("src/docs/gradle/publishing/maven-publish")
            .build("publishingConfiguration")
            .getOutput()).contains("MavenPublication").contains("https://repo.example.com");
  }

}
