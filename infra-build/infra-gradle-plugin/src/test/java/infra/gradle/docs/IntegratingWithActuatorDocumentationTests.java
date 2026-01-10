/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import infra.gradle.junit.GradleMultiDslExtension;
import infra.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the generating build info documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class IntegratingWithActuatorDocumentationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void basicBuildInfo() {
    this.gradleBuild.script("src/docs/gradle/integrating-with-actuator/build-info-basic").build("infraBuildInfo");
    assertThat(new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties"))
            .isFile();
  }

  @TestTemplate
  void buildInfoCustomValues() {
    this.gradleBuild.script("src/docs/gradle/integrating-with-actuator/build-info-custom-values")
            .build("infraBuildInfo");
    File file = new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties");
    assertThat(file).isFile();
    Properties properties = buildInfoProperties(file);
    assertThat(properties).containsEntry("build.artifact", "example-app");
    assertThat(properties).containsEntry("build.version", "1.2.3");
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.name", "Example application");
    assertThat(properties).containsKey("build.time");
  }

  @TestTemplate
  void buildInfoAdditional() {
    this.gradleBuild.script("src/docs/gradle/integrating-with-actuator/build-info-additional")
            .build("infraBuildInfo");
    File file = new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties");
    assertThat(file).isFile();
    Properties properties = buildInfoProperties(file);
    assertThat(properties).containsEntry("build.a", "alpha");
    assertThat(properties).containsEntry("build.b", "bravo");
  }

  @TestTemplate
  void buildInfoExcludeTime() {
    this.gradleBuild.script("src/docs/gradle/integrating-with-actuator/build-info-exclude-time")
            .build("infraBuildInfo");
    File file = new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties");
    assertThat(file).isFile();
    Properties properties = buildInfoProperties(file);
    assertThat(properties).doesNotContainKey("build.time");
  }

  private Properties buildInfoProperties(File file) {
    assertThat(file).isFile();
    Properties properties = new Properties();
    try (FileReader reader = new FileReader(file)) {
      properties.load(reader);
      return properties;
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
