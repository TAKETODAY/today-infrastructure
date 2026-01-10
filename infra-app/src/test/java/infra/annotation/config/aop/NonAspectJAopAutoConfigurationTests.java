/*
 * Copyright 2012-present the original author or authors.
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

package infra.annotation.config.aop;

import org.junit.jupiter.api.Test;

import infra.aop.config.AopConfigUtils;
import infra.aop.framework.autoproxy.AutoProxyUtils;
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
      BeanDefinition defaultProxyConfig = context.getBeanFactory()
              .getBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME);
      assertThat(defaultProxyConfig.getPropertyValues().getPropertyValue("proxyTargetClass")).isEqualTo(Boolean.TRUE);
    });
  }

  @Test
  void whenAspectJIsAbsentAndProxyTargetClassIsDisabledNoProxyCreatorBeanIsDefined() {
    this.contextRunner.withPropertyValues("infra.aop.proxy-target-class:false")
            .run((context) -> assertThat(context).doesNotHaveBean(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME)
                    .doesNotHaveBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
  }

}
