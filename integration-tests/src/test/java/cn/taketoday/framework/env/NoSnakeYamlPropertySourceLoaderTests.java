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

package cn.taketoday.framework.env;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link YamlPropertySourceLoader} when snakeyaml is not available.
 *
 * @author Madhura Bhave
 */
@ClassPathExclusions("snakeyaml-*.jar")
class NoSnakeYamlPropertySourceLoaderTests {

  private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

  @Test
  void load() {
    ByteArrayResource resource = new ByteArrayResource("foo:\n  bar: spam".getBytes());
    assertThatIllegalStateException().isThrownBy(() -> this.loader.load("resource", resource))
            .withMessageContaining("Attempted to load resource but snakeyaml was not found on the classpath");
  }

}
