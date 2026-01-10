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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import infra.gradle.junit.GradleMultiDslExtension;
import infra.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the documentation about running a Infra application.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class RunningDocumentationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void infraRunMain() throws IOException {
    writeMainClass();
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-run-main").build("infraRun").getOutput())
            .contains("com.example.ExampleApplication");
  }

  @TestTemplate
  void applicationPluginMainClassName() throws IOException {
    writeMainClass();
    assertThat(this.gradleBuild.script("src/docs/gradle/running/application-plugin-main-class-name")
            .build("infraRun")
            .getOutput()).contains("com.example.ExampleApplication");
  }

  @TestTemplate
  void infraApplicationDslMainClassName() throws IOException {
    writeMainClass();
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-app-dsl-main-class-name")
            .build("infraRun")
            .getOutput()).contains("com.example.ExampleApplication");
  }

  @TestTemplate
  void infraRunSourceResources() {
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-run-source-resources")
            .build("configuredClasspath")
            .getOutput()).contains(new File("src/main/resources").getPath());
  }

  @TestTemplate
  void infraRunDisableOptimizedLaunch() {
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-run-disable-optimized-launch")
            .build("optimizedLaunch")
            .getOutput()).contains("false");
  }

  @TestTemplate
  void infraRunSystemPropertyDefaultValue() {
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-run-system-property")
            .build("configuredSystemProperties")
            .getOutput()).contains("com.example.property = default");
  }

  @TestTemplate
  void infraRunSystemProperty() {
    assertThat(this.gradleBuild.script("src/docs/gradle/running/infra-run-system-property")
            .build("-Pexample=custom", "configuredSystemProperties")
            .getOutput()).contains("com.example.property = custom");
  }

  private void writeMainClass() throws IOException {
    File exampleApplication = new File(this.gradleBuild.getProjectDir(),
            "src/main/java/com/example/ExampleApplication.java");
    exampleApplication.getParentFile().mkdirs();
    try (PrintWriter writer = new PrintWriter(new FileWriter(exampleApplication))) {
      writer.println("package com.example;");
      writer.println("public class ExampleApplication {");
      writer.println("    public static void main(String[] args) {");
      writer.println("        System.out.println(ExampleApplication.class.getName());");
      writer.println("    }");
      writer.println("}");
    }
  }

}
