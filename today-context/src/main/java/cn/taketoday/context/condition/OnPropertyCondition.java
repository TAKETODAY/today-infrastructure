/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.util.List;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotationPredicates;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnProperty
 * @since 4.0 2022/1/16 17:52
 */
class OnPropertyCondition extends InfraCondition implements Ordered {

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE + 40;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    List<AnnotationAttributes> allAnnotationAttributes = metadata.getAnnotations()
            .stream(ConditionalOnProperty.class)
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .map(MergedAnnotation::asAnnotationAttributes)
            .toList();

    var match = new ArrayList<ConditionMessage>();
    var noMatch = new ArrayList<ConditionMessage>();
    for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
      ConditionOutcome outcome = determineOutcome(annotationAttributes, context.getEnvironment());
      (outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
    }
    if (!noMatch.isEmpty()) {
      return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
    }
    return ConditionOutcome.match(ConditionMessage.of(match));
  }

  private ConditionOutcome determineOutcome(AnnotationAttributes attributes, PropertyResolver resolver) {
    Spec spec = new Spec(attributes);
    var missingProperties = new ArrayList<String>();
    var nonMatchingProperties = new ArrayList<String>();
    spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
    if (!missingProperties.isEmpty()) {
      return ConditionOutcome.noMatch(
              ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
                      .didNotFind("property", "properties").items(Style.QUOTE, missingProperties));
    }
    if (!nonMatchingProperties.isEmpty()) {
      return ConditionOutcome.noMatch(
              ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
                      .found("different value in property", "different value in properties")
                      .items(Style.QUOTE, nonMatchingProperties));
    }
    return ConditionOutcome.match(
            ConditionMessage.forCondition(ConditionalOnProperty.class, spec).because("matched"));
  }

  private static class Spec {

    private final String prefix;
    private final String[] names;
    private final String havingValue;
    private final boolean matchIfMissing;

    Spec(AnnotationAttributes attributes) {
      String prefix = attributes.getString("prefix").trim();
      if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
        prefix = prefix + ".";
      }
      this.prefix = prefix;
      this.names = getNames(attributes);
      this.havingValue = attributes.getString("havingValue");
      this.matchIfMissing = attributes.getBoolean("matchIfMissing");
    }

    private String[] getNames(AnnotationAttributes attributes) {
      String[] name = (String[]) attributes.get("name");
      String[] value = (String[]) attributes.get("value");
      Assert.state(value.length > 0 || name.length > 0, "The name or value attribute of @ConditionalOnProperty must be specified");
      Assert.state(value.length == 0 || name.length == 0, "The name and value attributes of @ConditionalOnProperty are exclusive");
      return (value.length > 0) ? value : name;
    }

    private void collectProperties(PropertyResolver resolver, List<String> missing, List<String> nonMatching) {
      for (String name : this.names) {
        String key = this.prefix + name;
        if (resolver.containsProperty(key)) {
          if (!isMatch(resolver.getProperty(key), this.havingValue)) {
            nonMatching.add(name);
          }
        }
        else {
          if (!this.matchIfMissing) {
            missing.add(name);
          }
        }
      }
    }

    private boolean isMatch(String value, String requiredValue) {
      if (StringUtils.isNotEmpty(requiredValue)) {
        return requiredValue.equalsIgnoreCase(value);
      }
      return !"false".equalsIgnoreCase(value);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("(");
      result.append(this.prefix);
      if (this.names.length == 1) {
        result.append(this.names[0]);
      }
      else {
        result.append("[");
        result.append(StringUtils.arrayToCommaDelimitedString(this.names));
        result.append("]");
      }
      if (StringUtils.isNotEmpty(this.havingValue)) {
        result.append("=").append(this.havingValue);
      }
      result.append(")");
      return result.toString();
    }

  }

}
