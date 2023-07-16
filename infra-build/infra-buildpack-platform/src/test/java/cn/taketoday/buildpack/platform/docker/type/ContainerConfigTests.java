/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ContainerConfig}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class ContainerConfigTests extends AbstractJsonTests {

  @Test
  void ofWhenImageReferenceIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(null, (update) -> {
    })).withMessage("ImageReference must not be null");
  }

  @Test
  void ofWhenUpdateIsNullThrowsException() {
    ImageReference imageReference = ImageReference.of("ubuntu:bionic");
    assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(imageReference, null))
            .withMessage("Update must not be null");
  }

  @Test
  void writeToWritesJson() throws Exception {
    ImageReference imageReference = ImageReference.of("ubuntu:bionic");
    ContainerConfig containerConfig = ContainerConfig.of(imageReference, (update) -> {
      update.withUser("root");
      update.withCommand("ls", "-l");
      update.withArgs("-h");
      update.withLabel("spring", "boot");
      update.withBinding(Binding.from("bind-source", "bind-dest"));
      update.withEnv("name1", "value1");
      update.withEnv("name2", "value2");
      update.withNetworkMode("test");
      update.withSecurityOption("option=value");
    });
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    containerConfig.writeTo(outputStream);
    String actualJson = outputStream.toString(StandardCharsets.UTF_8);
    String expectedJson = StreamUtils.copyToString(getContent("container-config.json"), StandardCharsets.UTF_8);
    JSONAssert.assertEquals(expectedJson, actualJson, true);
  }

}
