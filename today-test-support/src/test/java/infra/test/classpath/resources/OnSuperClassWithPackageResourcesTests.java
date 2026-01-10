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

package infra.test.classpath.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithPackageResources} when used on a super-class.
 *
 * @author Andy Wilkinson
 */
class OnSuperClassWithPackageResourcesTests extends WithPackageResourcesClass {

  @Test
  void whenWithPackageResourcesIsUsedOnASuperClassThenResourcesAreAvailable() throws IOException {
    assertThat(new ClassPathResource("resource-1.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
    assertThat(new ClassPathResource("resource-2.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("two");
    assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString(StandardCharsets.UTF_8))
            .isEqualTo("three");
  }

}
