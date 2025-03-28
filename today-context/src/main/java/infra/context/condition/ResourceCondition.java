/*
 * Copyright 2017 - 2024 the original author or authors.
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
import java.util.Arrays;

import infra.context.annotation.ConditionContext;
import infra.core.io.Resource;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link InfraCondition} used to check if a resource can be found using a
 * configurable property and optional default location(s).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 13:33
 */
public abstract class ResourceCondition extends InfraCondition {

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
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
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
          ConditionContext context, AnnotatedTypeMetadata metadata) {
    ArrayList<String> found = new ArrayList<>();
    for (String location : this.resourceLocations) {
      Resource resource = context.getResourceLoader().getResource(location);
      if (resource != null && resource.exists()) {
        found.add(location);
      }
    }
    if (found.isEmpty()) {
      ConditionMessage message = startConditionMessage()
              .didNotFind("resource", "resources")
              .items(ConditionMessage.Style.QUOTE, Arrays.asList(this.resourceLocations));
      return ConditionOutcome.noMatch(message);
    }
    ConditionMessage message = startConditionMessage()
            .found("resource", "resources")
            .items(ConditionMessage.Style.QUOTE, found);
    return ConditionOutcome.match(message);
  }

  protected final ConditionMessage.Builder startConditionMessage() {
    return ConditionMessage.forCondition("ResourceCondition", "(" + this.name + ")");
  }

}
