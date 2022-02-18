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

package cn.taketoday.aop.support;



import org.junit.jupiter.api.Test;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.beans.testfixture.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RootClassFilter}.
 *
 * @author Sam Brannen
 */
public class RootClassFilterTests {

  private final ClassFilter filter1 = new RootClassFilter(Exception.class);
  private final ClassFilter filter2 = new RootClassFilter(Exception.class);
  private final ClassFilter filter3 = new RootClassFilter(ITestBean.class);

  @Test
  public void matches() {
    assertThat(filter1.matches(Exception.class)).isTrue();
    assertThat(filter1.matches(RuntimeException.class)).isTrue();
    assertThat(filter1.matches(Error.class)).isFalse();
  }

  @Test
  public void testEquals() {
    assertThat(filter1).isEqualTo(filter2);
    assertThat(filter1).isNotEqualTo(filter3);
  }

  @Test
  public void testHashCode() {
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    assertThat(filter1.hashCode()).isNotEqualTo(filter3.hashCode());
  }

  @Test
  public void testToString() {
    assertThat(filter1.toString()).isEqualTo("cn.taketoday.aop.support.RootClassFilter: java.lang.Exception");
    assertThat(filter1.toString()).isEqualTo(filter2.toString());
  }

}
