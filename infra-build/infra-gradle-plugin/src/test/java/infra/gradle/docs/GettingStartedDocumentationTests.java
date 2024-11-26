/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.gradle.junit.GradleMultiDslExtension;
import infra.gradle.testkit.GradleBuild;

/**
 * Tests for the getting started documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class GettingStartedDocumentationTests {

  GradleBuild gradleBuild;

  // NOTE: We can't run any 'apply-plugin' tests because during a release the
  // jar won't be there

  @TestTemplate
  void typicalPluginsAppliesExceptedPlugins() {
    this.gradleBuild.script("src/docs/gradle/getting-started/typical-plugins").build("verify");
  }

}
