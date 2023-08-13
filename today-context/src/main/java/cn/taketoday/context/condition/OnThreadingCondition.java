/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.condition;

import java.util.Map;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required {@link Threading}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnThreading
 * @since 4.0
 */
class OnThreadingCondition extends InfraCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnThreading.class.getName());
    Threading threading = (Threading) attributes.get("value");
    return getMatchOutcome(context.getEnvironment(), threading);
  }

  private ConditionOutcome getMatchOutcome(Environment environment, Threading threading) {
    String name = threading.name();
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnThreading.class);
    if (threading.isActive(environment)) {
      return ConditionOutcome.match(message.foundExactly(name));
    }
    return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
  }

}
