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

import cn.taketoday.context.Condition;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.loader.ConditionEvaluationContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.util.ClassUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Conditional} that only matches when the specified classes are on the
 * classpath.
 *
 * @author TODAY <br>
 * 2019-06-18 15:00
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

  /**
   * The classes that must be present
   *
   * @return the classes that must be present
   */
  String[] value() default { };

}

final class OnClassCondition implements Condition {

  @Override
  public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    MergedAnnotation<ConditionalOnClass> conditionalOnClass = metadata.getAnnotations().get(ConditionalOnClass.class);
    if (conditionalOnClass.isPresent()) {
      String[] classArray = conditionalOnClass.getStringArray(MergedAnnotation.VALUE);
      for (String name : classArray) {
        if (!ClassUtils.isPresent(name)) {
          return false;
        }
      }
    }

    MergedAnnotation<ConditionalOnClass> onMissingClass = metadata.getAnnotations().get(ConditionalOnClass.class);
    if (onMissingClass.isPresent()) {
      String[] classArray = onMissingClass.getStringArray(MergedAnnotation.VALUE);
      for (String name : classArray) {
        if (ClassUtils.isPresent(name)) {
          return false;
        }
      }
    }
    return true;
  }


}
