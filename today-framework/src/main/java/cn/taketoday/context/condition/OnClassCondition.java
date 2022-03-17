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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} and that checks for the presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 * @since 4.0 2022/1/16 16:09
 */
final class OnClassCondition extends FilteringContextCondition implements Condition, Ordered {

  @Override
  public ConditionOutcome getMatchOutcome(
          ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    ClassLoader classLoader = context.getClassLoader();
    ConditionMessage matchMessage = ConditionMessage.empty();
    List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
    if (onClasses != null) {
      List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
      if (!missing.isEmpty()) {
        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
                .didNotFind("required class", "required classes").items(Style.QUOTE, missing));
      }
      matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
              .found("required class", "required classes")
              .items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
    }
    List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
    if (onMissingClasses != null) {
      List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
      if (!present.isEmpty()) {
        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
                .found("unwanted class", "unwanted classes").items(Style.QUOTE, present));
      }
      matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
              .didNotFind("unwanted class", "unwanted classes")
              .items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
    }
    return ConditionOutcome.match(matchMessage);
  }

  private List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType.getName(), true);
    if (attributes == null) {
      return null;
    }
    List<String> candidates = new ArrayList<>();
    addAll(candidates, attributes.get("value"));
    addAll(candidates, attributes.get("name"));
    return candidates;
  }

  private void addAll(List<String> list, List<Object> itemsToAdd) {
    if (itemsToAdd != null) {
      for (Object item : itemsToAdd) {
        Collections.addAll(list, (String[]) item);
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

}
