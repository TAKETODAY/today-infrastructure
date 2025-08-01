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

package infra.app.diagnostics.analyzer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.condition.ConditionEvaluationReport;
import infra.context.condition.ConditionEvaluationReport.ConditionAndOutcome;
import infra.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import infra.context.condition.ConditionOutcome;
import infra.core.ResolvableType;
import infra.core.type.MethodMetadata;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.ClassUtils;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoSuchBeanDefinitionException}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 10:40
 */
class NoSuchBeanDefinitionFailureAnalyzer extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException> {

  private final ConditionEvaluationReport report;

  private final ConfigurableBeanFactory beanFactory;

  private final MetadataReaderFactory metadataReaderFactory;

  NoSuchBeanDefinitionFailureAnalyzer(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    // Get early as won't be accessible once context has failed to start
    this.report = ConditionEvaluationReport.get(beanFactory);
    this.metadataReaderFactory = new CachingMetadataReaderFactory(beanFactory.getBeanClassLoader());
  }

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause, @Nullable String description) {
    if (cause.getNumberOfBeansFound() != 0) {
      return null;
    }
    var autoConfigurationResults = getAutoConfigurationResults(cause);
    var userConfigurationResults = getUserConfigurationResults(cause);
    StringBuilder message = new StringBuilder();
    message.append(String.format("%s required %s that could not be found.%n",
            (description != null) ? description : "A component", getBeanDescription(cause)));
    InjectionPoint injectionPoint = findInjectionPoint(rootFailure);
    if (injectionPoint != null) {
      Annotation[] injectionAnnotations = injectionPoint.getAnnotations();
      if (injectionAnnotations.length > 0) {
        message.append(String.format("%nThe injection point has the following annotations:%n"));
        for (Annotation injectionAnnotation : injectionAnnotations) {
          message.append(String.format("\t- %s%n", injectionAnnotation));
        }
      }
    }
    if (!autoConfigurationResults.isEmpty() || !userConfigurationResults.isEmpty()) {
      message.append(String.format("%nThe following candidates were found but could not be injected:%n"));
      for (AutoConfigurationResult result : autoConfigurationResults) {
        message.append(String.format("\t- %s%n", result));
      }
      for (UserConfigurationResult result : userConfigurationResults) {
        message.append(String.format("\t- %s%n", result));
      }
    }
    String action = String.format("Consider %s %s in your configuration.",
            (!autoConfigurationResults.isEmpty() || !userConfigurationResults.isEmpty())
                    ? "revisiting the entries above or defining" : "defining",
            getBeanDescription(cause));
    return new FailureAnalysis(message.toString(), action, cause);
  }

  private String getBeanDescription(NoSuchBeanDefinitionException cause) {
    if (cause.getResolvableType() != null) {
      Class<?> type = extractBeanType(cause.getResolvableType());
      return "a bean of type '" + type.getName() + "'";
    }
    return "a bean named '" + cause.getBeanName() + "'";
  }

  private Class<?> extractBeanType(ResolvableType resolvableType) {
    return resolvableType.getRawClass();
  }

  private List<AutoConfigurationResult> getAutoConfigurationResults(NoSuchBeanDefinitionException cause) {
    List<AutoConfigurationResult> results = new ArrayList<>();
    collectReportedConditionOutcomes(cause, results);
    collectExcludedAutoConfiguration(cause, results);
    return results;
  }

  private List<UserConfigurationResult> getUserConfigurationResults(NoSuchBeanDefinitionException cause) {
    ResolvableType type = cause.getResolvableType();
    if (type == null) {
      return Collections.emptyList();
    }
    var beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, type);
    return Arrays.stream(beanNames)
            .map(beanName -> new UserConfigurationResult(getFactoryMethodMetadata(beanName),
                    beanFactory.getBean(beanName) == null))
            .collect(Collectors.toList());
  }

  @Nullable
  private MethodMetadata getFactoryMethodMetadata(String beanName) {
    BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
    if (beanDefinition instanceof AnnotatedBeanDefinition) {
      return ((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata();
    }
    return null;
  }

  private void collectReportedConditionOutcomes(NoSuchBeanDefinitionException cause, List<AutoConfigurationResult> results) {
    report.getConditionAndOutcomesBySource()
            .forEach((source, sourceOutcomes) -> collectReportedConditionOutcomes(cause, new Source(source), sourceOutcomes, results));
  }

  private void collectReportedConditionOutcomes(NoSuchBeanDefinitionException cause,
          Source source, ConditionAndOutcomes sourceOutcomes, List<AutoConfigurationResult> results) {
    if (sourceOutcomes.isFullMatch()) {
      return;
    }
    BeanMethods methods = new BeanMethods(source, cause);
    for (ConditionAndOutcome outcomePair : sourceOutcomes) {
      if (!outcomePair.outcome.isMatch()) {
        for (MethodMetadata method : methods) {
          results.add(new AutoConfigurationResult(method, outcomePair.outcome));
        }
      }
    }
  }

  private void collectExcludedAutoConfiguration(NoSuchBeanDefinitionException cause, List<AutoConfigurationResult> results) {
    for (String excludedClass : report.getExclusions()) {
      Source source = new Source(excludedClass);
      BeanMethods methods = new BeanMethods(source, cause);
      for (MethodMetadata method : methods) {
        String message = String.format("auto-configuration '%s' was excluded", ClassUtils.getShortName(excludedClass));
        results.add(new AutoConfigurationResult(method, new ConditionOutcome(false, message)));
      }
    }
  }

  @Nullable
  private InjectionPoint findInjectionPoint(Throwable failure) {
    var unsatisfiedDependencyException = findCause(failure, UnsatisfiedDependencyException.class);
    if (unsatisfiedDependencyException == null) {
      return null;
    }
    return unsatisfiedDependencyException.getInjectionPoint();
  }

  private static class Source {

    public final String className;

    @Nullable
    public final String methodName;

    Source(String source) {
      String[] tokens = source.split("#");
      this.className = (tokens.length > 1) ? tokens[0] : source;
      this.methodName = (tokens.length != 2) ? null : tokens[1];
    }

  }

  private class BeanMethods implements Iterable<MethodMetadata> {

    private final List<MethodMetadata> methods;

    BeanMethods(Source source, NoSuchBeanDefinitionException cause) {
      this.methods = findBeanMethods(source, cause);
    }

    private List<MethodMetadata> findBeanMethods(Source source, NoSuchBeanDefinitionException cause) {
      try {
        MetadataReader classMetadata = metadataReaderFactory.getMetadataReader(source.className);
        Set<MethodMetadata> candidates = classMetadata.getAnnotationMetadata()
                .getAnnotatedMethods(Component.class.getName());
        List<MethodMetadata> result = new ArrayList<>();
        for (MethodMetadata candidate : candidates) {
          if (isMatch(candidate, source, cause)) {
            result.add(candidate);
          }
        }
        return Collections.unmodifiableList(result);
      }
      catch (Exception ex) {
        return Collections.emptyList();
      }
    }

    private boolean isMatch(MethodMetadata candidate, Source source, NoSuchBeanDefinitionException cause) {
      if (source.methodName != null && !source.methodName.equals(candidate.getMethodName())) {
        return false;
      }
      String name = cause.getBeanName();
      ResolvableType resolvableType = cause.getResolvableType();
      return ((name != null && hasName(candidate, name))
              || (resolvableType != null && hasType(candidate, extractBeanType(resolvableType))));
    }

    private boolean hasName(MethodMetadata methodMetadata, String name) {
      Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(Component.class);
      String[] candidates = (attributes != null) ? (String[]) attributes.get("name") : null;
      if (candidates != null) {
        for (String candidate : candidates) {
          if (candidate.equals(name)) {
            return true;
          }
        }
        return false;
      }
      return methodMetadata.getMethodName().equals(name);
    }

    private boolean hasType(MethodMetadata candidate, Class<?> type) {
      String returnTypeName = candidate.getReturnTypeName();
      if (type.getName().equals(returnTypeName)) {
        return true;
      }
      try {
        Class<?> returnType = ClassUtils.forName(returnTypeName,
                NoSuchBeanDefinitionFailureAnalyzer.this.beanFactory.getBeanClassLoader());
        return type.isAssignableFrom(returnType);
      }
      catch (Throwable ex) {
        return false;
      }
    }

    @Override
    public Iterator<MethodMetadata> iterator() {
      return this.methods.iterator();
    }

  }

  private record AutoConfigurationResult(MethodMetadata methodMetadata, ConditionOutcome conditionOutcome) {

    @Override
    public String toString() {
      return String.format("Bean method '%s' in '%s' not loaded because %s", this.methodMetadata.getMethodName(),
              ClassUtils.getShortName(this.methodMetadata.getDeclaringClassName()),
              this.conditionOutcome.getMessage());
    }

  }

  private record UserConfigurationResult(@Nullable MethodMetadata methodMetadata, boolean nullBean) {

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("User-defined bean");
      if (this.methodMetadata != null) {
        sb.append(String.format(" method '%s' in '%s'", this.methodMetadata.getMethodName(),
                ClassUtils.getShortName(this.methodMetadata.getDeclaringClassName())));
      }
      if (this.nullBean) {
        sb.append(" ignored as the bean value is null");
      }
      return sb.toString();
    }

  }

}

