/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Pilar
 * @author Mahmoud Ben Hassine
 */
class VersionTests {

  private StringWriter writer;

  private CommandContext context;

  @BeforeEach
  void before() {
    writer = new StringWriter();
    context = mock(CommandContext.class);
    when(context.outputWriter()).thenReturn(new PrintWriter(writer));
    when(context.parsedInput()).thenReturn(mock(ParsedInput.class));
  }

  @ParameterizedTest
  @MethodSource("versionWithoutDetailsInfoData")
  void testVersionWithoutDetailsInfo(Version.BuildProperties buildProperties, Version.GitProperties gitProperties)
          throws Exception {
    // given
    Version version = new Version();
    version.setBuildProperties(buildProperties);
    version.setGitProperties(gitProperties);

    // when
    version.execute(context);
    String result = writer.toString();

    // then
    assertThat(result.contains("Version:")).isTrue();
    assertThat(result.contains("Build Version:")).isFalse();
    assertThat(result.contains("Build Group:")).isFalse();
    assertThat(result.contains("Build Artifact:")).isFalse();
    assertThat(result.contains("Build Name:")).isFalse();
    assertThat(result.contains("Build Time:")).isFalse();
    assertThat(result.contains("Git Short Commit ID:")).isFalse();
    assertThat(result.contains("Git Commit ID:")).isFalse();
    assertThat(result.contains("Git Branch:")).isFalse();
    assertThat(result.contains("Git Commit Time:")).isFalse();
  }

  static Stream<Arguments> versionWithoutDetailsInfoData() {
    return Stream.of(Arguments.of(null, null),
            Arguments.of(new Version.BuildProperties(null, null, null, null, null),
                    new Version.GitProperties(null, null, null, null)));
  }

  @Test
  void testVersionWithDetailsInfo() throws Exception {
    // given
    Version.BuildProperties buildProperties = new Version.BuildProperties("group", "artifact", "name", "1.0.0",
            Instant.now());
    Version.GitProperties gitProperties = new Version.GitProperties("branch", "commitId", "shortCommitId",
            Instant.now());

    Version version = new Version();
    version.setBuildProperties(buildProperties);
    version.setGitProperties(gitProperties);

    // when
    version.execute(context);
    String result = writer.toString();

    // then
    assertThat(result.contains("Version: " + buildProperties.version())).isTrue();
    assertThat(result.contains("Build Group: " + buildProperties.group())).isTrue();
    assertThat(result.contains("Build Artifact: " + buildProperties.artifact())).isTrue();
    assertThat(result.contains("Build Name: " + buildProperties.name())).isTrue();
    assertThat(result.contains("Build Time: " + buildProperties.time())).isTrue();
    assertThat(result.contains("Git Short Commit ID: " + gitProperties.shortCommitId())).isTrue();
    assertThat(result.contains("Git Commit ID: " + gitProperties.commitId())).isTrue();
    assertThat(result.contains("Git Branch: " + gitProperties.branch())).isTrue();
    assertThat(result.contains("Git Commit Time: " + gitProperties.commitTime())).isTrue();
  }

}
