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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.MethodInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultTestContextTests {

  private final MergedContextConfiguration configuration = mock(MergedContextConfiguration.class);

  private final CacheAwareContextLoaderDelegate delegate = mock(CacheAwareContextLoaderDelegate.class);

  private final DefaultTestContext context = new DefaultTestContext(getClass(), configuration, delegate);

  @Test
  void attributeOperationsAndCopyAreIndependent() {
    assertThat(context.hasAttributes()).isFalse();
    context.setAttributes(null);
    context.setAttributes(Map.of());
    context.setAttributes(Map.of("first", "one"));
    assertThat(context.hasAttribute("first")).isTrue();
    assertThat(context.hasAttribute("missing")).isFalse();
    AtomicInteger computations = new AtomicInteger();
    Object existing = context.computeAttribute("first", name -> computations.incrementAndGet());
    Integer computed = context.computeAttribute("second", name -> computations.incrementAndGet());
    assertThat(existing).isEqualTo("one");
    assertThat(computed).isEqualTo(1);
    assertThat(computations).hasValue(1);
    assertThat(context.getAttributeNames()).containsExactlyInAnyOrder("first", "second");
    DefaultTestContext copy = new DefaultTestContext(context);
    assertThat(copy.getAttributes()).containsAllEntriesOf(context.getAttributes());
    context.setAttribute("first", null);
    assertThat(copy.getAttribute("first")).isEqualTo("one");
    assertThat(context.removeAttribute("second")).isEqualTo(1);
    assertThat(context.removeAttribute("second")).isNull();
    context.copyAttributeFrom(new DefaultTestContext(context));
    context.copyAttributeFrom(copy);
    assertThat(context.getAttributes()).containsExactlyInAnyOrderEntriesOf(copy.getAttributes());
    context.clearAttributes();
    assertThat(context.hasAttributes()).isFalse();
    assertThat(copy.hasAttributes()).isTrue();
  }

  @Test
  void validatesAttributeArgumentsAndComputedValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> context.setAttribute(null, "value"));
    assertThatIllegalArgumentException().isThrownBy(() -> context.getAttribute(null));
    assertThatIllegalArgumentException().isThrownBy(() -> context.removeAttribute(null));
    assertThatIllegalArgumentException().isThrownBy(() -> context.hasAttribute(null));
    assertThatIllegalArgumentException().isThrownBy(() -> context.computeAttribute(null, name -> "value"));
    assertThatIllegalArgumentException().isThrownBy(() -> context.computeAttribute("name", null));
    assertThatIllegalArgumentException().isThrownBy(() -> context.copyAttributeFrom(null));
    assertThatIllegalStateException().isThrownBy(() -> context.computeAttribute("name", name -> null))
            .withMessageContaining("must not return null");
  }

  @Test
  void stateAndMethodInvokerCanBeUpdatedAndReset() throws Exception {
    assertThat(context.getTestClass()).isEqualTo(getClass());
    assertThatIllegalStateException().isThrownBy(context::getTestInstance).withMessage("No test instance");
    assertThatIllegalStateException().isThrownBy(context::getTestMethod).withMessage("No test method");
    Method method = getClass().getDeclaredMethod("stateAndMethodInvokerCanBeUpdatedAndReset");
    Throwable failure = new IllegalStateException("test failure");
    context.updateState(this, method, failure);
    assertThat(context.getTestInstance()).isSameAs(this);
    assertThat(context.getTestMethod()).isEqualTo(method);
    assertThat(context.getTestException()).isSameAs(failure);
    MethodInvoker invoker = mock(MethodInvoker.class);
    context.setMethodInvoker(invoker);
    assertThat(context.getMethodInvoker()).isSameAs(invoker);
    DefaultTestContext copy = new DefaultTestContext(context);
    assertThat(copy.getMethodInvoker()).isSameAs(MethodInvoker.DEFAULT_INVOKER);
    assertThat(copy.getTestException()).isNull();
    assertThatIllegalStateException().isThrownBy(copy::getTestInstance);
    assertThatIllegalArgumentException().isThrownBy(() -> context.setMethodInvoker(null));
    context.updateState(null, null, null);
    assertThat(context.getTestException()).isNull();
    assertThat(context.toString()).contains("testClass", "attributes", "mergedContextConfiguration");
  }

  @Test
  void delegatesContextLifecycleAndChecksActiveState() {
    ApplicationContext plain = mock(ApplicationContext.class);
    given(delegate.loadContext(configuration)).willReturn(plain);
    given(delegate.isContextLoaded(configuration)).willReturn(true);
    assertThat(context.hasApplicationContext()).isTrue();
    assertThat(context.getApplicationContext()).isSameAs(plain);
    verify(delegate).registerContextUsage(configuration, getClass());
    context.markApplicationContextUnused();
    verify(delegate).unregisterContextUsage(configuration, getClass());
    context.markApplicationContextDirty(HierarchyMode.CURRENT_LEVEL);
    verify(delegate).closeContext(configuration, HierarchyMode.CURRENT_LEVEL);
    ConfigurableApplicationContext configurable = mock(ConfigurableApplicationContext.class);
    given(delegate.loadContext(configuration)).willReturn(configurable);
    assertThatIllegalStateException().isThrownBy(context::getApplicationContext).withMessageContaining("not active");
    given(configurable.isActive()).willReturn(true);
    assertThat(context.getApplicationContext()).isSameAs(configurable);
    verify(configurable, never()).close();
  }

  @Test
  void constructorRequiresAllDependencies() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultTestContext(null, configuration, delegate));
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultTestContext(getClass(), null, delegate));
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultTestContext(getClass(), configuration, null));
  }

}
