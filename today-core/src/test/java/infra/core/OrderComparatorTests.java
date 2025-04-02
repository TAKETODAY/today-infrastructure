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

package infra.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  @Test
  void orderSourceProviderIsUsedForOrdering() {
    var comparable = new OrderComparator().withSourceProvider(obj -> {
      if (obj instanceof String) {
        return new StubOrdered(1);
      }
      if (obj instanceof Integer) {
        return new StubOrdered(2);
      }
      return null;
    });

    assertThat(comparable.compare("test", 123)).isEqualTo(-1);
    assertThat(comparable.compare(123, "test")).isEqualTo(1);
  }

  @Test
  void getPriorityReturnsNull() {
    assertThat(OrderComparator.INSTANCE.getPriority(new Object())).isNull();
  }

  @Test
  void sortHandlesEmptyList() {
    List<Object> list = new ArrayList<>();
    OrderComparator.sort(list);
    assertThat(list).isEmpty();
  }

  @Test
  void sortHandlesSingleElementList() {
    List<Object> list = new ArrayList<>();
    list.add(new StubOrdered(1));
    OrderComparator.sort(list);
    assertThat(list).hasSize(1);
  }

  @Test
  void sortHandlesEmptyArray() {
    Object[] array = new Object[0];
    OrderComparator.sort(array);
    assertThat(array).isEmpty();
  }

  @Test
  void sortHandlesSingleElementArray() {
    Object[] array = new Object[] { new StubOrdered(1) };
    OrderComparator.sort(array);
    assertThat(array).hasSize(1);
  }

  @Test
  void sortIfNecessaryHandlesNonListOrArray() {
    String value = "test";
    OrderComparator.sortIfNecessary(value);
    assertThat(value).isEqualTo("test");
  }

  @Test
  void orderSourceProviderImplementationHasPrecedenceOverProvider() {
    var source = new OrderSourceImplementation(5);
    var provider = new TestSourceProvider(source, new StubOrdered(10));

    var comparator = new OrderComparator();
    assertThat(comparator.compare(source, new StubOrdered(1))).isEqualTo(1);
  }

  @Test
  void compareWithMultipleSourceProvidersUsesFirst() {
    var obj = new OrderSourceImplementation(5);
    obj = new OrderSourceImplementation(10) {
      @Override
      public Object getOrderSource(Object o) {
        return new StubOrdered(20);
      }
    };

    assertThat(comparator.compare(obj, new StubOrdered(15))).isEqualTo(1);
  }

  @Test
  void sortListPreservesEqualOrderElements() {
    List<Ordered> list = new ArrayList<>();
    var first = new StubOrdered(10);
    var second = new StubOrdered(10);
    list.add(first);
    list.add(second);

    OrderComparator.sort(list);
    assertThat(list).containsExactly(first, second);
  }

  @Test
  void sortArrayPreservesEqualOrderElements() {
    var first = new StubOrdered(10);
    var second = new StubOrdered(10);
    Object[] array = new Object[] { first, second };

    OrderComparator.sort(array);
    assertThat(array).containsExactly(first, second);
  }

  @Test
  void priorityOrderedWithNullComparedToOrdered() {
    assertThat(comparator.compare(null, new StubPriorityOrdered(10))).isEqualTo(1);
    assertThat(comparator.compare(new StubPriorityOrdered(10), null)).isEqualTo(-1);
  }

  @Test
  void sortIfNecessaryHandlesNullValue() {
    OrderComparator.sortIfNecessary(null);
  }

  @Test
  void sortIfNecessaryHandlesListWithOrderedElements() {
    List<Ordered> list = new ArrayList<>();
    list.add(new StubOrdered(3));
    list.add(new StubOrdered(1));
    list.add(new StubOrdered(2));

    OrderComparator.sortIfNecessary(list);
    assertThat(list).extracting(Ordered::getOrder).containsExactly(1, 2, 3);
  }

  @Test
  void sortIfNecessaryHandlesArrayWithOrderedElements() {
    Object[] array = new Object[] { new StubOrdered(3), new StubOrdered(1), new StubOrdered(2) };

    OrderComparator.sortIfNecessary(array);
    assertThat(array).extracting("order").containsExactly(1, 2, 3);
  }

  @Test
  void sortIfNecessaryHandlesPrimitiveArray() {
    int[] array = new int[] { 3, 1, 2 };
    OrderComparator.sortIfNecessary(array);
    assertThat(array).containsExactly(3, 1, 2);
  }

  @Test
  void sortIfNecessaryHandlesCustomCollection() {
    Set<Ordered> set = new HashSet<>();
    set.add(new StubOrdered(1));
    set.add(new StubOrdered(2));
    OrderComparator.sortIfNecessary(set);
    assertThat(set).hasSize(2);
  }

  private static class OrderSourceImplementation implements OrderSourceProvider {
    private final int order;

    OrderSourceImplementation(int order) {
      this.order = order;
    }

    @Override
    public Object getOrderSource(Object obj) {
      return new StubOrdered(order);
    }
  }

  @Test
  void compareHandlesOrderSourceProviderReturningNullSource() {
    var provider = new TestSourceProvider(new Object(), null);
    var comparator = OrderComparator.INSTANCE.withSourceProvider(provider);
    assertThat(comparator.compare(new Object(), new Object())).isEqualTo(0);
  }

  @Test
  void compareWithSourceProviderListReturnsLowestOrderValue() {
    List<Ordered> sources = List.of(new StubOrdered(30), new StubOrdered(10), new StubOrdered(20));
    Comparator<Object> customComparator = comparator.withSourceProvider(
            new TestSourceProvider(5L, sources));
    assertThat(customComparator.compare(5L, new StubOrdered(15))).isEqualTo(1);
  }

  @Test
  void compareNullSourceProviderReturnsFallbackOrder() {
    Comparator<Object> customComparator = comparator.withSourceProvider(null);
    assertThat(customComparator.compare(new StubOrdered(1), new StubOrdered(2))).isEqualTo(-1);
  }

  @Test
  void compareListSourceProviderWithAllNullOrders() {
    List<Object> sources = List.of(new Object(), new Object());
    Comparator<Object> customComparator = comparator.withSourceProvider(
            new TestSourceProvider(5L, sources));
    assertThat(customComparator.compare(5L, new StubOrdered(1))).isEqualTo(1);
  }

  @Test
  void compareArraySourceProviderWithMixedOrderedAndNonOrdered() {
    Object[] sources = new Object[] { new Object(), new StubOrdered(5), new Object() };
    Comparator<Object> customComparator = comparator.withSourceProvider(
            new TestSourceProvider(5L, sources));
    assertThat(customComparator.compare(5L, new StubOrdered(10))).isEqualTo(-1);
  }

  @Test
  void sortEmptyListWithNullComparator() {
    List<Object> list = new ArrayList<>();
    OrderComparator.sort(list);
    assertThat(list).isEmpty();
  }

  @Test
  void sortEmptyArrayWithNullComparator() {
    Object[] array = new Object[0];
    OrderComparator.sort(array);
    assertThat(array).isEmpty();
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
