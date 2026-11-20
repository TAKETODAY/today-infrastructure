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

package infra.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.aop.Advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DefaultAdvisorAdapterRegistryTests {

  @Test
  void explicitUnknownAdviceDiagnosticRetainsItsMessage() {
    assertThat(new UnknownAdviceTypeException("No adapter for test advice")).hasMessage("No adapter for test advice");
  }

  @Test
  void registersCustomAdviceWithTheConfiguredRegistry() {
    DefaultAdvisorAdapterRegistry registry = new DefaultAdvisorAdapterRegistry();
    AdvisorAdapterRegistrationManager manager = new AdvisorAdapterRegistrationManager();
    manager.setAdvisorAdapterRegistry(registry);
    Advice advice = new Advice() { };
    AdvisorAdapter adapter = mock();
    MethodInterceptor interceptor = invocation -> invocation.proceed();
    given(adapter.supportsAdvice(advice)).willReturn(true);

    assertThatExceptionOfType(UnknownAdviceTypeException.class)
            .isThrownBy(() -> registry.wrap(advice))
            .withMessageContaining("neither a supported sub-interface");

    assertThat(manager.postProcessAfterInitialization(adapter, "adapter")).isSameAs(adapter);
    Advisor advisor = registry.wrap(advice);
    given(adapter.getInterceptor(advisor)).willReturn(interceptor);
    assertThat(advisor.getAdvice()).isSameAs(advice);
    assertThat(registry.getInterceptors(advisor)).containsExactly(interceptor);
  }
}
