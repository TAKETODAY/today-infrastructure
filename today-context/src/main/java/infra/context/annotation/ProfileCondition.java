/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import infra.core.env.Environment;
import infra.core.type.AnnotatedTypeMetadata;
import infra.util.MultiValueMap;

/**
 * {@link Condition} that matches based on the value of a {@link Profile @Profile}
 * annotation.
 *
 * @author TODAY 2018-11-14 18:52
 */
final class ProfileCondition implements Condition {

  @Override
  @SuppressWarnings("NullAway") // Reflection
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    MultiValueMap<String, @Nullable Object> attrs = metadata.getAllAnnotationAttributes(Profile.class);
    if (attrs != null) {
      Environment environment = context.getEnvironment();
      for (Object value : attrs.get("value")) {
        if (environment.matchesProfiles((String[]) value)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

}
