/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.aop.testfixture.SerializationTestUtils;
import cn.taketoday.aop.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
public abstract class AbstractRegexpMethodPointcutTests {

  private AbstractRegexpMethodPointcut rpc;

  @BeforeEach
  public void setUp() {
    rpc = getRegexpMethodPointcut();
  }

  protected abstract AbstractRegexpMethodPointcut getRegexpMethodPointcut();

  @Test
  public void testNoPatternSupplied() throws Exception {
    noPatternSuppliedTests(rpc);
  }

  @Test
  public void testSerializationWithNoPatternSupplied() throws Exception {
    rpc = SerializationTestUtils.serializeAndDeserialize(rpc);
    noPatternSuppliedTests(rpc);
  }

  protected void noPatternSuppliedTests(AbstractRegexpMethodPointcut rpc) throws Exception {
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isFalse();
    assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
    assertThat(rpc.getPatterns().length).isEqualTo(0);
  }

  @Test
  public void testExactMatch() throws Exception {
    rpc.setPattern("java.lang.Object.hashCode");
    exactMatchTests(rpc);
    rpc = SerializationTestUtils.serializeAndDeserialize(rpc);
    exactMatchTests(rpc);
  }

  protected void exactMatchTests(AbstractRegexpMethodPointcut rpc) throws Exception {
    // assumes rpc.setPattern("java.lang.Object.hashCode");
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isTrue();
    assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
  }

  @Test
  public void testSpecificMatch() throws Exception {
    rpc.setPattern("java.lang.String.hashCode");
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isFalse();
  }

  @Test
  public void testWildcard() throws Exception {
    rpc.setPattern(".*Object.hashCode");
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isTrue();
    assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
  }

  @Test
  public void testWildcardForOneClass() throws Exception {
    rpc.setPattern("java.lang.Object.*");
    assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
    assertThat(rpc.matches(Object.class.getMethod("wait"), String.class)).isTrue();
  }

  @Test
  public void testMatchesObjectClass() throws Exception {
    rpc.setPattern("java.lang.Object.*");
    assertThat(rpc.matches(Exception.class.getMethod("hashCode"), IOException.class)).isTrue();
    // Doesn't match a method from Throwable
    assertThat(rpc.matches(Exception.class.getMethod("getMessage"), Exception.class)).isFalse();
  }

  @Test
  public void testWithExclusion() throws Exception {
    this.rpc.setPattern(".*get.*");
    this.rpc.setExcludedPattern(".*Age.*");
    assertThat(this.rpc.matches(TestBean.class.getMethod("getName"), TestBean.class)).isTrue();
    assertThat(this.rpc.matches(TestBean.class.getMethod("getAge"), TestBean.class)).isFalse();
  }

}
