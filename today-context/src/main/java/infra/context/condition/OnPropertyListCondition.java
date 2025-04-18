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

package infra.context.condition;

import java.util.List;
import java.util.function.Supplier;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.properties.bind.BindResult;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a property whose value is a list is defined in the
 * environment.
 *
 * @author Eneias Silva
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 13:32
 */
public class OnPropertyListCondition extends InfraCondition {

  private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

  private final String propertyName;

  private final Supplier<ConditionMessage.Builder> messageBuilder;

  /**
   * Create a new instance with the property to check and the message builder to use.
   *
   * @param propertyName the name of the property
   * @param messageBuilder a message builder supplier that should provide a fresh
   * instance on each call
   */
  protected OnPropertyListCondition(String propertyName, Supplier<ConditionMessage.Builder> messageBuilder) {
    this.propertyName = propertyName;
    this.messageBuilder = messageBuilder;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    BindResult<?> property = Binder.get((ConfigurableEnvironment) context.getEnvironment()).bind(this.propertyName, STRING_LIST);
    ConditionMessage.Builder messageBuilder = this.messageBuilder.get();
    if (property.isBound()) {
      return ConditionOutcome.match(messageBuilder.found("property").items(this.propertyName));
    }
    return ConditionOutcome.noMatch(messageBuilder.didNotFind("property").items(this.propertyName));
  }

}
