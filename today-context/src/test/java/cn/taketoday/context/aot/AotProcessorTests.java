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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.context.aot.AbstractAotProcessor.Settings;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractAotProcessor}.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 4.0
 */
class AotProcessorTests {

  @Test
  void infraAotProcessingIsAvailableInDoProcess(@TempDir Path tempDir) {
    Settings settings = createTestSettings(tempDir);
    assertThat(new AbstractAotProcessor<String>(settings) {
      @Override
      protected String doProcess() {
        assertThat(System.getProperty("infra.aot.processing")).isEqualTo("true");
        return "Hello";
      }
    }.process()).isEqualTo("Hello");
  }

  @Test
  void builderRejectsMissingSourceOutput() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder().build())
            .withMessageContaining("'sourceOutput'");
  }

  @Test
  void builderRejectsMissingResourceOutput(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder().sourceOutput(tempDir).build())
            .withMessageContaining("'resourceOutput'");
  }

  @Test
  void builderRejectsMissingClassOutput(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder()
                    .sourceOutput(tempDir)
                    .resourceOutput(tempDir)
                    .build())
            .withMessageContaining("'classOutput'");
  }

  @Test
  void builderRejectsMissingGroupdId(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder()
                    .sourceOutput(tempDir)
                    .resourceOutput(tempDir)
                    .classOutput(tempDir)
                    .build())
            .withMessageContaining("'groupId'");
  }

  @Test
  void builderRejectsEmptyGroupdId(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder()
                    .sourceOutput(tempDir)
                    .resourceOutput(tempDir)
                    .classOutput(tempDir)
                    .groupId("           ")
                    .build())
            .withMessageContaining("'groupId'");
  }

  @Test
  void builderRejectsMissingArtifactId(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder()
                    .sourceOutput(tempDir)
                    .resourceOutput(tempDir)
                    .classOutput(tempDir)
                    .groupId("my-group")
                    .build())
            .withMessageContaining("'artifactId'");
  }

  @Test
  void builderRejectsEmptyArtifactId(@TempDir Path tempDir) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Settings.builder()
                    .sourceOutput(tempDir)
                    .resourceOutput(tempDir)
                    .classOutput(tempDir)
                    .groupId("my-group")
                    .artifactId("           ")
                    .build())
            .withMessageContaining("'artifactId'");
  }

  @Test
  void builderAcceptsRequiredSettings(@TempDir Path tempDir) {
    Settings settings = Settings.builder()
            .sourceOutput(tempDir)
            .resourceOutput(tempDir)
            .classOutput(tempDir)
            .groupId("my-group")
            .artifactId("my-artifact")
            .build();
    assertThat(settings).isNotNull();
    assertThat(settings.getSourceOutput()).isEqualTo(tempDir);
    assertThat(settings.getResourceOutput()).isEqualTo(tempDir);
    assertThat(settings.getClassOutput()).isEqualTo(tempDir);
    assertThat(settings.getGroupId()).isEqualTo("my-group");
    assertThat(settings.getArtifactId()).isEqualTo("my-artifact");
  }

  private static Settings createTestSettings(Path tempDir) {
    return Settings.builder()
            .sourceOutput(tempDir)
            .resourceOutput(tempDir)
            .classOutput(tempDir)
            .groupId("my-group")
            .artifactId("my-artifact")
            .build();
  }

}
