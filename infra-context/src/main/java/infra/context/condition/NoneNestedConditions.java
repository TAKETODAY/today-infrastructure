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

import java.util.ArrayList;
import java.util.List;

import infra.context.annotation.Condition;

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

