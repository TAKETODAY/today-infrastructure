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

package cn.taketoday.buildpack.platform.io;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultOwner}.
 *
 * @author Phillip Webb
 */
class DefaultOwnerTests {

  @Test
  void getUidReturnsUid() {
    DefaultOwner owner = new DefaultOwner(123, 456);
    assertThat(owner.getUid()).isEqualTo(123);
  }

  @Test
  void getGidReturnsGid() {
    DefaultOwner owner = new DefaultOwner(123, 456);
    assertThat(owner.getGid()).isEqualTo(456);
  }

  @Test
  void toStringReturnsString() {
    DefaultOwner owner = new DefaultOwner(123, 456);
    assertThat(owner).hasToString("123/456");
  }

}
