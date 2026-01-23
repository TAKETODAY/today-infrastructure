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

package infra.app.config;

import org.jspecify.annotations.Nullable;

import infra.app.ApplicationType;
import infra.app.config.ConditionalOnWebApplication.Type;
import infra.app.web.context.ConfigurableWebEnvironment;
import infra.app.web.context.reactive.ConfigurableReactiveWebEnvironment;
import infra.app.web.context.reactive.ReactiveWebApplicationContext;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.FilteringInfraCondition;
import infra.core.Ordered;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.util.ClassUtils;

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
    @Nullable ConditionOutcome[] outcomes = new ConditionOutcome[configClasses.length];
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
    if (ConditionalOnWebApplication.Type.MVC.name().equals(type)) {
      if (missingClassFilter.matches(ApplicationType.WEB_MVC_INDICATOR_CLASS, getBeanClassLoader())) {
        return ConditionOutcome.noMatch(message.didNotFind("Web MVC application classes").atAll());
      }
    }
    else if (ConditionalOnWebApplication.Type.REACTIVE.name().equals(type)) {
      if (missingClassFilter.matches(ApplicationType.REACTOR_INDICATOR_CLASS, getBeanClassLoader())) {
        return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
      }
    }
    if (missingClassFilter.matches(ApplicationType.WEB_INDICATOR_CLASS, getBeanClassLoader())
            && missingClassFilter.matches(ApplicationType.REACTOR_INDICATOR_CLASS, getBeanClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("reactive, web application classes").atAll());
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
      case MVC -> isMvcWebApplication(context);
      case REACTIVE -> isReactiveWebApplication(context);
      default -> isAnyWebApplication(context, required);
    };
  }

  private ConditionOutcome isAnyWebApplication(ConditionContext context, boolean required) {
    var message = ConditionMessage.forCondition(ConditionalOnWebApplication.class, required ? "(required)" : "");

    ConditionOutcome mvcOutcome = isMvcWebApplication(context);
    if (mvcOutcome.isMatch() && required) {
      return new ConditionOutcome(mvcOutcome.isMatch(), message.because(mvcOutcome.getMessage()));
    }
    ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
    if (reactiveOutcome.isMatch() && required) {
      return new ConditionOutcome(reactiveOutcome.isMatch(), message.because(reactiveOutcome.getMessage()));
    }
    return new ConditionOutcome(reactiveOutcome.isMatch() || mvcOutcome.isMatch(),
            message.because(mvcOutcome.getMessage())
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
   * web classes
   */
  private ConditionOutcome isMvcWebApplication(ConditionContext context) {
    var message = ConditionMessage.forCondition("");
    ClassNameFilter missingClassFilter = ClassNameFilter.MISSING;
    if (missingClassFilter.matches(ApplicationType.WEB_MVC_INDICATOR_CLASS, context.getClassLoader())) {
      return ConditionOutcome.noMatch(message.didNotFind("web mvc application classes").atAll());
    }

    if (context.getEnvironment() instanceof ConfigurableWebEnvironment) {
      return ConditionOutcome.match(message.foundExactly("ConfigurableWebEnvironment"));
    }

    Class<?> gwsac = ClassUtils.load("infra.web.server.context.GenericWebServerApplicationContext", context.getClassLoader());
    if (gwsac != null) {
      if (gwsac.isInstance(context.getResourceLoader())) {
        return ConditionOutcome.match(message.foundExactly("GenericWebServerApplicationContext"));
      }
    }
    return ConditionOutcome.noMatch(message.because("not a web application"));
  }

  private Type deduceType(AnnotatedTypeMetadata metadata) {
    var annotation = metadata.getAnnotation(ConditionalOnWebApplication.class);
    if (annotation.isPresent()) {
      return annotation.getEnum("type", Type.class);
    }
    return Type.ANY;
  }

}
