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
