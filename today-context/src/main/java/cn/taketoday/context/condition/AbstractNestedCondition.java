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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Abstract base class for nested conditions.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 17:55
 */
public abstract class AbstractNestedCondition
        extends InfraCondition implements ConfigurationCondition {

  private final ConfigurationPhase configurationPhase;

  AbstractNestedCondition(ConfigurationPhase configurationPhase) {
    Assert.notNull(configurationPhase, "ConfigurationPhase must not be null");
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

    private final List<ConditionOutcome> all;
    private final List<ConditionOutcome> matches;
    private final List<ConditionOutcome> nonMatches;

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

    public List<ConditionOutcome> getAll() {
      return this.all;
    }

    public List<ConditionOutcome> getMatches() {
      return this.matches;
    }

    public List<ConditionOutcome> getNonMatches() {
      return this.nonMatches;
    }

  }

  private static class MemberConditions {

    private final ConditionContext context;
    private final MetadataReaderFactory readerFactory;

    private final Map<AnnotationMetadata, List<Condition>> memberConditions;

    MemberConditions(ConditionContext context, ConfigurationPhase phase, String className) {
      this.context = context;
      this.readerFactory = new SimpleMetadataReaderFactory(context.getResourceLoader());
      String[] members = getMetadata(className).getMemberClassNames();
      this.memberConditions = getMemberConditions(members, phase, className);
    }

    private Map<AnnotationMetadata, List<Condition>> getMemberConditions(
            String[] members, ConfigurationPhase phase, String className) {
      MultiValueMap<AnnotationMetadata, Condition> memberConditions = MultiValueMap.fromLinkedHashMap();
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
      return Collections.unmodifiableMap(memberConditions);
    }

    private void validateMemberCondition(
            Condition condition, ConfigurationPhase nestedPhase, String nestedClassName) {
      if (nestedPhase == ConfigurationPhase.PARSE_CONFIGURATION
              && condition instanceof ConfigurationCondition) {
        ConfigurationPhase memberPhase = ((ConfigurationCondition) condition).getConfigurationPhase();
        if (memberPhase == ConfigurationPhase.REGISTER_BEAN) {
          throw new IllegalStateException("Nested condition " + nestedClassName + " uses a configuration "
                  + "phase that is inappropriate for " + condition.getClass());
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
      MultiValueMap<String, Object> attributes =
              metadata.getAllAnnotationAttributes(Conditional.class.getName(), true);
      Object values = (attributes != null) ? attributes.get("value") : null;
      return (List<String[]>) ((values != null) ? values : Collections.emptyList());
    }

    private Condition getCondition(String conditionClassName) {
      Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName, this.context.getClassLoader());
      return (Condition) BeanUtils.newInstance(conditionClass);
    }

    List<ConditionOutcome> getMatchOutcomes() {
      ArrayList<ConditionOutcome> outcomes = new ArrayList<>();
      for (Map.Entry<AnnotationMetadata, List<Condition>> entry : memberConditions.entrySet()) {
        AnnotationMetadata metadata = entry.getKey();
        List<Condition> conditions = entry.getValue();
        outcomes.add(new MemberOutcomes(this.context, metadata, conditions).getUltimateOutcome());
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
        return ((InfraCondition) condition).getMatchOutcome(this.context, metadata);
      }
      return new ConditionOutcome(condition.matches(this.context, metadata), ConditionMessage.empty());
    }

    ConditionOutcome getUltimateOutcome() {
      ConditionMessage.Builder message = ConditionMessage.forCondition(
              "NestedCondition on " + ClassUtils.getShortName(this.metadata.getClassName()));
      if (this.outcomes.size() == 1) {
        ConditionOutcome outcome = this.outcomes.get(0);
        return new ConditionOutcome(outcome.isMatch(), message.because(outcome.getMessage()));
      }
      ArrayList<ConditionOutcome> match = new ArrayList<>();
      ArrayList<ConditionOutcome> nonMatch = new ArrayList<>();
      for (ConditionOutcome outcome : this.outcomes) {
        (outcome.isMatch() ? match : nonMatch).add(outcome);
      }
      if (nonMatch.isEmpty()) {
        return ConditionOutcome.match(message.found("matching nested conditions").items(match));
      }
      return ConditionOutcome.noMatch(message.found("non-matching nested conditions").items(nonMatch));
    }

  }

}
