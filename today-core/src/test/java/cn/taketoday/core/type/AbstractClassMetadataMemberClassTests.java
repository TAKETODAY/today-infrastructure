/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing implementations of
 * {@link ClassMetadata#getMemberClassNames()}.
 *
 * @author Chris Beams
 * @since 4.0
 */
public abstract class AbstractClassMetadataMemberClassTests {

  protected abstract ClassMetadata getClassMetadataFor(Class<?> clazz);

  @Test
  void withNoMemberClasses() {
    ClassMetadata metadata = getClassMetadataFor(L0_a.class);
    String[] nestedClasses = metadata.getMemberClassNames();
    assertThat(nestedClasses).isEmpty();
  }

  @Test
  void withPublicMemberClasses() {
    ClassMetadata metadata = getClassMetadataFor(L0_b.class);
    String[] nestedClasses = metadata.getMemberClassNames();
    assertThat(nestedClasses).containsOnly(L0_b.L1.class.getName());
  }

  @Test
  void withNonPublicMemberClasses() {
    ClassMetadata metadata = getClassMetadataFor(L0_c.class);
    String[] nestedClasses = metadata.getMemberClassNames();
    assertThat(nestedClasses).containsOnly(L0_c.L1.class.getName());
  }

  @Test
  void againstMemberClass() {
    ClassMetadata metadata = getClassMetadataFor(L0_b.L1.class);
    String[] nestedClasses = metadata.getMemberClassNames();
    assertThat(nestedClasses).isEmpty();
  }

  public static class L0_a {
  }

  public static class L0_b {
    public static class L1 { }
  }

  public static class L0_c {
    private static class L1 { }
  }

}
