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

package cn.taketoday.context.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.config.AutoConfigurationMetadata;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

/**
 * {@link Condition} and that checks for the presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 * @since 4.0 2022/1/16 16:09
 */
final class OnClassCondition extends FilteringInfraCondition implements Condition, Ordered {

  @Override
  protected ConditionOutcome[] getOutcomes(String[] configClasses, AutoConfigurationMetadata configMetadata) {
    // Split the work and perform half in a background thread if more than one
    // processor is available. Using a single additional thread seems to offer the
    // best performance. More threads make things worse.
    if (configClasses.length > 1 && Runtime.getRuntime().availableProcessors() > 1) {
      return resolveOutcomesThreaded(configClasses, configMetadata);
    }
    else {
      OutcomesResolver outcomesResolver = new StandardOutcomesResolver(configClasses, 0,
              configClasses.length, configMetadata, getBeanClassLoader());
      return outcomesResolver.resolveOutcomes();
    }
  }

  private ConditionOutcome[] resolveOutcomesThreaded(String[] configClasses, AutoConfigurationMetadata configMetadata) {
    int split = configClasses.length / 2;
    OutcomesResolver firstHalfResolver = createOutcomesResolver(configClasses, 0, split, configMetadata);
    OutcomesResolver secondHalfResolver = new StandardOutcomesResolver(
            configClasses, split, configClasses.length, configMetadata, getBeanClassLoader());

    ConditionOutcome[] secondHalf = secondHalfResolver.resolveOutcomes();
    ConditionOutcome[] firstHalf = firstHalfResolver.resolveOutcomes();
    ConditionOutcome[] outcomes = new ConditionOutcome[configClasses.length];
    System.arraycopy(firstHalf, 0, outcomes, 0, firstHalf.length);
    System.arraycopy(secondHalf, 0, outcomes, split, secondHalf.length);
    return outcomes;
  }

  private OutcomesResolver createOutcomesResolver(String[] autoConfigurationClasses,
          int start, int end, AutoConfigurationMetadata autoConfigurationMetadata) {
    var outcomesResolver = new StandardOutcomesResolver(
            autoConfigurationClasses, start, end, autoConfigurationMetadata, getBeanClassLoader());
    return new ThreadedOutcomesResolver(outcomesResolver);
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ClassLoader classLoader = context.getClassLoader();
    ConditionMessage matchMessage = ConditionMessage.empty();
    List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
    if (onClasses != null) {
      List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
      if (!missing.isEmpty()) {
        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
                .didNotFind("required class", "required classes")
                .items(Style.QUOTE, missing));
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
                .found("unwanted class", "unwanted classes")
                .items(Style.QUOTE, present));
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

  private interface OutcomesResolver {

    ConditionOutcome[] resolveOutcomes();

  }

  private static final class ThreadedOutcomesResolver implements OutcomesResolver, Runnable {

    private final Thread thread;
    private final OutcomesResolver outcomesResolver;

    private volatile ConditionOutcome[] outcomes;

    private ThreadedOutcomesResolver(OutcomesResolver outcomesResolver) {
      this.thread = new Thread(this);
      this.outcomesResolver = outcomesResolver;
      this.thread.start();
    }

    @Override
    public void run() {
      this.outcomes = outcomesResolver.resolveOutcomes();
    }

    @Override
    public ConditionOutcome[] resolveOutcomes() {
      try {
        this.thread.join();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return this.outcomes;
    }

  }

  private record StandardOutcomesResolver(
          String[] configClasses, int start, int end,
          AutoConfigurationMetadata configMetadata, ClassLoader beanClassLoader) implements OutcomesResolver {

    @Override
    public ConditionOutcome[] resolveOutcomes() {
      return getOutcomes(this.configClasses, this.start, this.end, this.configMetadata);
    }

    private ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
            int start, int end, AutoConfigurationMetadata autoConfigurationMetadata) {
      ConditionOutcome[] outcomes = new ConditionOutcome[end - start];
      for (int i = start; i < end; i++) {
        String autoConfigurationClass = autoConfigurationClasses[i];
        if (autoConfigurationClass != null) {
          String candidates = autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnClass");
          if (candidates != null) {
            outcomes[i - start] = getOutcome(candidates);
          }
        }
      }
      return outcomes;
    }

    private ConditionOutcome getOutcome(String candidates) {
      try {
        if (!candidates.contains(",")) {
          return getOutcome(candidates, this.beanClassLoader);
        }
        for (String candidate : StringUtils.commaDelimitedListToStringArray(candidates)) {
          ConditionOutcome outcome = getOutcome(candidate, this.beanClassLoader);
          if (outcome != null) {
            return outcome;
          }
        }
      }
      catch (Exception ex) {
        // We'll get another chance later
      }
      return null;
    }

    private ConditionOutcome getOutcome(String className, ClassLoader classLoader) {
      if (ClassNameFilter.MISSING.matches(className, classLoader)) {
        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
                .didNotFind("required class")
                .items(Style.QUOTE, className));
      }
      return null;
    }

  }

}
