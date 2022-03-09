/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation.spr12233;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * Tests cornering the regression reported in SPR-12233.
 *
 * @author Phillip Webb
 */
public class Spr12233Tests {

  @Test
  public void spr12233() throws Exception {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(PropertySourcesPlaceholderConfigurer.class);
    ctx.register(ImportConfiguration.class);
    ctx.refresh();
    ctx.close();
  }

  static class NeverConfigurationCondition implements ConfigurationCondition {
    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
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
