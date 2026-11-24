/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.util;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApplicationContextTestUtilsTests {

  @Test
  void closesChildBeforeParentAndAcceptsNull() {
    ConfigurableApplicationContext parent = mock(ConfigurableApplicationContext.class);
    ConfigurableApplicationContext child = mock(ConfigurableApplicationContext.class);
    given(child.getParent()).willReturn(parent);
    ApplicationContextTestUtils.closeAll(child);
    ApplicationContextTestUtils.closeAll(null);
    InOrder order = inOrder(child, parent);
    order.verify(child).close();
    order.verify(parent).close();
  }

  @Test
  void traversesNonconfigurableContextsToCloseTheirParents() {
    ApplicationContext child = mock(ApplicationContext.class);
    ConfigurableApplicationContext parent = mock(ConfigurableApplicationContext.class);
    given(child.getParent()).willReturn(parent);
    ApplicationContextTestUtils.closeAll(child);
    verify(parent).close();
  }

}
