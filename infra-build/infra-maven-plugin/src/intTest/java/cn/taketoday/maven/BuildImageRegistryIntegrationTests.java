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

package cn.taketoday.maven;

import com.github.dockerjava.api.DockerClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.buildpack.platform.docker.DockerApi;
import cn.taketoday.buildpack.platform.docker.UpdateListener;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import cn.taketoday.test.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support using a Docker image registry.
 *
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Disabled until differences between running locally and in CI can be diagnosed")
class BuildImageRegistryIntegrationTests extends AbstractArchiveIntegrationTests {

  @Container
  static final RegistryContainer registry = new RegistryContainer().withStartupAttempts(5)
          .withStartupTimeout(Duration.ofMinutes(3));

  DockerClient dockerClient;

  String registryAddress;

  @BeforeEach
  void setUp() {
    assertThat(registry.isRunning()).isTrue();
    this.dockerClient = registry.getDockerClient();
    this.registryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithPublish(MavenBuild mavenBuild) {
    String repoName = "test-image";
    String imageName = this.registryAddress + "/" + repoName;
    mavenBuild.project("build-image-publish")
            .goals("package")
            .systemProperty("infra.build-image.imageName", imageName)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("Successfully built image")
                      .contains("Pushing image '" + imageName + ":latest" + "'")
                      .contains("Pushed image '" + imageName + ":latest" + "'");
              ImageReference imageReference = ImageReference.of(imageName);
              DockerApi.ImageApi imageApi = new DockerApi().image();
              Image pulledImage = imageApi.pull(imageReference, UpdateListener.none());
              assertThat(pulledImage).isNotNull();
              imageApi.remove(imageReference, false);
            });
  }

  private static class RegistryContainer extends GenericContainer<RegistryContainer> {

    RegistryContainer() {
      super(DockerImageNames.registry());
      addExposedPorts(5000);
      addEnv("SERVER_NAME", "localhost");
    }

  }

}
