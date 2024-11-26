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

package infra.context.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.beans.BeanUtils;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ApplicationContext;
import infra.context.annotation.ConfigurationCondition.ConfigurationPhase;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.MultiValueMap;

/**
 * Condition Evaluation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Condition
 * @since 4.0 2021/10/1 21:12
 */
public class ConditionEvaluator {

  private final ConditionContext evaluationContext;

  public ConditionEvaluator(ApplicationContext context, BeanDefinitionRegistry registry) {
    this.evaluationContext = new ConditionContext(context, registry);
  }

  /**
   * Create a new {@link ConditionEvaluator} instance.
   */
  public ConditionEvaluator(@Nullable Environment environment,
          @Nullable ResourceLoader resourceLoader, @Nullable BeanDefinitionRegistry registry) {
    this.evaluationContext = new ConditionContext(registry, environment, resourceLoader);
  }

  /**
   * Determine if an item should be skipped based on {@code @Conditional} annotations.
   *
   * @param metadata the meta data
   * @return if the item should be skipped
   */
  public boolean passCondition(AnnotatedTypeMetadata metadata) {
    return passCondition(metadata, null);
  }

  public boolean passCondition(AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    return !shouldSkip(metadata, phase);
  }

  //

  /**
   * Determine if an item should be skipped based on {@code @Conditional} annotations.
   * The {@link ConfigurationPhase} will be deduced from the type of item (i.e. a
   * {@code @Configuration} class will be {@link ConfigurationPhase#PARSE_CONFIGURATION})
   *
   * @param metadata the meta data
   * @return if the item should be skipped
   */
  public boolean shouldSkip(AnnotatedTypeMetadata metadata) {
    return shouldSkip(metadata, null);
  }

  /**
   * Determine if an item should be skipped based on {@code @Conditional} annotations.
   *
   * @param metadata the meta data
   * @param phase the phase of the call
   * @return if the item should be skipped
   */
  public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {
      return false;
    }

    if (phase == null) {
      if (metadata instanceof AnnotationMetadata am
              && ConfigurationClassUtils.isConfigurationCandidate(am)) {
        return shouldSkip(metadata, ConfigurationPhase.PARSE_CONFIGURATION);
      }
      return shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN);
    }

    for (Condition condition : collectConditions(metadata)) {
      ConfigurationPhase requiredPhase = null;
      if (condition instanceof ConfigurationCondition cc) {
        requiredPhase = cc.getConfigurationPhase();
      }
      if ((requiredPhase == null || requiredPhase == phase)
              && !condition.matches(this.evaluationContext, metadata)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Clear the local metadata cache, if any, removing all cached metadata.
   */
  public void clearCache() {
    evaluationContext.close();
  }

  /**
   * Return the {@linkplain Condition conditions} that should be applied when
   * considering the given annotated type.
   *
   * @param metadata the metadata of the annotated type
   * @return the ordered list of conditions for that type
   */
  List<Condition> collectConditions(@Nullable AnnotatedTypeMetadata metadata) {
    if (metadata == null || !metadata.isAnnotated(Conditional.class)) {
      return Collections.emptyList();
    }

    ArrayList<Condition> conditions = new ArrayList<>();
    ClassLoader classLoader = evaluationContext.getClassLoader();
    for (String[] conditionClasses : getConditionClasses(metadata)) {
      for (String conditionClass : conditionClasses) {
        Condition condition = getCondition(conditionClass, classLoader);
        conditions.add(condition);
      }
    }

    AnnotationAwareOrderComparator.sort(conditions);
    return conditions;
  }

  @SuppressWarnings("unchecked")
  private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(), true);
    Object values = (attributes != null ? attributes.get("value") : null);
    return (List<String[]>) (values != null ? values : Collections.emptyList());
  }

  private Condition getCondition(String conditionClassName, @Nullable ClassLoader classloader) {
    Class<Condition> conditionClass = ClassUtils.resolveClassName(conditionClassName, classloader);
    return BeanUtils.newInstance(conditionClass);
  }

}
