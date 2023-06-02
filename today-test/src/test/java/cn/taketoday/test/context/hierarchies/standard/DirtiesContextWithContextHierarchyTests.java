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

package cn.taketoday.test.context.hierarchies.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link DirtiesContext.HierarchyMode}
 * in conjunction with context hierarchies configured via {@link ContextHierarchy}.
 *
 * <p>Note that correct method execution order is essential, thus the use of
 * {@link TestMethodOrder @TestMethodOrder}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ParentConfig.class),
        @ContextConfiguration(classes = DirtiesContextWithContextHierarchyTests.ChildConfig.class)
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DirtiesContextWithContextHierarchyTests {

  @Autowired
  private StringBuilder foo;

  @Autowired
  private StringBuilder baz;

  @Autowired
  private ApplicationContext context;

  @BeforeEach
  void verifyContextHierarchy() {
    assertThat(context).as("child ApplicationContext").isNotNull();
    assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
    assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
  }

  @Test
  @Order(1)
  void verifyOriginalStateAndDirtyContexts() {
    assertOriginalState();
    reverseStringBuilders();
  }

  @Test
  @Order(2)
  @DirtiesContext
  void verifyContextsWereDirtiedAndTriggerExhaustiveCacheClearing() {
    assertDirtyParentContext();
    assertDirtyChildContext();
  }

  @Test
  @Order(3)
  @DirtiesContext(hierarchyMode = HierarchyMode.CURRENT_LEVEL)
  void verifyOriginalStateWasReinstatedAndDirtyContextsAndTriggerCurrentLevelCacheClearing() {
    assertOriginalState();
    reverseStringBuilders();
  }

  @Test
  @Order(4)
  void verifyParentContextIsStillDirtyButChildContextHasBeenReinstated() {
    assertDirtyParentContext();
    assertCleanChildContext();
  }

  private void reverseStringBuilders() {
    foo.reverse();
    baz.reverse();
  }

  private void assertOriginalState() {
    assertCleanParentContext();
    assertCleanChildContext();
  }

  private void assertCleanParentContext() {
    assertThat(foo.toString()).isEqualTo("foo");
  }

  private void assertCleanChildContext() {
    assertThat(baz.toString()).isEqualTo("baz-child");
  }

  private void assertDirtyParentContext() {
    assertThat(foo.toString()).isEqualTo("oof");
  }

  private void assertDirtyChildContext() {
    assertThat(baz.toString()).isEqualTo("dlihc-zab");
  }

  @Configuration
  static class ParentConfig {

    @Bean
    StringBuilder foo() {
      return new StringBuilder("foo");
    }

    @Bean
    StringBuilder baz() {
      return new StringBuilder("baz-parent");
    }
  }

  @Configuration
  static class ChildConfig {

    @Bean
    StringBuilder baz() {
      return new StringBuilder("baz-child");
    }
  }

}
