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

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import infra.aop.framework.ProxyFactory;
import infra.aop.TargetSource;
import infra.aop.StandardProxy;
import infra.aop.target.HotSwappableTargetSource;
import infra.aop.target.SingletonTargetSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link InfraMockResolver}.
 *
 * @author Moritz Halbritter
 * @author Sam Brannen
 * @see InfraMockResolverIntegrationTests
 * @since 5.0
 */
class InfraMockResolverTests {

  @Test
  void staticTarget() {
    MyServiceImpl myService = new MyServiceImpl();
    MyService proxy = ProxyFactory.getProxy(MyService.class, new SingletonTargetSource(myService));
    Object target = new InfraMockResolver().resolve(proxy);
    assertThat(target).isInstanceOf(MyServiceImpl.class);
  }

  @Test
  void nonStaticTarget() {
    MyServiceImpl myService = new MyServiceImpl();
    MyService proxy = ProxyFactory.getProxy(MyService.class, new HotSwappableTargetSource(myService));
    Object target = new InfraMockResolver().resolve(proxy);
    assertThat(target).isInstanceOf(StandardProxy.class);
  }

  @Test
  void emptyStaticTargetAndOpaqueProxyRemainUnwrapped() throws Exception {
    TargetSource source = mock(TargetSource.class);
    given(source.isStatic()).willReturn(true);
    org.mockito.BDDMockito.<Class<?>>given(source.getTargetClass()).willReturn(MyService.class);
    MyService proxy = ProxyFactory.getProxy(MyService.class, source);
    assertThat(InfraMockResolver.getUltimateTargetObject(proxy)).isSameAs(proxy);
    ProxyFactory factory = new ProxyFactory(new MyServiceImpl());
    factory.setOpaque(true);
    Object opaque = factory.getProxy();
    assertThat(InfraMockResolver.getUltimateTargetObject(opaque)).isSameAs(opaque);
    InfraMockResolver.rejectUnsupportedSpyTarget("opaque", opaque);
  }

  @Test
  void failingStaticTargetPreservesCause() throws Exception {
    TargetSource source = mock(TargetSource.class);
    given(source.isStatic()).willReturn(true);
    org.mockito.BDDMockito.<Class<?>>given(source.getTargetClass()).willReturn(MyService.class);
    Exception failure = new Exception("target failed");
    given(source.getTarget()).willThrow(failure);
    MyService proxy = ProxyFactory.getProxy(MyService.class, source);
    assertThatIllegalStateException().isThrownBy(() -> InfraMockResolver.getUltimateTargetObject(proxy))
            .withMessage("Failed to unwrap proxied object").withCause(failure);
  }

  private interface MyService {
  }

  private static final class MyServiceImpl implements MyService {
  }

}
