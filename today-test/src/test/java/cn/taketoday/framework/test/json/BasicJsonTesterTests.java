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

package cn.taketoday.framework.test.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BasicJsonTester}.
 *
 * @author Phillip Webb
 */
public class BasicJsonTesterTests {

  private static final String JSON = "{\"spring\":[\"boot\",\"framework\"]}";

  private final BasicJsonTester json = new BasicJsonTester(getClass());

  @Test
  void createWhenResourceLoadClassIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new BasicJsonTester(null))
            .withMessageContaining("ResourceLoadClass must not be null");
  }

  @Test
  void fromJsonStringShouldReturnJsonContent() {
    assertThat(this.json.from(JSON)).isEqualToJson("source.json");
  }

  @Test
  void fromResourceStringShouldReturnJsonContent() {
    assertThat(this.json.from("source.json")).isEqualToJson(JSON);
  }

  @Test
  void fromResourceStringWithClassShouldReturnJsonContent() {
    assertThat(this.json.from("source.json", getClass())).isEqualToJson(JSON);
  }

  @Test
  void fromByteArrayShouldReturnJsonContent() {
    assertThat(this.json.from(JSON.getBytes())).isEqualToJson("source.json");
  }

  @Test
  void fromFileShouldReturnJsonContent(@TempDir Path temp) throws Exception {
    File file = new File(temp.toFile(), "file.json");
    FileCopyUtils.copy(JSON.getBytes(), file);
    assertThat(this.json.from(file)).isEqualToJson("source.json");
  }

  @Test
  void fromInputStreamShouldReturnJsonContent() {
    InputStream inputStream = new ByteArrayInputStream(JSON.getBytes());
    assertThat(this.json.from(inputStream)).isEqualToJson("source.json");
  }

  @Test
  void fromResourceShouldReturnJsonContent() {
    Resource resource = new ByteArrayResource(JSON.getBytes());
    assertThat(this.json.from(resource)).isEqualToJson("source.json");
  }

}
