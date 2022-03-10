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

import jakarta.annotation.Priority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class OrderUtilsTests {

  @Test
  void getSimpleOrder() {
    assertThat(OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
    assertThat(OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
  }

  @Test
  void getPriorityOrder() {
    assertThat(OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
    assertThat(OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
  }

  @Test
  void getOrderWithBoth() {
    assertThat(OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
    assertThat(OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
  }

  @Test
  void getDefaultOrder() {
    assertThat(OrderUtils.getOrder(NoOrder.class, 33)).isEqualTo(33);
    assertThat(OrderUtils.getOrder(NoOrder.class, 33)).isEqualTo(33);
  }

  @Test
  void getPriorityValueNoAnnotation() {
    assertThat(OrderUtils.getPriority(SimpleOrder.class)).isNull();
    assertThat(OrderUtils.getPriority(SimpleOrder.class)).isNull();
  }

  @Test
  void getPriorityValue() {
    assertThat(OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
    assertThat(OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
  }

  @Order(50)
  private static class SimpleOrder { }

  @Priority(55)
  private static class SimplePriority { }

  @Order(50)
  @Priority(55)
  private static class OrderAndPriority { }

  private static class NoOrder { }

}
