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

package infra.app.config.web;

import org.jspecify.annotations.Nullable;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.InfraCondition;
import infra.core.env.ConfigurableEnvironment;
import infra.core.type.AnnotatedTypeMetadata;
import infra.util.ClassUtils;

/**
 * {@link Condition} that checks whether or not the Framework resource handling chain is
 * enabled.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnEnabledResourceChain
 * @since 4.0 2022/2/18 23:03
 */
class OnEnabledResourceChainCondition extends InfraCondition {

  private static final String WEBJAR_ASSET_LOCATOR = "org.webjars.WebJarAssetLocator";

  private static final String WEBJAR_VERSION_LOCATOR = "org.webjars.WebJarVersionLocator";

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
    boolean fixed = getEnabledProperty(environment, "strategy.fixed.", false);
    boolean content = getEnabledProperty(environment, "strategy.content.", false);
    Boolean chain = getEnabledProperty(environment, "", null);
    Boolean match = infra.app.config.web.WebProperties.Resources.Chain.getEnabled(fixed, content, chain);
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnEnabledResourceChain.class);
    if (match == null) {
      if (ClassUtils.isPresent(WEBJAR_VERSION_LOCATOR, getClass().getClassLoader())) {
        return ConditionOutcome.match(message.found("class").items(WEBJAR_VERSION_LOCATOR));
      }
      if (ClassUtils.isPresent(WEBJAR_ASSET_LOCATOR, getClass().getClassLoader())) {
        return ConditionOutcome.match(message.found("class").items(WEBJAR_ASSET_LOCATOR));
      }
      return ConditionOutcome.noMatch(message.didNotFind("class").items(WEBJAR_VERSION_LOCATOR));
    }
    if (match) {
      return ConditionOutcome.match(message.because("enabled"));
    }
    return ConditionOutcome.noMatch(message.because("disabled"));
  }

  private Boolean getEnabledProperty(ConfigurableEnvironment environment, String key, @Nullable Boolean defaultValue) {
    String name = "web.resources.chain." + key + "enabled";
    return environment.getProperty(name, Boolean.class, defaultValue);
  }

}
