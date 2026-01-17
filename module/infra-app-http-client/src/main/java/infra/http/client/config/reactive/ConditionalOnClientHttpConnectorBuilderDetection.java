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

package infra.http.client.config.reactive;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.core.type.AnnotatedTypeMetadata;
import infra.http.client.reactive.ClientHttpConnectorBuilder;

/**
 * {@link Condition} that checks that {@link ClientHttpConnectorBuilder} can be detected.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ConditionalOnClientHttpConnectorBuilderDetection extends InfraCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    try {
      ClientHttpConnectorBuilder.detect(context.getClassLoader());
      return ConditionOutcome.match("Detected ClientHttpConnectorBuilder");
    }
    catch (IllegalStateException ex) {
      return ConditionOutcome.noMatch("Unable to detect ClientHttpConnectorBuilder");
    }
  }

}
