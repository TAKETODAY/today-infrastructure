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

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ObjectUtils;

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
   * Decide whether to load the bean
   *
   * @param annotated Target class or a method
   * @return If matched
   */
  public boolean passCondition(final AnnotatedElement annotated) {
    final AnnotationAttributes[] attributes =
            AnnotationUtils.getAttributesArray(annotated, Conditional.class);
    if (ObjectUtils.isNotEmpty(attributes)) {
      if (attributes.length == 1) {
        return passCondition(annotated, attributes[0].getClassArray(Constant.VALUE));
      }
      AnnotationAwareOrderComparator.sort(attributes);
      for (final AnnotationAttributes conditional : attributes) {
        if (!passCondition(annotated, conditional.getClassArray(Constant.VALUE))) {
          return false; // can't match
        }
      }
    }
    return true;
  }

  public boolean passCondition(
          final AnnotatedElement annotated,
          final Class<? extends Condition>[] condition
  ) {
    Assert.notNull(condition, "Condition Class must not be null");
    ApplicationContext context = evaluationContext.getContext();
    for (final Class<? extends Condition> conditionClass : condition) {
      if (!BeanUtils.newInstance(conditionClass, context).matches(evaluationContext, annotated)) {
        return false; // can't match
      }
    }
    return true;
  }

}
