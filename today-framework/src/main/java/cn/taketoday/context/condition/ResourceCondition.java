/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * {@link ContextCondition} used to check if a resource can be found using a
 * configurable property and optional default location(s).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 13:33
 */
public abstract class ResourceCondition extends ContextCondition {

  private final String name;

  private final String property;

  private final String[] resourceLocations;

  /**
   * Create a new condition.
   *
   * @param name the name of the component
   * @param property the configuration property
   * @param resourceLocations default location(s) where the configuration file can be
   * found if the configuration key is not specified
   */
  protected ResourceCondition(String name, String property, String... resourceLocations) {
    this.name = name;
    this.property = property;
    this.resourceLocations = resourceLocations;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    if (context.getEnvironment().containsProperty(this.property)) {
      return ConditionOutcome.match(startConditionMessage().foundExactly("property " + this.property));
    }
    return getResourceOutcome(context, metadata);
  }

  /**
   * Check if one of the default resource locations actually exists.
   *
   * @param context the condition context
   * @param metadata the annotation metadata
   * @return the condition outcome
   */
  protected ConditionOutcome getResourceOutcome(
          ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    List<String> found = new ArrayList<>();
    for (String location : this.resourceLocations) {
      Resource resource = context.getResourceLoader().getResource(location);
      if (resource != null && resource.exists()) {
        found.add(location);
      }
    }
    if (found.isEmpty()) {
      ConditionMessage message = startConditionMessage().didNotFind("resource", "resources").items(Style.QUOTE,
              Arrays.asList(this.resourceLocations));
      return ConditionOutcome.noMatch(message);
    }
    ConditionMessage message = startConditionMessage().found("resource", "resources").items(Style.QUOTE, found);
    return ConditionOutcome.match(message);
  }

  protected final ConditionMessage.Builder startConditionMessage() {
    return ConditionMessage.forCondition("ResourceCondition", "(" + this.name + ")");
  }

}
