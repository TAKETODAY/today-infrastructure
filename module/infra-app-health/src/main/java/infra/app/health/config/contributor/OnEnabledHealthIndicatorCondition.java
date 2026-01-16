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

package infra.app.health.config.contributor;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.core.annotation.MergedAnnotation;
import infra.core.env.Environment;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a health indicator is enabled.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class OnEnabledHealthIndicatorCondition extends InfraCondition {

  private static final String DEFAULTS_PROPERTY_NAME = "app.health.defaults.enabled";

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    MergedAnnotation<?> annotation = metadata.getAnnotations().get(ConditionalOnEnabledHealthIndicator.class);
    String name = annotation.getString(MergedAnnotation.VALUE);
    Environment environment = context.getEnvironment();
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnEnabledHealthIndicator.class);
    String propertyName = "app.health.%s.enabled".formatted(name);
    if (environment.containsProperty(propertyName)) {
      boolean match = environment.getFlag(propertyName, true);
      return new ConditionOutcome(match, message.because(propertyName + " is " + match));
    }
    boolean match = context.getEnvironment().getFlag(DEFAULTS_PROPERTY_NAME, true);
    return new ConditionOutcome(match, message.because(DEFAULTS_PROPERTY_NAME + " is considered " + match));
  }

}
