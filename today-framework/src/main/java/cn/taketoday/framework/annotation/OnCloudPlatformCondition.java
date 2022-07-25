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

package cn.taketoday.framework.annotation;

import java.util.Map;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.condition.ConditionMessage;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.context.condition.ContextCondition;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.cloud.CloudPlatform;

/**
 * {@link Condition} that checks for a required {@link CloudPlatform}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnCloudPlatform
 * @since 4.0 2022/4/4 12:21
 */
class OnCloudPlatformCondition extends ContextCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnCloudPlatform.class.getName());
    CloudPlatform cloudPlatform = (CloudPlatform) attributes.get("value");
    return getMatchOutcome(context.getEnvironment(), cloudPlatform);
  }

  private ConditionOutcome getMatchOutcome(Environment environment, CloudPlatform cloudPlatform) {
    String name = cloudPlatform.name();
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnCloudPlatform.class);
    if (cloudPlatform.isActive(environment)) {
      return ConditionOutcome.match(message.foundExactly(name));
    }
    return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
  }

}
