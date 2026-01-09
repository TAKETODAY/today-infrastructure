/*
 * Copyright 2002-present the original author or authors.
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
