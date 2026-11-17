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

package infra.web.config;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a user-defined {@link ForwardedHeaderStrategy}.
 *
 * @author Phillip Webb
 */
class OnForwardedHeaderStrategyCondition extends InfraCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ForwardedHeaderStrategy required = metadata.getAnnotation(ConditionalOnForwardedHeaderStrategy.class)
            .getEnum("value", ForwardedHeaderStrategy.class);
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnForwardedHeaderStrategy.class);
    ForwardedHeaderStrategy strategy = Binder.get((ConfigurableEnvironment) context.getEnvironment())
            .bind("web.forwarded-headers.strategy", ForwardedHeaderStrategy.class).orElse(null);
    if (required == strategy) {
      return ConditionOutcome.match(message.found("forwarded header strategy").items(required));
    }
    return ConditionOutcome.noMatch(message.didNotFind("forwarded header strategy").items(required));
  }

}
