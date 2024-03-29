/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.properties.scan.valid.a;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

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
