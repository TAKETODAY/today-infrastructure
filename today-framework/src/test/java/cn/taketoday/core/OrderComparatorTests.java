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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/12 21:58
 */
class OrderComparatorTests {

  private final OrderComparator comparator = new OrderComparator();

  @Test
  void compareOrderedInstancesBefore() {
    assertThat(this.comparator.compare(new StubOrdered(100), new StubOrdered(2000))).isEqualTo(-1);
  }

  @Test
  void compareOrderedInstancesSame() {
    assertThat(this.comparator.compare(new StubOrdered(100), new StubOrdered(100))).isEqualTo(0);
  }

  @Test
  void compareOrderedInstancesAfter() {
    assertThat(this.comparator.compare(new StubOrdered(982300), new StubOrdered(100))).isEqualTo(1);
  }

  @Test
  void compareOrderedInstancesNullFirst() {
    assertThat(this.comparator.compare(null, new StubOrdered(100))).isEqualTo(1);
  }

  @Test
  void compareOrderedInstancesNullLast() {
    assertThat(this.comparator.compare(new StubOrdered(100), null)).isEqualTo(-1);
  }

  @Test
  void compareOrderedInstancesDoubleNull() {
    assertThat(this.comparator.compare(null, null)).isEqualTo(0);
  }

  @Test
  void compareTwoNonOrderedInstancesEndsUpAsSame() {
    assertThat(this.comparator.compare(new Object(), new Object())).isEqualTo(0);
  }

  @Test
  void comparePriorityOrderedInstancesBefore() {
    assertThat(this.comparator.compare(new StubPriorityOrdered(100), new StubPriorityOrdered(2000))).isEqualTo(-1);
  }

  @Test
  void comparePriorityOrderedInstancesSame() {
    assertThat(this.comparator.compare(new StubPriorityOrdered(100), new StubPriorityOrdered(100))).isEqualTo(0);
  }

  @Test
  void comparePriorityOrderedInstancesAfter() {
    assertThat(this.comparator.compare(new StubPriorityOrdered(982300), new StubPriorityOrdered(100))).isEqualTo(1);
  }

  @Test
  void comparePriorityOrderedInstanceToStandardOrderedInstanceWithHigherPriority() {
    assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(200), new StubOrdered(100));
  }

  @Test
  void comparePriorityOrderedInstanceToStandardOrderedInstanceWithSamePriority() {
    assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(100), new StubOrdered(100));
  }

  @Test
  void comparePriorityOrderedInstanceToStandardOrderedInstanceWithLowerPriority() {
    assertThatPriorityOrderedAlwaysWins(new StubPriorityOrdered(100), new StubOrdered(200));
  }

  private void assertThatPriorityOrderedAlwaysWins(StubPriorityOrdered priority, StubOrdered standard) {
    assertThat(this.comparator.compare(priority, standard)).isEqualTo(-1);
    assertThat(this.comparator.compare(standard, priority)).isEqualTo(1);
  }

  @Test
  void compareWithSimpleSourceProvider() {
    Comparator<Object> customComparator = this.comparator.withSourceProvider(
            new TestSourceProvider(5L, new StubOrdered(25)));
    assertThat(customComparator.compare(new StubOrdered(10), 5L)).isEqualTo(-1);
  }

  @Test
  void compareWithSourceProviderArray() {
    Comparator<Object> customComparator = this.comparator.withSourceProvider(
            new TestSourceProvider(5L, new Object[] { new StubOrdered(10), new StubOrdered(-25) }));
    assertThat(customComparator.compare(5L, new Object())).isEqualTo(-1);
  }

  @Test
  void compareWithSourceProviderArrayNoMatch() {
    Comparator<Object> customComparator = this.comparator.withSourceProvider(
            new TestSourceProvider(5L, new Object[] { new Object(), new Object() }));
    assertThat(customComparator.compare(new Object(), 5L)).isEqualTo(0);
  }

  @Test
  void compareWithSourceProviderEmpty() {
    Comparator<Object> customComparator = this.comparator.withSourceProvider(
            new TestSourceProvider(50L, new Object()));
    assertThat(customComparator.compare(new Object(), 5L)).isEqualTo(0);
  }

  private static class StubOrdered implements Ordered {

    private final int order;

    StubOrdered(int order) {
      this.order = order;
    }

    @Override
    public int getOrder() {
      return this.order;
    }
  }

  private static class StubPriorityOrdered implements PriorityOrdered {

    private final int order;

    StubPriorityOrdered(int order) {
      this.order = order;
    }

    @Override
    public int getOrder() {
      return this.order;
    }
  }

  private static class TestSourceProvider implements OrderSourceProvider {

    private final Object target;

    private final Object orderSource;

    TestSourceProvider(Object target, Object orderSource) {
      this.target = target;
      this.orderSource = orderSource;
    }

    @Override
    public Object getOrderSource(Object obj) {
      if (target.equals(obj)) {
        return orderSource;
      }
      return null;
    }
  }

}
