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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.annotation;

import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order
 * for a component to be registered.
 *
 * <p>Conditions are checked immediately before the bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 *
 * @author TODAY 2018-11-10 13:44
 * @see Conditional
 */
@FunctionalInterface
public interface Condition {

  /**
   * Determine if the condition matches.
   *
   * @param context ConditionEvaluationContext
   * @param metadata the metadata of the {@link cn.taketoday.core.type.AnnotationMetadata class}
   * * or {@link cn.taketoday.core.type.MethodMetadata method} being checked
   * @return Return {@code false} to indicate that the bean should not be
   * * registered
   * @since 4.0
   */
  boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata);

}
