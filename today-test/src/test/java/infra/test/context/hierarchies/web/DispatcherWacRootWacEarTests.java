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

package infra.test.context.hierarchies.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.mock.api.MockContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
public class DispatcherWacRootWacEarTests extends RootWacEarTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private String ear;

  @Autowired
  private String root;

  @Autowired
  private String dispatcher;

  @Disabled("Superseded by verifyDispatcherWacConfig()")
  @Test
  @Override
  void verifyEarConfig() {
    /* no-op */
  }

  @Disabled("Superseded by verifyDispatcherWacConfig()")
  @Test
  @Override
  void verifyRootWacConfig() {
    /* no-op */
  }

  @Test
  void verifyDispatcherWacConfig() {
    ApplicationContext parent = wac.getParent();
    assertThat(parent).isNotNull();
    boolean condition = parent instanceof WebApplicationContext;
    assertThat(condition).isTrue();

    ApplicationContext grandParent = parent.getParent();
    assertThat(grandParent).isNotNull();
    boolean condition1 = grandParent instanceof WebApplicationContext;
    assertThat(condition1).isFalse();

    MockContext dispatcherMockContext = wac.getMockContext();
    assertThat(dispatcherMockContext).isNotNull();
    MockContext rootMockContext = ((WebApplicationContext) parent).getMockContext();
    assertThat(rootMockContext).isNotNull();
    assertThat(rootMockContext).isSameAs(dispatcherMockContext);

    assertThat(rootMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);
    assertThat(dispatcherMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);

    assertThat(ear).isEqualTo("ear");
    assertThat(root).isEqualTo("root");
    assertThat(dispatcher).isEqualTo("dispatcher");
  }

}
