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

package infra.context.annotation.spr12233;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Value;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationCondition;
import infra.context.annotation.Import;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.context.annotation.AnnotationConfigApplicationContext;
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
