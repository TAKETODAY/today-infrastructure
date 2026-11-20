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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import infra.aop.ProxyMethodInvocation;
import infra.aop.framework.ProxyFactory;
import infra.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class InterceptorConfigurationTests {

  @Test
  void dynamicTraceLoggerCanHideProxyClassNames() {
    var interceptor = new DebugInterceptor(true);
    MethodInvocation invocation = mock();
    var proxy = new ProxyFactory(new Task()).getProxy();
    given(invocation.getThis()).willReturn(proxy);

    assertThat(interceptor.getLoggerForInvocation(invocation).getName()).isEqualTo(proxy.getClass().getName());
    interceptor.setHideProxyClassNames(true);
    assertThat(interceptor.getLoggerForInvocation(invocation).getName()).isEqualTo(Task.class.getName());

    given(invocation.getThis()).willReturn(null);
    assertThatIllegalStateException().isThrownBy(() -> interceptor.getLoggerForInvocation(invocation))
            .withMessageContaining("Target is required");
    interceptor.setLoggerName("aop.trace.test");
    assertThat(interceptor.getLoggerForInvocation(invocation).getName()).isEqualTo("aop.trace.test");
  }

  @Test
  void disablingStackTracesPreservesTheMessage() {
    var interceptor = new SimpleTraceInterceptor();
    Logger logger = mock();
    interceptor.setLogExceptionStackTrace(false);

    interceptor.writeToLog(logger, "invocation failed", new IllegalStateException("failure"));

    verify(logger).trace("invocation failed");
    verifyNoMoreInteractions(logger);
  }

  @Test
  void debugCountCanBeReset() throws Throwable {
    var interceptor = new DebugInterceptor();
    MethodInvocation invocation = mock();
    interceptor.invoke(invocation);
    interceptor.invoke(invocation);
    assertThat(interceptor.getCount()).isEqualTo(2);

    interceptor.resetCount();
    interceptor.invoke(invocation);
    assertThat(interceptor.getCount()).isEqualTo(1);
  }

  @Test
  void monitorCanUseTheConcreteTargetClassInItsTraceName() throws Exception {
    var interceptor = new PerformanceMonitorInterceptor();
    interceptor.setLogTargetClassInvocation(true);
    MethodInvocation invocation = mock();
    given(invocation.getMethod()).willReturn(Runnable.class.getMethod("run"));
    given(invocation.getThis()).willReturn(new Task());

    assertThat(interceptor.createInvocationTraceName(invocation)).isEqualTo(Task.class.getName() + ".run");
  }

  @Test
  void beanNameAdvisorsRequireAProxyInvocationAndAnExposedName() {
    MethodInvocation plainInvocation = mock();
    ProxyMethodInvocation proxyInvocation = mock();
    assertThatIllegalArgumentException().isThrownBy(() -> ExposeBeanNameAdvisors.getBeanName(plainInvocation))
            .withMessageContaining("not a ProxyMethodInvocation");
    assertThatIllegalStateException().isThrownBy(() -> ExposeBeanNameAdvisors.getBeanName(proxyInvocation))
            .withMessageContaining("not set on MethodInvocation");

    for (var advisor : new infra.aop.Advisor[] { ExposeBeanNameAdvisors.createAdvisorWithoutIntroduction("task"),
            ExposeBeanNameAdvisors.createAdvisorIntroducingNamedBean("task") }) {
      assertThatIllegalStateException().isThrownBy(() -> ((MethodInterceptor) advisor.getAdvice()).invoke(plainInvocation))
              .withMessageContaining("not a ProxyMethodInvocation");
    }
  }

  static class Task implements Runnable {

    @Override
    public void run() {
    }
  }
}
