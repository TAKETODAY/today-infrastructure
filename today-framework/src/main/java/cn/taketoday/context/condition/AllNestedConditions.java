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

package cn.taketoday.context.condition;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.Condition;

/**
 * {@link Condition} that will match when all nested class conditions match. Can be used
 * to create composite conditions, for example:
 *
 * <pre class="code">
 * static class OnJndiAndProperty extends AllNestedConditions {
 *
 *    OnJndiAndProperty() {
 *        super(ConfigurationPhase.PARSE_CONFIGURATION);
 *    }
 *
 *    &#064;ConditionalOnJndi()
 *    static class OnJndi {
 *    }
 *
 *    &#064;ConditionalOnProperty("something")
 *    static class OnProperty {
 *    }
 *
 * }
 * </pre>
 * <p>
 * The
 * {@link cn.taketoday.context.annotation.ConfigurationCondition.ConfigurationPhase
 * ConfigurationPhase} should be specified according to the conditions that are defined.
 * In the example above, all conditions are static and can be evaluated early so
 * {@code PARSE_CONFIGURATION} is a right fit.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 12:15
 */
public abstract class AllNestedConditions extends AbstractNestedCondition {

  public AllNestedConditions(ConfigurationPhase configurationPhase) {
    super(configurationPhase);
  }

  @Override
  protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
    boolean match = hasSameSize(memberOutcomes.getMatches(), memberOutcomes.getAll());
    List<ConditionMessage> messages = new ArrayList<>();
    messages.add(ConditionMessage.forCondition("AllNestedConditions")
            .because(memberOutcomes.getMatches().size() +
                    " matched " + memberOutcomes.getNonMatches().size() + " did not"));

    for (ConditionOutcome outcome : memberOutcomes.getAll()) {
      messages.add(outcome.getConditionMessage());
    }
    return new ConditionOutcome(match, ConditionMessage.of(messages));
  }

  private boolean hasSameSize(List<?> list1, List<?> list2) {
    return list1.size() == list2.size();
  }

}
