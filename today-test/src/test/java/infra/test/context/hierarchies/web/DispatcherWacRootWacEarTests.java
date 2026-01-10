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
