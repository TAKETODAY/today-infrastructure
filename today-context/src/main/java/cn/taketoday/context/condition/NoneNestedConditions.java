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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.Condition;

/**
 * {@link Condition} that will match when none of the nested class conditions match. Can
 * be used to create composite conditions, for example:
 *
 * <pre>{@code
 * static class OnNeitherJndiNorProperty extends NoneNestedConditions {
 *
 *    OnNeitherJndiNorProperty() {
 *        super(ConfigurationPhase.PARSE_CONFIGURATION);
 *    }
 *
 *    @ConditionalOnJndi()
 *    static class OnJndi {
 *    }
 *
 *    @ConditionalOnProperty("something")
 *    static class OnProperty {
 *    }
 *
 * }
 * }</pre>
 * <p>
 * The
 * {@link ConfigurationPhase ConfigurationPhase} should be specified according
 * to the conditions that are defined. In the example above, all conditions
 * are static and can be evaluated early so {@code PARSE_CONFIGURATION} is a right fit.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 18:06
 */
public abstract class NoneNestedConditions extends AbstractNestedCondition {

  public NoneNestedConditions(ConfigurationPhase configurationPhase) {
    super(configurationPhase);
  }

  @Override
  protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
    boolean match = memberOutcomes.matches.isEmpty();
    List<ConditionMessage> messages = new ArrayList<>();
    messages.add(ConditionMessage.forCondition("NoneNestedConditions")
            .because("%d matched %d did not".formatted(memberOutcomes.matches.size(), memberOutcomes.nonMatches.size())));
    for (ConditionOutcome outcome : memberOutcomes.all) {
      messages.add(outcome.getConditionMessage());
    }
    return new ConditionOutcome(match, ConditionMessage.of(messages));
  }

}

