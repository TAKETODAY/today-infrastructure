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

package infra.context.properties.scan.valid.a;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Profile;
import infra.context.properties.ConfigurationProperties;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * @author Madhura Bhave
 */
public class AScanConfiguration {

  @ConfigurationProperties(prefix = "a")
  static class AProperties {

  }

  @Profile("test")
  @ConfigurationProperties(prefix = "profile")
  static class MyProfileProperties {

  }

  @Conditional(TestResourceCondition.class)
  @ConfigurationProperties(prefix = "resource")
  static class MyResourceProperties {

  }

  static class TestResourceCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return context.getResourceLoader().getResource("test").exists();
    }
  }

}
