/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.classpath.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithPackageResources} on a class.
 *
 * @author Andy Wilkinson
 */
@WithPackageResources({ "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" })
class OnClassWithPackageResourcesTests {

  @Test
  void whenWithPackageResourcesIsUsedOnAClassThenResourcesAreAvailable() throws IOException {
    assertThat(new ClassPathResource("resource-1.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
    assertThat(new ClassPathResource("resource-2.txt").getContentAsString()).isEqualTo("two");
    assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString())
            .isEqualTo("three");
  }

}
