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

package infra.annotation;

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
    CloudPlatform cloudPlatform = metadata.getAnnotation(ConditionalOnCloudPlatform.class)
            .getEnum("value", CloudPlatform.class);
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
