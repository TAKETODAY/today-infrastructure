/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.http.client.reactive;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.core.type.AnnotatedTypeMetadata;
import infra.http.client.config.reactive.ClientHttpConnectorBuilder;

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
