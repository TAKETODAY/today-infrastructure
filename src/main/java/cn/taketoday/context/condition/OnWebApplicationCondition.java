/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.condition;

import java.util.Map;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.servlet.WebServletApplicationContext;

/**
 * {@link Condition} that checks for the presence or absence of
 * {@link WebApplicationContext}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see ConditionalOnApplication
 * @see ConditionalOnNotWebApplication
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnWebApplicationCondition extends FilteringContextCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    boolean required = metadata.isAnnotated(ConditionalOnApplication.class.getName());
    ConditionOutcome outcome = isWebApplication(context, metadata, required);
    if (required && !outcome.isMatch()) {
      return ConditionOutcome.noMatch(outcome.getConditionMessage());
    }
    if (!required && outcome.isMatch()) {
      return ConditionOutcome.noMatch(outcome.getConditionMessage());
    }
    return ConditionOutcome.match(outcome.getConditionMessage());
  }

  private ConditionOutcome isWebApplication(
          ConditionEvaluationContext context, AnnotatedTypeMetadata metadata, boolean required) {
    return switch (deduceType(metadata)) {
      case SERVLET_WEB -> isServletWebApplication(context);
      case REACTIVE_WEB -> isReactiveWebApplication(context);
      default -> isAnyApplication(context, required);
    };
  }

  private ConditionOutcome isAnyApplication(ConditionEvaluationContext context, boolean required) {
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnApplication.class,
            required ? "(required)" : "");
    ConditionOutcome servletOutcome = isServletWebApplication(context);
    if (servletOutcome.isMatch() && required) {
      return new ConditionOutcome(servletOutcome.isMatch(), message.because(servletOutcome.getMessage()));
    }
    ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
    if (reactiveOutcome.isMatch() && required) {
      return new ConditionOutcome(reactiveOutcome.isMatch(), message.because(reactiveOutcome.getMessage()));
    }
    return new ConditionOutcome(servletOutcome.isMatch() || reactiveOutcome.isMatch(),
            message.because(servletOutcome.getMessage()).append("and").append(reactiveOutcome.getMessage()));
  }

  private ConditionOutcome isServletWebApplication(ConditionEvaluationContext context) {
    ConditionMessage.Builder message = ConditionMessage.forCondition("");
    if (!ClassNameFilter.isPresent(ApplicationType.SERVLET_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("servlet web application classes").atAll());
    }
    if (context.getBeanFactory() != null) {
      String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
      if (ObjectUtils.containsElement(scopes, "session")) {
        return ConditionOutcome.match(message.foundExactly("'session' scope"));
      }
    }
    if (context.getResourceLoader() instanceof WebServletApplicationContext) {
      return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
    }
    return ConditionOutcome.noMatch(message.because("not a servlet web application"));
  }

  private ConditionOutcome isReactiveWebApplication(ConditionEvaluationContext context) {
    ConditionMessage.Builder message = ConditionMessage.forCondition("");
    if (!ClassNameFilter.isPresent(ApplicationType.NETTY_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
    }
    ResourceLoader resourceLoader = context.getResourceLoader();
    if (resourceLoader instanceof WebApplicationContext && !(resourceLoader instanceof WebServletApplicationContext)) {
      return ConditionOutcome.match(message.foundExactly("ReactiveWebApplicationContext"));
    }
    return ConditionOutcome.noMatch(message.because("not a reactive web application"));
  }

  private ApplicationType deduceType(AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnApplication.class.getName());
    if (attributes != null) {
      return (ApplicationType) attributes.get("type");
    }
    return ApplicationType.STANDARD;
  }

}
