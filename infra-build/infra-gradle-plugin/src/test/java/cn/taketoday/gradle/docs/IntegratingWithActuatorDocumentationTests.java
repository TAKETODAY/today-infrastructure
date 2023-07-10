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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import cn.taketoday.gradle.junit.GradleMultiDslExtension;
import cn.taketoday.gradle.testkit.GradleBuild;

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
