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

import java.util.ArrayList;
import java.util.List;

import infra.context.annotation.ConditionContext;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.lang.Assert;
import infra.util.MultiValueMap;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/7 17:15
 */
final class OnResourceCondition extends InfraCondition {

  @Override
  @SuppressWarnings("NullAway")
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(ConditionalOnResource.class, true);
    ResourceLoader loader = context.getResourceLoader();
    List<String> locations = new ArrayList<>();
    collectValues(locations, attributes.get("value"));
    Assert.isTrue(!locations.isEmpty(), "@ConditionalOnResource annotations must specify at least one resource location");
    ArrayList<String> missing = new ArrayList<>();
    for (String location : locations) {
      String resource = context.getEnvironment().resolvePlaceholders(location);
      if (!loader.getResource(resource).exists()) {
        missing.add(location);
      }
    }
    if (!missing.isEmpty()) {
      return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnResource.class)
              .didNotFind("resource", "resources").items(ConditionMessage.Style.QUOTE, missing));
    }
    return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnResource.class)
            .found("location", "locations").items(locations));
  }

  private void collectValues(List<String> names, List<Object> values) {
    for (Object value : values) {
      for (Object item : (Object[]) value) {
        names.add((String) item);
      }
    }
  }
}
