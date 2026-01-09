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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.beans.BeanUtils;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.ConfigurationCondition;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.MultiValueMap;

/**
 * Abstract base class for nested conditions.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 17:55
 */
public abstract class AbstractNestedCondition extends InfraCondition implements ConfigurationCondition {

  private final ConfigurationPhase configurationPhase;

  AbstractNestedCondition(ConfigurationPhase configurationPhase) {
    Assert.notNull(configurationPhase, "ConfigurationPhase is required");
    this.configurationPhase = configurationPhase;
  }

  @Override
  public ConfigurationPhase getConfigurationPhase() {
    return this.configurationPhase;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String className = getClass().getName();
    MemberConditions memberConditions = new MemberConditions(context, this.configurationPhase, className);
    MemberMatchOutcomes memberOutcomes = new MemberMatchOutcomes(memberConditions);
    return getFinalMatchOutcome(memberOutcomes);
  }

  protected abstract ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes);

  protected static class MemberMatchOutcomes {

    public final List<ConditionOutcome> all;
    public final List<ConditionOutcome> matches;
    public final List<ConditionOutcome> nonMatches;

    public MemberMatchOutcomes(MemberConditions memberConditions) {
      this.all = memberConditions.getMatchOutcomes();
      ArrayList<ConditionOutcome> matches = new ArrayList<>();
      ArrayList<ConditionOutcome> nonMatches = new ArrayList<>();
      for (ConditionOutcome outcome : this.all) {
        (outcome.isMatch() ? matches : nonMatches).add(outcome);
      }
      this.matches = Collections.unmodifiableList(matches);
      this.nonMatches = Collections.unmodifiableList(nonMatches);
    }

  }

  private static class MemberConditions {

    private final ConditionContext context;
    private final MetadataReaderFactory readerFactory;

    private final Map<AnnotationMetadata, List<Condition>> memberConditions;

    MemberConditions(ConditionContext context, ConfigurationPhase phase, String className) {
      this.context = context;
      this.readerFactory = MetadataReaderFactory.create(context.getResourceLoader());
      String[] members = getMetadata(className).getMemberClassNames();
      this.memberConditions = getMemberConditions(members, phase, className);
    }

    private Map<AnnotationMetadata, List<Condition>> getMemberConditions(
            String[] members, ConfigurationPhase phase, String className) {
      var memberConditions = MultiValueMap.<AnnotationMetadata, Condition>forLinkedHashMap();
      for (String member : members) {
        AnnotationMetadata metadata = getMetadata(member);
        for (String[] conditionClasses : getConditionClasses(metadata)) {
          for (String conditionClass : conditionClasses) {
            Condition condition = getCondition(conditionClass);
            validateMemberCondition(condition, phase, className);
            memberConditions.add(metadata, condition);
          }
        }
      }
      return memberConditions;
    }

    private void validateMemberCondition(Condition condition, ConfigurationPhase nestedPhase, String nestedClassName) {
      if (nestedPhase == ConfigurationPhase.PARSE_CONFIGURATION
              && condition instanceof ConfigurationCondition ccd) {
        ConfigurationPhase memberPhase = ccd.getConfigurationPhase();
        if (memberPhase == ConfigurationPhase.REGISTER_BEAN) {
          throw new IllegalStateException("Nested condition %s uses a configuration phase that is inappropriate for %s"
                  .formatted(nestedClassName, condition.getClass()));
        }
      }
    }

    private AnnotationMetadata getMetadata(String className) {
      try {
        return this.readerFactory.getMetadataReader(className).getAnnotationMetadata();
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @SuppressWarnings("unchecked")
    private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
      var attributes = metadata.getAllAnnotationAttributes(Conditional.class, true);
      if (attributes != null) {
        Object values = attributes.get("value");
        if (values != null) {
          return (List<String[]>) values;
        }
      }
      return Collections.emptyList();
    }

    private Condition getCondition(String className) {
      var conditionClass = ClassUtils.<Condition>resolveClassName(className, context.getClassLoader());
      return BeanUtils.newInstance(conditionClass);
    }

    List<ConditionOutcome> getMatchOutcomes() {
      ArrayList<ConditionOutcome> outcomes = new ArrayList<>();
      for (Map.Entry<AnnotationMetadata, List<Condition>> entry : memberConditions.entrySet()) {
        AnnotationMetadata metadata = entry.getKey();
        List<Condition> conditions = entry.getValue();
        outcomes.add(new MemberOutcomes(context, metadata, conditions).getUltimateOutcome());
      }
      return Collections.unmodifiableList(outcomes);
    }

  }

  private static class MemberOutcomes {

    private final AnnotationMetadata metadata;
    private final List<ConditionOutcome> outcomes;
    private final ConditionContext context;

    MemberOutcomes(ConditionContext context, AnnotationMetadata metadata, List<Condition> conditions) {
      this.context = context;
      this.metadata = metadata;
      this.outcomes = new ArrayList<>(conditions.size());
      for (Condition condition : conditions) {
        this.outcomes.add(getConditionOutcome(metadata, condition));
      }
    }

    private ConditionOutcome getConditionOutcome(AnnotationMetadata metadata, Condition condition) {
      if (condition instanceof InfraCondition) {
        return ((InfraCondition) condition).getMatchOutcome(context, metadata);
      }
      return new ConditionOutcome(condition.matches(context, metadata), ConditionMessage.empty());
    }

    ConditionOutcome getUltimateOutcome() {
      var message = ConditionMessage.forCondition(
              "NestedCondition on " + ClassUtils.getShortName(metadata.getClassName()));
      if (outcomes.size() == 1) {
        ConditionOutcome outcome = outcomes.get(0);
        return new ConditionOutcome(outcome.isMatch(), message.because(outcome.getMessage()));
      }
      ArrayList<ConditionOutcome> match = new ArrayList<>();
      ArrayList<ConditionOutcome> nonMatch = new ArrayList<>();
      for (ConditionOutcome outcome : outcomes) {
        (outcome.isMatch() ? match : nonMatch).add(outcome);
      }
      if (nonMatch.isEmpty()) {
        return ConditionOutcome.match(message.found("matching nested conditions").items(match));
      }
      return ConditionOutcome.noMatch(message.found("non-matching nested conditions").items(nonMatch));
    }

  }

}
