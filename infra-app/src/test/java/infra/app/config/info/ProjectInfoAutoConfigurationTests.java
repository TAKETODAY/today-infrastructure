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

package infra.app.config.info;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Properties;

import infra.app.config.context.PropertyPlaceholderAutoConfiguration;
import infra.app.info.BuildProperties;
import infra.app.info.GitProperties;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectInfoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ProjectInfoAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
          AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, ProjectInfoAutoConfiguration.class));

  @Test
  void gitPropertiesUnavailableIfResourceNotAvailable() {
    this.contextRunner.run((context) -> assertThat(context.getBeansOfType(GitProperties.class)).isEmpty());
  }

  @Test
  void gitPropertiesWithNoData() {
    this.contextRunner
            .withPropertyValues("project.info.git.location="
                    + "classpath:/infra/app/config/info/git-no-data.properties")
            .run((context) -> {
              GitProperties gitProperties = context.getBean(GitProperties.class);
              assertThat(gitProperties.getBranch()).isNull();
            });
  }

  @Test
  void gitPropertiesFallbackWithGitPropertiesBean() {
    this.contextRunner.withUserConfiguration(CustomInfoPropertiesConfiguration.class)
            .withPropertyValues(
                    "project.info.git.location=classpath:/infra/app/config/info/git.properties")
            .run((context) -> {
              GitProperties gitProperties = context.getBean(GitProperties.class);
              assertThat(gitProperties).isSameAs(context.getBean("customGitProperties"));
            });
  }

  @Test
  void gitPropertiesUsesUtf8ByDefault() {
    this.contextRunner
            .withPropertyValues(
                    "project.info.git.location=classpath:/infra/app/config/info/git.properties")
            .run((context) -> {
              GitProperties gitProperties = context.getBean(GitProperties.class);
              assertThat(gitProperties.get("commit.charset")).isEqualTo("test™");
            });
  }

  @Test
  void gitPropertiesEncodingCanBeConfigured() {
    this.contextRunner
            .withPropertyValues("project.info.git.encoding=US-ASCII",
                    "project.info.git.location=classpath:/infra/app/config/info/git.properties")
            .run((context) -> {
              GitProperties gitProperties = context.getBean(GitProperties.class);
              assertThat(gitProperties.get("commit.charset")).isNotEqualTo("test™");
            });
  }

  @Test
  @WithResource(name = "META-INF/build-info.properties", content = """
          build.group=com.example
          build.artifact=demo
          build.name=Demo Project
          build.version=0.0.1-SNAPSHOT
          build.time=2016-03-04T14:16:05.000Z
          """)
  void buildPropertiesDefaultLocation() {
    this.contextRunner.run((context) -> {
      BuildProperties buildProperties = context.getBean(BuildProperties.class);
      assertThat(buildProperties.getGroup()).isEqualTo("com.example");
      assertThat(buildProperties.getArtifact()).isEqualTo("demo");
      assertThat(buildProperties.getName()).isEqualTo("Demo Project");
      assertThat(buildProperties.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
      Instant time = buildProperties.getTime();
      assertThat(time).isNotNull();
      assertThat(time.toEpochMilli()).isEqualTo(1457100965000L);
    });
  }

  @Test
  void buildPropertiesCustomLocation() {
    this.contextRunner.withPropertyValues("project.info.build.location="
                    + "classpath:/infra/app/config/info/build-info.properties")
            .run((context) -> {
              BuildProperties buildProperties = context.getBean(BuildProperties.class);
              assertThat(buildProperties.getGroup()).isEqualTo("com.example.acme");
              assertThat(buildProperties.getArtifact()).isEqualTo("acme");
              assertThat(buildProperties.getName()).isEqualTo("acme");
              assertThat(buildProperties.getVersion()).isEqualTo("1.0.1-SNAPSHOT");
              Instant time = buildProperties.getTime();
              assertThat(time).isNotNull();
              assertThat(time.toEpochMilli()).isEqualTo(1457088120000L);
            });
  }

  @Test
  void buildPropertiesCustomInvalidLocation() {
    this.contextRunner.withPropertyValues("project.info.build.location=classpath:/org/acme/no-build-info.properties")
            .run((context) -> assertThat(context.getBeansOfType(BuildProperties.class)).isEmpty());
  }

  @Test
  void buildPropertiesFallbackWithBuildInfoBean() {
    this.contextRunner.withUserConfiguration(CustomInfoPropertiesConfiguration.class).run((context) -> {
      BuildProperties buildProperties = context.getBean(BuildProperties.class);
      assertThat(buildProperties).isSameAs(context.getBean("customBuildProperties"));
    });
  }

  @Test
  void buildPropertiesUsesUtf8ByDefault() {
    this.contextRunner.withPropertyValues(
                    "project.info.build.location=classpath:/infra/app/config/info/build-info.properties")
            .run((context) -> {
              BuildProperties buildProperties = context.getBean(BuildProperties.class);
              assertThat(buildProperties.get("charset")).isEqualTo("test™");
            });
  }

  @Test
  void buildPropertiesEncodingCanBeConfigured() {
    this.contextRunner.withPropertyValues("project.info.build.encoding=US-ASCII",
                    "project.info.build.location=classpath:/infra/app/config/info/build-info.properties")
            .run((context) -> {
              BuildProperties buildProperties = context.getBean(BuildProperties.class);
              assertThat(buildProperties.get("charset")).isNotEqualTo("test™");
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomInfoPropertiesConfiguration {

    @Bean
    GitProperties customGitProperties() {
      return new GitProperties(new Properties());
    }

    @Bean
    BuildProperties customBuildProperties() {
      return new BuildProperties(new Properties());
    }

  }

}
