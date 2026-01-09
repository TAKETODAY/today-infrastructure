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

package infra.context.condition;

import org.jspecify.annotations.Nullable;

import javax.naming.NamingException;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.core.Ordered;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotatedTypeMetadata;
import infra.jndi.JndiLocatorDelegate;
import infra.jndi.JndiLocatorSupport;
import infra.util.StringUtils;

/**
 * {@link Condition} that checks for JNDI locations.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnJndi
 * @since 4.0 2022/1/17 14:52
 */
class OnJndiCondition extends InfraCondition implements Ordered {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    MergedAnnotation<ConditionalOnJndi> annotation = metadata.getAnnotation(ConditionalOnJndi.class);
    String[] locations = annotation.getStringValueArray();
    try {
      return getMatchOutcome(locations);
    }
    catch (NoClassDefFoundError ex) {
      return ConditionOutcome
              .noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class).because("JNDI class not found"));
    }
  }

  private ConditionOutcome getMatchOutcome(String[] locations) {
    if (!isJndiAvailable()) {
      return ConditionOutcome.noMatch(
              ConditionMessage.forCondition(ConditionalOnJndi.class).notAvailable("JNDI environment"));
    }
    if (locations.length == 0) {
      return ConditionOutcome.match(
              ConditionMessage.forCondition(ConditionalOnJndi.class).available("JNDI environment"));
    }
    JndiLocator locator = getJndiLocator(locations);
    String location = locator.lookupFirstLocation();
    String details = "(" + StringUtils.arrayToCommaDelimitedString(locations) + ")";
    if (location != null) {
      return ConditionOutcome.match(
              ConditionMessage.forCondition(ConditionalOnJndi.class, details)
                      .foundExactly("\"%s\"".formatted(location))
      );
    }
    return ConditionOutcome.noMatch(
            ConditionMessage.forCondition(ConditionalOnJndi.class, details)
                    .didNotFind("any matching JNDI location").atAll()
    );
  }

  protected boolean isJndiAvailable() {
    return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
  }

  protected JndiLocator getJndiLocator(String[] locations) {
    return new JndiLocator(locations);
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 20;
  }

  protected static class JndiLocator extends JndiLocatorSupport {

    private final String[] locations;

    public JndiLocator(String[] locations) {
      this.locations = locations;
    }

    @Nullable
    public String lookupFirstLocation() {
      for (String location : this.locations) {
        try {
          lookup(location);
          return location;
        }
        catch (NamingException ex) {
          // Swallow and continue
        }
      }
      return null;
    }

  }

}
