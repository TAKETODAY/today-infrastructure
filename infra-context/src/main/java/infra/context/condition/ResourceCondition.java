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
