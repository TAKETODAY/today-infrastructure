/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.annotation.config.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.condition.ConditionMessage;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.context.condition.InfraCondition;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.util.ClassUtils;

/**
 * {@link Conditional @Conditional} that checks whether or not the Framework resource
 * handling chain is enabled. Matches if
 * {@link WebProperties.Resources.Chain#getEnabled()} is {@code true} or if
 * {@code webjars-locator-core} is on the classpath.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 23:03
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnEnabledResourceChainCondition.class)
public @interface ConditionalOnEnabledResourceChain {

}

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

  @Override
  public ConditionOutcome getMatchOutcome(
          ConditionContext context, AnnotatedTypeMetadata metadata) {
    ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
    boolean fixed = getEnabledProperty(environment, "strategy.fixed.", false);
    boolean content = getEnabledProperty(environment, "strategy.content.", false);
    Boolean chain = getEnabledProperty(environment, "", null);
    Boolean match = WebProperties.Resources.Chain.getEnabled(fixed, content, chain);
    ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnEnabledResourceChain.class);
    if (match == null) {
      if (ClassUtils.isPresent(WEBJAR_ASSET_LOCATOR, getClass().getClassLoader())) {
        return ConditionOutcome.match(message.found("class").items(WEBJAR_ASSET_LOCATOR));
      }
      return ConditionOutcome.noMatch(message.didNotFind("class").items(WEBJAR_ASSET_LOCATOR));
    }
    if (match) {
      return ConditionOutcome.match(message.because("enabled"));
    }
    return ConditionOutcome.noMatch(message.because("disabled"));
  }

  private Boolean getEnabledProperty(ConfigurableEnvironment environment, String key, Boolean defaultValue) {
    String name = "web.resources.chain." + key + "enabled";
    return environment.getProperty(name, Boolean.class, defaultValue);
  }

}
