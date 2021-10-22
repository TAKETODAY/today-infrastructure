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

package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.StandardMethodMetadata;
import cn.taketoday.lang.Assert;

import java.lang.reflect.Method;

/**
 * Condition Evaluation
 *
 * @author TODAY 2021/10/1 21:12
 * @see Condition
 * @since 4.0
 */
public class ConditionEvaluator {
  private final ConditionEvaluationContext evaluationContext;

  public ConditionEvaluator(ApplicationContext context, BeanDefinitionRegistry registry) {
    this.evaluationContext = new ConditionEvaluationContext(context, registry);
  }

  /**
   * Determine if an item should be skipped based on {@code @Conditional} annotations.
   *
   * @param metadata the meta data
   * @return if the item should be skipped
   */
  public boolean passCondition(AnnotatedTypeMetadata metadata) {
    AnnotationAttributes[] attributes = metadata.getAnnotations().getAttributes(Conditional.class);
    for (AnnotationAttributes attribute : attributes) {
      Class<? extends Condition>[] classArray = attribute.getClassArray(MergedAnnotation.VALUE);
      if (!passCondition(metadata, classArray)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decide whether to load the bean
   *
   * @param annotated Target class or a method
   * @return If matched
   */
  public boolean passCondition(Class<?> annotated) {
    AnnotationMetadata introspect = AnnotationMetadata.introspect(annotated);
    return passCondition(introspect);
  }

  public boolean passCondition(Method annotated) {
    StandardMethodMetadata standardMethodMetadata = new StandardMethodMetadata(annotated);
    return passCondition(standardMethodMetadata);
  }

  public boolean passCondition(
          AnnotatedTypeMetadata metadata,
          Class<? extends Condition>[] condition
  ) {
    Assert.notNull(condition, "Condition Class must not be null");
    ApplicationContext context = evaluationContext.getContext();
    for (Class<? extends Condition> conditionClass : condition) {
      // TODO
      if (!BeanUtils.newInstance(conditionClass, context).matches(evaluationContext, metadata)) {
        return false; // can't match
      }
    }
    return true;
  }

}
