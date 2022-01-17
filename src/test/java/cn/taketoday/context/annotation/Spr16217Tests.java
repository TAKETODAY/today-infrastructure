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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
public class Spr16217Tests {

  @Test
  @Disabled("TODO")
  public void baseConfigurationIsIncludedWhenFirstSuperclassReferenceIsSkippedInRegisterBeanPhase() {
    try (StandardApplicationContext context =
            new StandardApplicationContext(RegisterBeanPhaseImportingConfiguration.class)) {
      context.getBean("someBean");
    }
  }

  @Test
  public void baseConfigurationIsIncludedWhenFirstSuperclassReferenceIsSkippedInParseConfigurationPhase() {
    try (StandardApplicationContext context =
            new StandardApplicationContext(ParseConfigurationPhaseImportingConfiguration.class)) {
      context.getBean("someBean");
    }
  }

  @Test
  public void baseConfigurationIsIncludedOnceWhenBothConfigurationClassesAreActive() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.setAllowBeanDefinitionOverriding(false);
    context.register(UnconditionalImportingConfiguration.class);
    context.refresh();
    try {
      context.getBean("someBean");
    }
    finally {
      context.close();
    }
  }

  public static class RegisterBeanPhaseCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

  public static class ParseConfigurationPhaseCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.PARSE_CONFIGURATION;
    }
  }

  @Import({ RegisterBeanPhaseConditionConfiguration.class, BarConfiguration.class })
  public static class RegisterBeanPhaseImportingConfiguration {
  }

  @Import({ ParseConfigurationPhaseConditionConfiguration.class, BarConfiguration.class })
  public static class ParseConfigurationPhaseImportingConfiguration {
  }

  @Import({ UnconditionalConfiguration.class, BarConfiguration.class })
  public static class UnconditionalImportingConfiguration {
  }

  public static class BaseConfiguration {

    @Bean
    public String someBean() {
      return "foo";
    }
  }

  @Conditional(RegisterBeanPhaseCondition.class)
  public static class RegisterBeanPhaseConditionConfiguration extends BaseConfiguration {
  }

  @Conditional(ParseConfigurationPhaseCondition.class)
  public static class ParseConfigurationPhaseConditionConfiguration extends BaseConfiguration {
  }

  public static class UnconditionalConfiguration extends BaseConfiguration {
  }

  public static class BarConfiguration extends BaseConfiguration {
  }

}
