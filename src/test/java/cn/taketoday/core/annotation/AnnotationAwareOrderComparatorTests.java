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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
