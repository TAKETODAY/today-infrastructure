/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.context.hierarchies.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.annotation.DirtiesContext;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;

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
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
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
