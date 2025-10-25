/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.scope;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.SimpleBeanDefinitionRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ScopedProxyUtils}.
 *
 * @author Sam Brannen
 */
class ScopedProxyUtilsTests {

  @Test
  void getTargetBeanNameAndIsScopedTarget() {
    String originalBeanName = "myBean";
    String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);

    assertThat(targetBeanName).isNotEqualTo(originalBeanName).endsWith(originalBeanName);
    assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
    assertThat(ScopedProxyUtils.isScopedTarget(originalBeanName)).isFalse();
  }

  @Test
  void getOriginalBeanNameAndIsScopedTarget() {
    String originalBeanName = "myBean";
    String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);
    String parsedOriginalBeanName = ScopedProxyUtils.getOriginalBeanName(targetBeanName);

    assertThat(parsedOriginalBeanName).isNotEqualTo(targetBeanName).isEqualTo(originalBeanName);
    assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
    assertThat(ScopedProxyUtils.isScopedTarget(parsedOriginalBeanName)).isFalse();
  }

  @Test
  void getOriginalBeanNameForNullTargetBean() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName(null))
            .withMessage("bean name 'null' does not refer to the target of a scoped proxy");
  }

  @Test
  void getOriginalBeanNameForNonScopedTarget() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName("myBean"))
            .withMessage("bean name 'myBean' does not refer to the target of a scoped proxy");
  }

  @Test
  void createScopedProxyTargetAppliesAutowireSettingsToProxyBeanDefinition() {
    AbstractBeanDefinition targetDefinition = new GenericBeanDefinition();
    // Opposite of defaults
    targetDefinition.setAutowireCandidate(false);
    targetDefinition.setDefaultCandidate(false);
    targetDefinition.setPrimary(true);
    targetDefinition.setFallback(true);

    BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
    BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(
            new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);
    AbstractBeanDefinition proxyBeanDefinition = (AbstractBeanDefinition) proxyHolder.getBeanDefinition();

    assertThat(proxyBeanDefinition.isAutowireCandidate()).isFalse();
    assertThat(proxyBeanDefinition.isDefaultCandidate()).isFalse();
    assertThat(proxyBeanDefinition.isPrimary()).isTrue();
    assertThat(proxyBeanDefinition.isFallback()).isTrue();
  }

  @Test
  void createScopedProxyTargetAppliesBeanAttributesToProxyBeanDefinition() {
    GenericBeanDefinition targetDefinition = new GenericBeanDefinition();
    // Opposite of defaults
    targetDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    targetDefinition.setSource("theSource");
    targetDefinition.addQualifier(new AutowireCandidateQualifier("myQualifier"));

    BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
    BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(
            new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);
    BeanDefinition proxyBeanDefinition = proxyHolder.getBeanDefinition();

    assertThat(proxyBeanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
    assertThat(proxyBeanDefinition).isInstanceOf(RootBeanDefinition.class);
    assertThat(proxyBeanDefinition.getPropertyValues()).hasSize(2);
    assertThat(proxyBeanDefinition.getPropertyValues().getPropertyValue("proxyTargetClass")).isEqualTo(false);
    assertThat(proxyBeanDefinition.getPropertyValues().getPropertyValue("targetBeanName")).isEqualTo(
            ScopedProxyUtils.getTargetBeanName("myBean"));

    RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) proxyBeanDefinition;
    assertThat(rootBeanDefinition.getQualifiers()).hasSize(1);
    assertThat(rootBeanDefinition.hasQualifier("myQualifier")).isTrue();
    assertThat(rootBeanDefinition.getSource()).isEqualTo("theSource");
  }

  @Test
  void createScopedProxyTargetCleansAutowireSettingsInTargetDefinition() {
    AbstractBeanDefinition targetDefinition = new GenericBeanDefinition();
    targetDefinition.setAutowireCandidate(true);
    targetDefinition.setDefaultCandidate(true);
    targetDefinition.setPrimary(true);
    targetDefinition.setFallback(true);

    BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
    ScopedProxyUtils.createScopedProxy(
            new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);

    assertThat(targetDefinition.isAutowireCandidate()).isFalse();
    assertThat(targetDefinition.isDefaultCandidate()).isFalse();
    assertThat(targetDefinition.isPrimary()).isFalse();
    assertThat(targetDefinition.isFallback()).isFalse();
  }

}
