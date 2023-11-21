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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RandomString}.
 *
 * @author Phillip Webb
 */
class RandomStringTests {

  @Test
  void generateWhenPrefixIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> RandomString.generate(null, 10))
            .withMessage("Prefix is required");
  }

  @Test
  void generateGeneratesRandomString() {
    String s1 = RandomString.generate("abc-", 10);
    String s2 = RandomString.generate("abc-", 10);
    String s3 = RandomString.generate("abc-", 20);
    assertThat(s1).hasSize(14).startsWith("abc-").isNotEqualTo(s2);
    assertThat(s3).hasSize(24).startsWith("abc-");
  }

}
