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

package cn.taketoday.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cn.taketoday.gradle.junit.GradleMultiDslExtension;
import cn.taketoday.gradle.testkit.GradleBuild;

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
