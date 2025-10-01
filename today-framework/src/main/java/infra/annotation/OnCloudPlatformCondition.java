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

package infra.annotation;

import java.util.Map;

import infra.app.cloud.CloudPlatform;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.core.env.Environment;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required {@link CloudPlatform}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnCloudPlatform
 * @since 4.0 2022/4/4 12:21
 */
@SuppressWarnings("NullAway")
class OnCloudPlatformCondition extends InfraCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnCloudPlatform.class);
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
