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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.lang.Assert;

/**
 * {@link Conditional} that only matches when the specified resources are exits
 *
 * @author TODAY <br>
 * 2019-06-18 15:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnResourceCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnResource {

  /**
   * The resources that must be present.
   *
   * @return the resource paths that must be present.
   */
  String[] value() default {};

}

final class OnResourceCondition extends ContextCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(ConditionalOnResource.class.getName(), true);
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
              .didNotFind("resource", "resources").items(Style.QUOTE, missing));
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
