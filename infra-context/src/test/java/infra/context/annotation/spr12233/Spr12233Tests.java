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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation.spr12233;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Value;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationCondition;
import infra.context.annotation.Import;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * @author Phillip Webb
 */
public class Spr12233Tests {

  @Test
  public void spr12233() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(PropertySourcesPlaceholderConfigurer.class);
    ctx.register(ImportConfiguration.class);
    ctx.refresh();
    ctx.close();
  }

  static class NeverConfigurationCondition implements ConfigurationCondition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

  @Import(ComponentScanningConfiguration.class)
  static class ImportConfiguration {

  }

  @Configuration
  @ComponentScan
  static class ComponentScanningConfiguration {

  }

  @Configuration
  @Conditional(NeverConfigurationCondition.class)
  static class ConditionWithPropertyValueInjection {

    @Value("${idontexist}")
    private String property;
  }
}
