/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.core.Ordered;
import jakarta.annotation.Priority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/12 21:55
 */
class AnnotationAwareOrderComparatorTests {

  @Test
  void instanceVariableIsAnAnnotationAwareOrderComparator() {
    assertThat(AnnotationAwareOrderComparator.INSTANCE).isInstanceOf(AnnotationAwareOrderComparator.class);
  }

  @Test
  void sortInstances() {
    List<Object> list = new ArrayList<>();
    list.add(new B());
    list.add(new A());
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0) instanceof A).isTrue();
    assertThat(list.get(1) instanceof B).isTrue();
  }

  @Test
  void sortInstancesWithPriority() {
    List<Object> list = new ArrayList<>();
    list.add(new B2());
    list.add(new A2());
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0) instanceof A2).isTrue();
    assertThat(list.get(1) instanceof B2).isTrue();
  }

  @Test
  void sortInstancesWithOrderAndPriority() {
    List<Object> list = new ArrayList<>();
    list.add(new B());
    list.add(new A2());
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0) instanceof A2).isTrue();
    assertThat(list.get(1) instanceof B).isTrue();
  }

  @Test
  void sortInstancesWithSubclass() {
    List<Object> list = new ArrayList<>();
    list.add(new B());
    list.add(new C());
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0) instanceof C).isTrue();
    assertThat(list.get(1) instanceof B).isTrue();
  }

  @Test
  void sortClasses() {
    List<Object> list = new ArrayList<>();
    list.add(B.class);
    list.add(A.class);
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0)).isEqualTo(A.class);
    assertThat(list.get(1)).isEqualTo(B.class);
  }

  @Test
  void sortClassesWithSubclass() {
    List<Object> list = new ArrayList<>();
    list.add(B.class);
    list.add(C.class);
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0)).isEqualTo(C.class);
    assertThat(list.get(1)).isEqualTo(B.class);
  }

  @Test
  void sortWithNulls() {
    List<Object> list = new ArrayList<>();
    list.add(null);
    list.add(B.class);
    list.add(null);
    list.add(A.class);
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0)).isEqualTo(A.class);
    assertThat(list.get(1)).isEqualTo(B.class);
    assertThat(list.get(2)).isNull();
    assertThat(list.get(3)).isNull();
  }

  @Test
  void sortArray() {
    Object[] array = new Object[] { new B(), new A() };
    AnnotationAwareOrderComparator.sort(array);
    assertThat(array[0] instanceof A).isTrue();
    assertThat(array[1] instanceof B).isTrue();
  }

  @Test
  void sortArrayWithPriority() {
    Object[] array = new Object[] { new B2(), new A2() };
    AnnotationAwareOrderComparator.sort(array);
    assertThat(array[0] instanceof A2).isTrue();
    assertThat(array[1] instanceof B2).isTrue();
  }

  @Test
  void sortArrayWithOrderAndPriority() {
    Object[] array = new Object[] { new B(), new A2() };
    AnnotationAwareOrderComparator.sort(array);
    assertThat(array[0] instanceof A2).isTrue();
    assertThat(array[1] instanceof B).isTrue();
  }

  @Test
  void sortArrayWithSubclass() {
    Object[] array = new Object[] { new B(), new C() };
    AnnotationAwareOrderComparator.sort(array);
    assertThat(array[0] instanceof C).isTrue();
    assertThat(array[1] instanceof B).isTrue();
  }

  @Test
  void sortArrayWithNulls() {
    Object[] array = new Object[] { null, B.class, null, A.class };
    AnnotationAwareOrderComparator.sort(array);
    assertThat(array[0]).isEqualTo(A.class);
    assertThat(array[1]).isEqualTo(B.class);
    assertThat(array[2]).isNull();
    assertThat(array[3]).isNull();
  }

  @Test
  void sortIfNecessaryWithList() {
    List<Object> list = new ArrayList<>();
    list.add(new B());
    list.add(new A());
    AnnotationAwareOrderComparator.sortIfNecessary(list);
    assertThat(list.get(0) instanceof A).isTrue();
    assertThat(list.get(1) instanceof B).isTrue();
  }

  @Test
  void sortIfNecessaryWithArray() {
    Object[] array = new Object[] { new B(), new A() };
    AnnotationAwareOrderComparator.sortIfNecessary(array);
    assertThat(array[0] instanceof A).isTrue();
    assertThat(array[1] instanceof B).isTrue();
  }

  @Test
  void sortIfNecessaryWithOtherValue() {
    String value = "not sortable";
    // Should not throw exception
    AnnotationAwareOrderComparator.sortIfNecessary(value);
    assertThat(value).isEqualTo("not sortable");
  }

  @Test
  void findOrderFromOrderedInstance() {
    Ordered ordered = () -> 10;
    Integer order = AnnotationAwareOrderComparator.INSTANCE.findOrder(ordered);
    assertThat(order).isEqualTo(10);
  }

  @Test
  void findOrderFromOrderAnnotation() {
    Integer order = AnnotationAwareOrderComparator.INSTANCE.findOrder(new A());
    assertThat(order).isEqualTo(1);
  }

  @Test
  void findOrderFromPriorityAnnotation() {
    Integer order = AnnotationAwareOrderComparator.INSTANCE.findOrder(new A2());
    assertThat(order).isEqualTo(1);
  }

  @Test
  void findOrderFromClassWithOrderAnnotation() {
    Integer order = AnnotationAwareOrderComparator.INSTANCE.findOrder(A.class);
    assertThat(order).isEqualTo(1);
  }

  @Test
  void findOrderFromNull() {
    Integer order = AnnotationAwareOrderComparator.INSTANCE.findOrder(null);
    assertThat(order).isNull();
  }

  @Test
  void getPriorityFromClass() {
    Integer priority = AnnotationAwareOrderComparator.INSTANCE.getPriority(A2.class);
    assertThat(priority).isEqualTo(1);
  }

  @Test
  void getPriorityFromObject() {
    Integer priority = AnnotationAwareOrderComparator.INSTANCE.getPriority(new A2());
    assertThat(priority).isEqualTo(1);
  }

  @Test
  void getPriorityFromObjectWithoutPriority() {
    Integer priority = AnnotationAwareOrderComparator.INSTANCE.getPriority(new A());
    assertThat(priority).isNull();
  }

  @Order(1)
  private static class A {
  }

  @Order(2)
  private static class B {
  }

  private static class C extends A {
  }

  @Priority(1)
  private static class A2 {
  }

  @Priority(2)
  private static class B2 {
  }

}
