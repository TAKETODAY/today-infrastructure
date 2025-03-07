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

package infra.annotation.config.aop;

import org.junit.jupiter.api.Test;

import infra.aop.config.AopConfigUtils;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AopAutoConfiguration} without AspectJ.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("aspectjweaver*.jar")
class NonAspectJAopAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class));

  @Test
  void whenAspectJIsAbsentAndProxyTargetClassIsEnabledProxyCreatorBeanIsDefined() {
    this.contextRunner.run((context) -> {
      BeanDefinition autoProxyCreator = context.getBeanFactory()
              .getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
      assertThat(autoProxyCreator.getPropertyValues().get("proxyTargetClass").getValue()).isEqualTo(Boolean.TRUE);
    });
  }

  @Test
  void whenAspectJIsAbsentAndProxyTargetClassIsDisabledNoProxyCreatorBeanIsDefined() {
    contextRunner.withPropertyValues("infra.aop.proxy-target-class:false")
            .run(context -> assertThat(context).doesNotHaveBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
  }

}
