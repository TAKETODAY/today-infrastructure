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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;
import cn.taketoday.test.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfraBuildImage} tasks requiring a Docker image registry.
 *
 * @author Scott Frederick
 */
@GradleCompatibility
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Disabled until differences between running locally and in CI can be diagnosed")
class InfraBuildImageRegistryIntegrationTests {

  @Container
  static final RegistryContainer registry = new RegistryContainer().withStartupAttempts(5)
          .withStartupTimeout(Duration.ofMinutes(3));

  String registryAddress;

  GradleBuild gradleBuild;

  @BeforeEach
  void setUp() {
    assertThat(registry.isRunning()).isTrue();
    this.registryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
  }

  @TestTemplate
  void buildsImageAndPublishesToRegistry() throws IOException {
    writeMainClass();
    String repoName = "test-image";
    String imageName = this.registryAddress + "/" + repoName;
    BuildResult result = this.gradleBuild.build("infraBuildImage", "--imageName=" + imageName);
    assertThat(result.task(":infraBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("Building image")
            .contains("Successfully built image")
            .contains("Pushing image '" + imageName + ":latest" + "'")
            .contains("Pushed image '" + imageName + ":latest" + "'");
    ImageReference imageReference = ImageReference.of(imageName);
    Image pulledImage = new DockerApi().image().pull(imageReference, UpdateListener.none());
    assertThat(pulledImage).isNotNull();
    new DockerApi().image().remove(imageReference, false);
  }

  private void writeMainClass() {
    File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
    examplePackage.mkdirs();
    File main = new File(examplePackage, "Main.java");
    try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
      writer.println("package example;");
      writer.println();
      writer.println("import java.io.IOException;");
      writer.println();
      writer.println("public class Main {");
      writer.println();
      writer.println("    public static void main(String[] args) {");
      writer.println("    }");
      writer.println();
      writer.println("}");
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static class RegistryContainer extends GenericContainer<RegistryContainer> {

    RegistryContainer() {
      super(DockerImageNames.registry());
      addExposedPorts(5000);
      addEnv("SERVER_NAME", "localhost");
    }

  }

}
