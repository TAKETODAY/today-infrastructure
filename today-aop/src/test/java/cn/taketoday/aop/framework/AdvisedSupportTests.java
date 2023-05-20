/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.framework;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.DefaultInterceptorChainFactory;
import cn.taketoday.aop.DynamicIntroductionAdvice;
import cn.taketoday.aop.IntroductionAdvisor;

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