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

package infra.app.env;

import org.junit.jupiter.api.Test;

import infra.core.io.ByteArrayResource;
import infra.test.classpath.ClassPathExclusions;

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
