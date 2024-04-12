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

package cn.taketoday.context.condition;

import javax.naming.NamingException;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.jndi.JndiLocatorDelegate;
import cn.taketoday.jndi.JndiLocatorSupport;
import cn.taketoday.util.StringUtils;

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
