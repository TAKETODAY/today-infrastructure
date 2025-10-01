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

import org.jspecify.annotations.Nullable;

import infra.annotation.ConditionalOnWebApplication.Type;
import infra.app.ApplicationType;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.FilteringInfraCondition;
import infra.core.Ordered;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.web.server.context.GenericWebServerApplicationContext;
import infra.web.server.reactive.context.ConfigurableReactiveWebEnvironment;
import infra.web.server.reactive.context.ReactiveWebApplicationContext;
import infra.web.server.support.ConfigurableNettyWebEnvironment;

/**
 * {@link Condition} that checks for the presence or absence of
 * web.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 * @since 4.0
 */
class OnWebApplicationCondition extends FilteringInfraCondition implements Ordered {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }

  @SuppressWarnings("NullAway")
  @Override
  protected @Nullable ConditionOutcome[] getOutcomes(String[] configClasses, AutoConfigurationMetadata configMetadata) {
    ConditionOutcome[] outcomes = new ConditionOutcome[configClasses.length];
    for (int i = 0; i < outcomes.length; i++) {
      String autoConfigurationClass = configClasses[i];
      if (autoConfigurationClass != null) {
        outcomes[i] = getOutcome(configMetadata.get(autoConfigurationClass, "ConditionalOnWebApplication"));
      }
    }
    return outcomes;
  }

  @Nullable
  private ConditionOutcome getOutcome(@Nullable String type) {
    if (type == null) {
      return null;
    }
    ClassNameFilter missingClassFilter = ClassNameFilter.MISSING;
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnWebApplication.class);
    if (ConditionalOnWebApplication.Type.NETTY.name().equals(type)) {
      if (missingClassFilter.matches(ApplicationType.NETTY_WEB_INDICATOR_CLASS, getBeanClassLoader())) {
        return ConditionOutcome.noMatch(message.didNotFind("netty web application classes").atAll());
      }
    }
    else if (ConditionalOnWebApplication.Type.REACTIVE.name().equals(type)) {
      if (missingClassFilter.matches(ApplicationType.REACTOR_INDICATOR_CLASS, getBeanClassLoader())) {
        return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
      }
    }
    if (missingClassFilter.matches(ApplicationType.NETTY_WEB_INDICATOR_CLASS, getBeanClassLoader())
            && missingClassFilter.matches(ApplicationType.REACTOR_INDICATOR_CLASS, getBeanClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("reactive, netty web application classes").atAll());
    }
    return null;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    boolean required = metadata.isAnnotated(ConditionalOnWebApplication.class);
    ConditionOutcome outcome = isWebApplication(context, metadata, required);
    if (required && !outcome.isMatch()) {
      return ConditionOutcome.noMatch(outcome.getConditionMessage());
    }
    if (!required && outcome.isMatch()) {
      return ConditionOutcome.noMatch(outcome.getConditionMessage());
    }
    return ConditionOutcome.match(outcome.getConditionMessage());
  }

  private ConditionOutcome isWebApplication(ConditionContext context, AnnotatedTypeMetadata metadata, boolean required) {
    return switch (deduceType(metadata)) {
      case NETTY -> isNettyWebApplication(context);
      case REACTIVE -> isReactiveWebApplication(context);
      default -> isAnyApplication(context, required);
    };
  }

  private ConditionOutcome isAnyApplication(ConditionContext context, boolean required) {
    var message = ConditionMessage.forCondition(ConditionalOnWebApplication.class, required ? "(required)" : "");

    ConditionOutcome nettyOutcome = isNettyWebApplication(context);
    if (nettyOutcome.isMatch() && required) {
      return new ConditionOutcome(nettyOutcome.isMatch(), message.because(nettyOutcome.getMessage()));
    }
    ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
    if (reactiveOutcome.isMatch() && required) {
      return new ConditionOutcome(reactiveOutcome.isMatch(), message.because(reactiveOutcome.getMessage()));
    }
    return new ConditionOutcome(reactiveOutcome.isMatch() || nettyOutcome.isMatch(),
            message.because(nettyOutcome.getMessage())
                    .append("and").append(reactiveOutcome.getMessage()));
  }

  private ConditionOutcome isReactiveWebApplication(ConditionContext context) {
    var message = ConditionMessage.forCondition("");

    ClassNameFilter missingClassFilter = ClassNameFilter.MISSING;
    if (missingClassFilter.matches(ApplicationType.WEB_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("web application classes").atAll());
    }

    if (missingClassFilter.matches(ApplicationType.REACTOR_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
    }

    if (context.getEnvironment() instanceof ConfigurableReactiveWebEnvironment) {
      return ConditionOutcome.match(message.foundExactly("ConfigurableReactiveWebEnvironment"));
    }

    ResourceLoader resourceLoader = context.getResourceLoader();
    if (resourceLoader instanceof ReactiveWebApplicationContext) {
      return ConditionOutcome.match(message.foundExactly("ReactiveWebApplicationContext"));
    }
    return ConditionOutcome.noMatch(message.because("not a reactive web application"));
  }

  /**
   * web netty classes
   */
  private ConditionOutcome isNettyWebApplication(ConditionContext context) {
    var message = ConditionMessage.forCondition("");
    ClassNameFilter missingClassFilter = ClassNameFilter.MISSING;
    if (missingClassFilter.matches(ApplicationType.WEB_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("web application classes").atAll());
    }

    if (missingClassFilter.matches(ApplicationType.NETTY_WEB_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("netty classes").atAll());
    }

    if (context.getEnvironment() instanceof ConfigurableNettyWebEnvironment) {
      return ConditionOutcome.match(message.foundExactly("NettyWebConfigurableEnvironment"));
    }

    ResourceLoader resourceLoader = context.getResourceLoader();
    if (resourceLoader instanceof GenericWebServerApplicationContext) {
      return ConditionOutcome.match(message.foundExactly("GenericWebServerApplicationContext"));
    }
    return ConditionOutcome.noMatch(message.because("not a netty web application"));
  }

  private Type deduceType(AnnotatedTypeMetadata metadata) {
    var annotation = metadata.getAnnotation(ConditionalOnWebApplication.class);
    if (annotation.isPresent()) {
      return annotation.getEnum("type", Type.class);
    }
    return Type.ANY;
  }

}
