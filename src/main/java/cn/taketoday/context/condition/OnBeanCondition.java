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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.HierarchicalBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotation.Adapt;
import cn.taketoday.core.annotation.MergedAnnotationCollectors;
import cn.taketoday.core.annotation.MergedAnnotationPredicates;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Jakub Kubrynski
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
class OnBeanCondition extends FilteringContextCondition implements ConfigurationCondition, Ordered {

  @Override
  public ConfigurationPhase getConfigurationPhase() {
    return ConfigurationPhase.REGISTER_BEAN;
  }

  @Override
  public final ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    ConditionMessage matchMessage = ConditionMessage.empty();
    MergedAnnotations annotations = metadata.getAnnotations();
    if (annotations.isPresent(ConditionalOnBean.class)) {
      Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
      MatchResult matchResult = getMatchingBeans(context, spec);
      if (!matchResult.isAllMatched()) {
        String reason = createOnBeanNoMatchReason(matchResult);
        return ConditionOutcome.noMatch(spec.message().because(reason));
      }
      matchMessage = spec.message(matchMessage).found("bean", "beans").items(
              Style.QUOTE, matchResult.getNamesOfAllMatches());
    }
    if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
      Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
      MatchResult matchResult = getMatchingBeans(context, spec);
      if (!matchResult.isAllMatched()) {
        return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
      }
      else if (!hasSingleAutowireCandidate(
              context.getBeanFactory(), matchResult.getNamesOfAllMatches(), spec.getStrategy() == SearchStrategy.ALL)) {
        return ConditionOutcome.noMatch(
                spec.message().didNotFind("a primary bean from beans")
                        .items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
      }

      matchMessage = spec.message(matchMessage).found("a primary bean from beans")
              .items(Style.QUOTE, matchResult.getNamesOfAllMatches());
    }
    if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
      Spec<ConditionalOnMissingBean> spec = new Spec<>(
              context, metadata, annotations, ConditionalOnMissingBean.class);
      MatchResult matchResult = getMatchingBeans(context, spec);
      if (matchResult.isAnyMatched()) {
        String reason = createOnMissingBeanNoMatchReason(matchResult);
        return ConditionOutcome.noMatch(spec.message().because(reason));
      }
      matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
    }
    return ConditionOutcome.match(matchMessage);
  }

  protected final MatchResult getMatchingBeans(ConditionEvaluationContext context, Spec<?> spec) {
    ClassLoader classLoader = context.getClassLoader();
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
    Set<Class<?>> parameterizedContainers = spec.getParameterizedContainers();
    if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
      BeanFactory parent = beanFactory.getParentBeanFactory();
      Assert.isInstanceOf(ConfigurableBeanFactory.class, parent, "Unable to use SearchStrategy.ANCESTORS");
      beanFactory = (ConfigurableBeanFactory) parent;
    }
    MatchResult result = new MatchResult();
    Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(
            classLoader, beanFactory, considerHierarchy, spec.getIgnoredTypes(), parameterizedContainers);
    for (String type : spec.getTypes()) {
      Collection<String> typeMatches = getBeanNamesForType(
              classLoader, considerHierarchy, beanFactory, type, parameterizedContainers);
      typeMatches.removeIf(beansIgnoredByType::contains);
      if (typeMatches.isEmpty()) {
        result.recordUnmatchedType(type);
      }
      else {
        result.recordMatchedType(type, typeMatches);
      }
    }
    for (String annotation : spec.getAnnotations()) {
      Set<String> annotationMatches = getBeanNamesForAnnotation(
              classLoader, beanFactory, annotation, considerHierarchy);
      annotationMatches.removeAll(beansIgnoredByType);
      if (annotationMatches.isEmpty()) {
        result.recordUnmatchedAnnotation(annotation);
      }
      else {
        result.recordMatchedAnnotation(annotation, annotationMatches);
      }
    }
    for (String beanName : spec.getNames()) {
      if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
        result.recordMatchedName(beanName);
      }
      else {
        result.recordUnmatchedName(beanName);
      }
    }
    return result;
  }

  private Set<String> getNamesOfBeansIgnoredByType(
          ClassLoader classLoader, BeanFactory beanFactory,
          boolean considerHierarchy, Set<String> ignoredTypes, Set<Class<?>> parameterizedContainers) {
    Set<String> result = null;
    for (String ignoredType : ignoredTypes) {
      Collection<String> ignoredNames = getBeanNamesForType(
              classLoader, considerHierarchy, beanFactory, ignoredType, parameterizedContainers);
      result = addAll(result, ignoredNames);
    }
    return (result != null) ? result : Collections.emptySet();
  }

  private Set<String> getBeanNamesForType(
          ClassLoader classLoader, boolean considerHierarchy,
          BeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
    try {
      return getBeanNamesForType(
              beanFactory, considerHierarchy, resolve(type, classLoader), parameterizedContainers);
    }
    catch (ClassNotFoundException | NoClassDefFoundError ex) {
      return Collections.emptySet();
    }
  }

  private Set<String> getBeanNamesForType(
          BeanFactory beanFactory, boolean considerHierarchy, Class<?> type, Set<Class<?>> parameterizedContainers) {
    Set<String> result = collectBeanNamesForType(
            beanFactory, considerHierarchy, type, parameterizedContainers, null);
    return result != null ? result : Collections.emptySet();
  }

  private Set<String> collectBeanNamesForType(
          BeanFactory beanFactory, boolean considerHierarchy,
          Class<?> type, Set<Class<?>> parameterizedContainers, Set<String> result) {
    result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));
    for (Class<?> container : parameterizedContainers) {
      ResolvableType generic = ResolvableType.fromClassWithGenerics(container, type);
      result = addAll(result, beanFactory.getBeanNamesForType(generic, true, false));
    }
    if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
      BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
      if (parent != null) {
        result = collectBeanNamesForType(
                parent, considerHierarchy, type, parameterizedContainers, result);
      }
    }
    return result;
  }

  private Set<String> getBeanNamesForAnnotation(
          ClassLoader classLoader, ConfigurableBeanFactory beanFactory,
          String type, boolean considerHierarchy) throws LinkageError {
    Set<String> result = null;
    try {
      result = collectBeanNamesForAnnotation(
              beanFactory, resolveAnnotationType(classLoader, type), considerHierarchy, result);
    }
    catch (ClassNotFoundException ex) {
      // Continue
    }
    return (result != null) ? result : Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Annotation> resolveAnnotationType(
          ClassLoader classLoader, String type) throws ClassNotFoundException {
    return (Class<? extends Annotation>) resolve(type, classLoader);
  }

  private Set<String> collectBeanNamesForAnnotation(
          BeanFactory beanFactory, Class<? extends Annotation> annotationType, boolean considerHierarchy, Set<String> result) {
    result = addAll(result, beanFactory.getBeanNamesForAnnotation(annotationType));
    if (considerHierarchy) {
      BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
      if (parent != null) {
        result = collectBeanNamesForAnnotation(parent, annotationType, considerHierarchy, result);
      }
    }
    return result;
  }

  private boolean containsBean(ConfigurableBeanFactory beanFactory, String beanName,
                               boolean considerHierarchy) {
    if (considerHierarchy) {
      return beanFactory.containsBean(beanName);
    }
    return beanFactory.containsLocalBean(beanName);
  }

  private String createOnBeanNoMatchReason(MatchResult matchResult) {
    StringBuilder reason = new StringBuilder();
    appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
    appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
    appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
    return reason.toString();
  }

  private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
    if (!unmatched.isEmpty()) {
      if (reason.length() > 0) {
        reason.append(" and ");
      }
      reason.append("did not find any beans ");
      reason.append(description);
      reason.append(" ");
      reason.append(StringUtils.collectionToDelimitedString(unmatched, ", "));
    }
  }

  private String createOnMissingBeanNoMatchReason(MatchResult matchResult) {
    StringBuilder reason = new StringBuilder();
    appendMessageForMatches(reason, matchResult.getMatchedAnnotations(), "annotated with");
    appendMessageForMatches(reason, matchResult.getMatchedTypes(), "of type");
    if (!matchResult.getMatchedNames().isEmpty()) {
      if (reason.length() > 0) {
        reason.append(" and ");
      }
      reason.append("found beans named ");
      reason.append(StringUtils.collectionToDelimitedString(matchResult.getMatchedNames(), ", "));
    }
    return reason.toString();
  }

  private void appendMessageForMatches(
          StringBuilder reason, Map<String, Collection<String>> matches, String description) {
    if (!matches.isEmpty()) {
      for (Map.Entry<String, Collection<String>> entry : matches.entrySet()) {
        if (reason.length() > 0) {
          reason.append(" and ");
        }
        reason.append("found beans ");
        reason.append(description);
        reason.append(" '");
        reason.append(entry.getKey());
        reason.append("' ");
        reason.append(StringUtils.collectionToDelimitedString(entry.getValue(), ", "));
      }
    }
  }

  private boolean hasSingleAutowireCandidate(
          ConfigurableBeanFactory beanFactory, Set<String> beanNames, boolean considerHierarchy) {
    return (beanNames.size() == 1 || getPrimaryBeans(beanFactory, beanNames, considerHierarchy).size() == 1);
  }

  private List<String> getPrimaryBeans(
          ConfigurableBeanFactory beanFactory, Set<String> beanNames, boolean considerHierarchy) {
    ArrayList<String> primaryBeans = new ArrayList<>();
    for (String beanName : beanNames) {
      BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
      if (beanDefinition != null && beanDefinition.isPrimary()) {
        primaryBeans.add(beanName);
      }
    }
    return primaryBeans;
  }

  private BeanDefinition findBeanDefinition(
          ConfigurableBeanFactory beanFactory, String beanName, boolean considerHierarchy) {
    if (beanFactory.containsBeanDefinition(beanName)) {
      return beanFactory.getBeanDefinition(beanName);
    }
    if (considerHierarchy && beanFactory.getParentBeanFactory() instanceof ConfigurableBeanFactory configurable) {
      return findBeanDefinition(configurable, beanName, considerHierarchy);
    }
    return null;
  }

  private static Set<String> addAll(Set<String> result, Collection<String> additional) {
    if (CollectionUtils.isEmpty(additional)) {
      return result;
    }
    result = (result != null) ? result : new LinkedHashSet<>();
    result.addAll(additional);
    return result;
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  /**
   * A search specification extracted from the underlying annotation.
   */
  private static class Spec<A extends Annotation> {

    private final ClassLoader classLoader;
    private final Class<? extends Annotation> annotationType;

    private final Set<String> names;
    private final Set<String> types;
    private final Set<String> annotations;
    private final Set<String> ignoredTypes;
    @Nullable
    private final SearchStrategy strategy;
    private final Set<Class<?>> parameterizedContainers;

    Spec(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations, Class<A> annotationType) {
      MultiValueMap<String, Object> attributes = annotations.stream(annotationType)
              .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
              .collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
      this.annotationType = annotationType;
      this.classLoader = context.getClassLoader();
      this.names = extract(attributes, "name");
      this.annotations = extract(attributes, "annotation");
      this.ignoredTypes = extract(attributes, "ignored", "ignoredType");
      this.parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"));
      this.strategy = annotations.get(annotationType).getValue("search", SearchStrategy.class).orElse(null);
      Set<String> types = extractTypes(attributes);
      BeanTypeDeductionException deductionException = null;
      if (types.isEmpty() && this.names.isEmpty()) {
        try {
          types = deducedBeanType(context, metadata);
        }
        catch (BeanTypeDeductionException ex) {
          deductionException = ex;
        }
      }
      this.types = types;
      validate(deductionException);
    }

    protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
      return extract(attributes, "value", "type");
    }

    private Set<String> extract(MultiValueMap<String, Object> attributes, String... attributeNames) {
      if (attributes.isEmpty()) {
        return Collections.emptySet();
      }
      LinkedHashSet<String> result = new LinkedHashSet<>();
      for (String attributeName : attributeNames) {
        List<Object> values = attributes.getOrDefault(attributeName, Collections.emptyList());
        for (Object value : values) {
          if (value instanceof String[]) {
            merge(result, (String[]) value);
          }
          else if (value instanceof String) {
            merge(result, (String) value);
          }
        }
      }
      return result.isEmpty() ? Collections.emptySet() : result;
    }

    private void merge(Set<String> result, String... additional) {
      Collections.addAll(result, additional);
    }

    private Set<Class<?>> resolveWhenPossible(Set<String> classNames) {
      if (classNames.isEmpty()) {
        return Collections.emptySet();
      }
      LinkedHashSet<Class<?>> resolved = new LinkedHashSet<>(classNames.size());
      for (String className : classNames) {
        try {
          resolved.add(resolve(className, this.classLoader));
        }
        catch (ClassNotFoundException | NoClassDefFoundError ignored) { }
      }
      return resolved;
    }

    protected void validate(BeanTypeDeductionException ex) {
      if (!hasAtLeastOneElement(this.types, this.names, this.annotations)) {
        String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
        if (ex == null) {
          throw new IllegalStateException(message);
        }
        throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
      }
    }

    private boolean hasAtLeastOneElement(Set<?>... sets) {
      for (Set<?> set : sets) {
        if (!set.isEmpty()) {
          return true;
        }
      }
      return false;
    }

    protected final String getAnnotationName() {
      return "@" + ClassUtils.getShortName(this.annotationType);
    }

    private Set<String> deducedBeanType(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      if (metadata instanceof MethodMetadata && metadata.isAnnotated(Component.class.getName())) {
        return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
      }
      return Collections.emptySet();
    }

    private Set<String> deducedBeanTypeForBeanMethod(ConditionEvaluationContext context, MethodMetadata metadata) {
      try {
        Class<?> returnType = getReturnType(context, metadata);
        return Collections.singleton(returnType.getName());
      }
      catch (Throwable ex) {
        throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
      }
    }

    private Class<?> getReturnType(ConditionEvaluationContext context, MethodMetadata metadata)
            throws ClassNotFoundException, LinkageError {
      // Safe to load at this point since we are in the REGISTER_BEAN phase
      ClassLoader classLoader = context.getClassLoader();
      Class<?> returnType = resolve(metadata.getReturnTypeName(), classLoader);
      if (isParameterizedContainer(returnType)) {
        returnType = getReturnTypeGeneric(metadata, classLoader);
      }
      return returnType;
    }

    private boolean isParameterizedContainer(Class<?> type) {
      for (Class<?> parameterizedContainer : this.parameterizedContainers) {
        if (parameterizedContainer.isAssignableFrom(type)) {
          return true;
        }
      }
      return false;
    }

    private Class<?> getReturnTypeGeneric(MethodMetadata metadata, ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {
      Class<?> declaringClass = resolve(metadata.getDeclaringClassName(), classLoader);
      Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
      return ResolvableType.forReturnType(beanMethod).resolveGeneric();
    }

    private Method findBeanMethod(Class<?> declaringClass, String methodName) {
      Method method = ReflectionUtils.findMethod(declaringClass, methodName);
      if (isBeanMethod(method)) {
        return method;
      }
      Method[] candidates = ReflectionUtils.getAllDeclaredMethods(declaringClass);
      for (Method candidate : candidates) {
        if (candidate.getName().equals(methodName) && isBeanMethod(candidate)) {
          return candidate;
        }
      }
      throw new IllegalStateException("Unable to find bean method " + methodName);
    }

    private boolean isBeanMethod(Method method) {
      return method != null
              && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
              .isPresent(Component.class);
    }

    private SearchStrategy getStrategy() {
      return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
    }

    Set<String> getNames() {
      return this.names;
    }

    Set<String> getTypes() {
      return this.types;
    }

    Set<String> getAnnotations() {
      return this.annotations;
    }

    Set<String> getIgnoredTypes() {
      return this.ignoredTypes;
    }

    Set<Class<?>> getParameterizedContainers() {
      return this.parameterizedContainers;
    }

    ConditionMessage.Builder message() {
      return ConditionMessage.forCondition(this.annotationType, this);
    }

    ConditionMessage.Builder message(ConditionMessage message) {
      return message.andCondition(this.annotationType, this);
    }

    @Override
    public String toString() {
      boolean hasNames = !this.names.isEmpty();
      boolean hasTypes = !this.types.isEmpty();
      boolean hasIgnoredTypes = !this.ignoredTypes.isEmpty();
      StringBuilder string = new StringBuilder();
      string.append("(");
      if (hasNames) {
        string.append("names: ");
        string.append(StringUtils.collectionToCommaDelimitedString(this.names));
        string.append(hasTypes ? " " : "; ");
      }
      if (hasTypes) {
        string.append("types: ");
        string.append(StringUtils.collectionToCommaDelimitedString(this.types));
        string.append(hasIgnoredTypes ? " " : "; ");
      }
      if (hasIgnoredTypes) {
        string.append("ignored: ");
        string.append(StringUtils.collectionToCommaDelimitedString(this.ignoredTypes));
        string.append("; ");
      }

      if (strategy != null) {
        string.append("SearchStrategy: ");
        string.append(strategy.toString().toLowerCase(Locale.ENGLISH));
        string.append(")");
      }
      return string.toString();
    }

  }

  /**
   * Specialized {@link Spec specification} for
   * {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
   */
  private static class SingleCandidateSpec extends Spec<ConditionalOnSingleCandidate> {

    private static final Collection<String> FILTERED_TYPES = Arrays.asList("", Object.class.getName());

    SingleCandidateSpec(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
      super(context, metadata, annotations, ConditionalOnSingleCandidate.class);
    }

    @Override
    protected Set<String> extractTypes(MultiValueMap<String, Object> attributes) {
      Set<String> types = super.extractTypes(attributes);
      types.removeAll(FILTERED_TYPES);
      return types;
    }

    @Override
    protected void validate(BeanTypeDeductionException ex) {
      if (getTypes().size() != 1) {
        throw new IllegalArgumentException(
                getAnnotationName() + " annotations must specify only one type (got "
                        + StringUtils.collectionToCommaDelimitedString(getTypes()) + ")");
      }
    }

  }

  /**
   * Results collected during the condition evaluation.
   */
  private static final class MatchResult {

    private final HashSet<String> namesOfAllMatches = new HashSet<>();
    private final HashMap<String, Collection<String>> matchedTypes = new HashMap<>();
    private final HashMap<String, Collection<String>> matchedAnnotations = new HashMap<>();

    private final ArrayList<String> matchedNames = new ArrayList<>();
    private final ArrayList<String> unmatchedNames = new ArrayList<>();
    private final ArrayList<String> unmatchedTypes = new ArrayList<>();
    private final ArrayList<String> unmatchedAnnotations = new ArrayList<>();

    private void recordMatchedName(String name) {
      this.matchedNames.add(name);
      this.namesOfAllMatches.add(name);
    }

    private void recordUnmatchedName(String name) {
      this.unmatchedNames.add(name);
    }

    private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
      this.matchedAnnotations.put(annotation, matchingNames);
      this.namesOfAllMatches.addAll(matchingNames);
    }

    private void recordUnmatchedAnnotation(String annotation) {
      this.unmatchedAnnotations.add(annotation);
    }

    private void recordMatchedType(String type, Collection<String> matchingNames) {
      this.matchedTypes.put(type, matchingNames);
      this.namesOfAllMatches.addAll(matchingNames);
    }

    private void recordUnmatchedType(String type) {
      this.unmatchedTypes.add(type);
    }

    boolean isAllMatched() {
      return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty()
              && this.unmatchedTypes.isEmpty();
    }

    boolean isAnyMatched() {
      return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty())
              || (!this.matchedTypes.isEmpty());
    }

    Map<String, Collection<String>> getMatchedAnnotations() {
      return this.matchedAnnotations;
    }

    List<String> getMatchedNames() {
      return this.matchedNames;
    }

    Map<String, Collection<String>> getMatchedTypes() {
      return this.matchedTypes;
    }

    List<String> getUnmatchedAnnotations() {
      return this.unmatchedAnnotations;
    }

    List<String> getUnmatchedNames() {
      return this.unmatchedNames;
    }

    List<String> getUnmatchedTypes() {
      return this.unmatchedTypes;
    }

    Set<String> getNamesOfAllMatches() {
      return this.namesOfAllMatches;
    }

  }

  /**
   * Exception thrown when the bean type cannot be deduced.
   */
  static final class BeanTypeDeductionException extends RuntimeException {

    private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
      super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
    }

  }

}
