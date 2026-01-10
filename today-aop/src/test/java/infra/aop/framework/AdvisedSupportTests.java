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

package infra.aop.framework;

import org.junit.jupiter.api.Test;

import infra.aop.Advisor;
import infra.aop.DefaultInterceptorChainFactory;
import infra.aop.DynamicIntroductionAdvice;
import infra.aop.IntroductionAdvisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 23:25
 */
class AdvisedSupportTests {

  @Test
  void construct() {
    AdvisedSupport support = new AdvisedSupport(CharSequence.class);
    assertThat(support.getProxiedInterfaces()).hasSize(1).containsExactly(CharSequence.class);
    assertThat(support.getInterceptorChainFactory()).isSameAs(DefaultInterceptorChainFactory.INSTANCE);

    support.setInterceptorChainFactory(DefaultInterceptorChainFactory.INSTANCE);
    assertThat(support.getInterceptorChainFactory()).isSameAs(DefaultInterceptorChainFactory.INSTANCE);
  }

  @Test
  void addInterface() {
    AdvisedSupport support = new AdvisedSupport();
    support.addInterface(CharSequence.class);

    assertThatThrownBy((() -> support.addInterface(Object.class)))
            .isInstanceOf(IllegalArgumentException.class);
    assertThat(support.getProxiedInterfaces()).hasSize(1).containsExactly(CharSequence.class);
  }

  @Test
  void simple() {
    AdvisedSupport support = new AdvisedSupport();
    assertThat(support.isPreFiltered()).isFalse();
    support.setPreFiltered(true);
    assertThat(support.isPreFiltered()).isTrue();

    support.setFrozen(true);

    assertThatThrownBy((() -> support.removeAdvisor(1)))
            .isInstanceOf(AopConfigException.class)
            .hasMessage("Cannot remove Advisor: Configuration is frozen.");

    assertThat(support.getAdvisorCount()).isZero();

    Advisor advisor = mock(Advisor.class);
    assertThatThrownBy((() -> support.addAdvisors(advisor)))
            .isInstanceOf(AopConfigException.class)
            .hasMessage("Cannot add advisor: Configuration is frozen.");

    support.setFrozen(false);
    support.addAdvisors(advisor);
    assertThat(support.getAdvisorCount()).isEqualTo(1);
    assertThat(support.getAdvisorsInternal()).containsExactly(advisor);

    support.setFrozen(true);
    assertThatThrownBy((() -> support.addAdvisorInternal(1, advisor)))
            .isInstanceOf(AopConfigException.class)
            .hasMessage("Cannot add advisor: Configuration is frozen.");

    support.setFrozen(false);
    assertThatThrownBy((() -> support.addAdvisorInternal(2, advisor)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal position 2 in advisor list with size 1");
    support.addAdvisorInternal(1, advisor);
    assertThat(support.getAdvisorCount()).isEqualTo(2);
    assertThat(support.getAdvisorsInternal()).containsExactly(advisor, advisor);

    // DynamicIntroductionAdvice

    DynamicIntroductionAdvice advice = mock(DynamicIntroductionAdvice.class);
    assertThatThrownBy((() -> support.addAdvice(advice)))
            .isInstanceOf(AopConfigException.class)
            .hasMessage("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");

    assertThat(support.toString()).contains("targetSource")
            .contains("interfaces").contains("advisors")
            .isEqualTo(support.toProxyConfigString());
    // copyConfigurationFrom

    IntroductionAdvisor introductionAdvisor = mock(IntroductionAdvisor.class);
    given(introductionAdvisor.getInterfaces()).willReturn(new Class[0]);
    support.addAdvisor(introductionAdvisor);

    AdvisedSupport support1 = new AdvisedSupport();
    support1.copyConfigurationFrom(support);
    assertThat(support.toString()).isEqualTo(support1.toString());

  }

}