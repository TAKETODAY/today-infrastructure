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

package cn.taketoday.framework.diagnostics.analyzer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.InjectionPoint;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.context.condition.ConditionEvaluationReport.ConditionAndOutcome;
import cn.taketoday.context.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoSuchBeanDefinitionException}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 10:40
 */
class NoSuchBeanDefinitionFailureAnalyzer extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException>
        implements BeanFactoryAware {

  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private MetadataReaderFactory metadataReaderFactory;

  @Nullable
  private ConditionEvaluationReport report;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory);
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    this.metadataReaderFactory = new CachingMetadataReaderFactory(this.beanFactory.getBeanClassLoader());
    // Get early as won't be accessible once context has failed to start
    this.report = ConditionEvaluationReport.get(this.beanFactory);
  }

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause, @Nullable String description) {
    if (cause.getNumberOfBeansFound() != 0) {
      return null;
    }
    List<AutoConfigurationResult> autoConfigurationResults = getAutoConfigurationResults(cause);
    List<UserConfigurationResult> userConfigurationResults = getUserConfigurationResults(cause);
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
    Set<String> beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, type);
    return beanNames.stream()
            .map((beanName) -> new UserConfigurationResult(getFactoryMethodMetadata(beanName),
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

  private void collectReportedConditionOutcomes(
          NoSuchBeanDefinitionException cause, List<AutoConfigurationResult> results) {
    this.report.getConditionAndOutcomesBySource()
            .forEach((source, sourceOutcomes) -> collectReportedConditionOutcomes(cause, new Source(source),
                    sourceOutcomes, results));
  }

  private void collectReportedConditionOutcomes(
          NoSuchBeanDefinitionException cause, Source source,
          ConditionAndOutcomes sourceOutcomes, List<AutoConfigurationResult> results) {
    if (sourceOutcomes.isFullMatch()) {
      return;
    }
    BeanMethods methods = new BeanMethods(source, cause);
    for (ConditionAndOutcome conditionAndOutcome : sourceOutcomes) {
      if (!conditionAndOutcome.getOutcome().isMatch()) {
        for (MethodMetadata method : methods) {
          results.add(new AutoConfigurationResult(method, conditionAndOutcome.getOutcome()));
        }
      }
    }
  }

  private void collectExcludedAutoConfiguration(
          NoSuchBeanDefinitionException cause, List<AutoConfigurationResult> results) {
    for (String excludedClass : this.report.getExclusions()) {
      Source source = new Source(excludedClass);
      BeanMethods methods = new BeanMethods(source, cause);
      for (MethodMetadata method : methods) {
        String message = String.format("auto-configuration '%s' was excluded",
                ClassUtils.getShortName(excludedClass));
        results.add(new AutoConfigurationResult(method, new ConditionOutcome(false, message)));
      }
    }
  }

  @Nullable
  private InjectionPoint findInjectionPoint(Throwable failure) {
    UnsatisfiedDependencyException unsatisfiedDependencyException = findCause(failure,
            UnsatisfiedDependencyException.class);
    if (unsatisfiedDependencyException == null) {
      return null;
    }
    return unsatisfiedDependencyException.getInjectionPoint();
  }

  private static class Source {

    private final String className;

    @Nullable
    private final String methodName;

    Source(String source) {
      String[] tokens = source.split("#");
      this.className = (tokens.length > 1) ? tokens[0] : source;
      this.methodName = (tokens.length != 2) ? null : tokens[1];
    }

    String getClassName() {
      return this.className;
    }

    @Nullable
    String getMethodName() {
      return this.methodName;
    }

  }

  private class BeanMethods implements Iterable<MethodMetadata> {

    private final List<MethodMetadata> methods;

    BeanMethods(Source source, NoSuchBeanDefinitionException cause) {
      this.methods = findBeanMethods(source, cause);
    }

    private List<MethodMetadata> findBeanMethods(Source source, NoSuchBeanDefinitionException cause) {
      try {
        MetadataReader classMetadata = NoSuchBeanDefinitionFailureAnalyzer.this.metadataReaderFactory
                .getMetadataReader(source.getClassName());
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
      if (source.getMethodName() != null && !source.getMethodName().equals(candidate.getMethodName())) {
        return false;
      }
      String name = cause.getBeanName();
      ResolvableType resolvableType = cause.getResolvableType();
      return ((name != null && hasName(candidate, name))
              || (resolvableType != null && hasType(candidate, extractBeanType(resolvableType))));
    }

    private boolean hasName(MethodMetadata methodMetadata, String name) {
      Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(Component.class.getName());
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

    private UserConfigurationResult(@Nullable MethodMetadata methodMetadata, boolean nullBean) {
      this.methodMetadata = methodMetadata;
      this.nullBean = nullBean;
    }

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

