/**
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.context.Condition;
import cn.taketoday.context.Conditional;
import cn.taketoday.context.loader.ConditionEvaluationContext;
import cn.taketoday.lang.Constant;
import cn.taketoday.core.env.Environment;
import cn.taketoday.util.StringUtils;

/**
 * {@link Conditional} that checks if the specified properties have a specific
 * value. By default the properties must be present in the {@link Environment}
 *
 * @author TODAY <br>
 * 2019-06-18 15:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnProperty {

  /**
   * property names
   *
   * @return the names
   */
  String[] value() default {};

  /**
   * A prefix that should be applied to each property. The prefix automatically
   * ends with a dot if not specified. A valid prefix is defined by one or more
   * words separated with dots (e.g. {@code "acme.system.feature"}).
   *
   * @return the prefix
   */
  String prefix() default Constant.BLANK;

}

final class OnPropertyCondition implements Condition {

  @Override
  public boolean matches(ConditionEvaluationContext context, AnnotatedElement annotated) {
    ConditionalOnProperty conditionalOnProperty = annotated.getAnnotation(ConditionalOnProperty.class);
    String prefix = conditionalOnProperty.prefix();

    Environment environment = context.getEnvironment();
    if (StringUtils.isEmpty(prefix)) {
      for (String key : conditionalOnProperty.value()) {
        if (!environment.containsProperty(key)) {
          return false;
        }
      }
    }
    else {
      for (String key : conditionalOnProperty.value()) {
        if (!environment.containsProperty(prefix + key)) {
          return false;
        }
      }
    }
    return true;
  }

}
