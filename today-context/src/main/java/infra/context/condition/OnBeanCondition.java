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

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import infra.aop.scope.ScopedProxyUtils;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.HierarchicalBeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.SingletonBeanRegistry;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.ConfigurationCondition;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.core.Ordered;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotation.Adapt;
import infra.core.annotation.MergedAnnotationCollectors;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.MethodMetadata;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Jakub Kubrynski
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
class OnBeanCondition extends FilteringInfraCondition implements ConfigurationCondition, Ordered {

  @Override
  public ConfigurationPhase getConfigurationPhase() {
    return ConfigurationPhase.REGISTER_BEAN;
  }

  @Override
  protected final ConditionOutcome[] getOutcomes(String[] configClasses, AutoConfigurationMetadata configMetadata) {
    ConditionOutcome[] outcomes = new ConditionOutcome[configClasses.length];
    for (int i = 0; i < outcomes.length; i++) {
      String autoConfigurationClass = configClasses[i];
      if (autoConfigurationClass != null) {
        Set<String> onBeanTypes = configMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
        ConditionOutcome outcome = getOutcome(onBeanTypes, ConditionalOnBean.class);
        if (outcome == null) {
          Set<String> onSingleCandidateTypes = configMetadata.getSet(autoConfigurationClass, "ConditionalOnSingleCandidate");
          outcome = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
        }
        outcomes[i] = outcome;
      }
    }
    return outcomes;
  }

  private ConditionOutcome getOutcome(Set<String> requiredBeanTypes, Class<? extends Annotation> annotation) {
    List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
    if (!missing.isEmpty()) {
      ConditionMessage message = ConditionMessage.forCondition(annotation)
              .didNotFind("required type", "required types")
              .items(ConditionMessage.Style.QUOTE, missing);
      return ConditionOutcome.noMatch(message);
    }
    return null;
  }

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ConditionOutcome matchOutcome = ConditionOutcome.match();
    MergedAnnotations annotations = metadata.getAnnotations();
    if (annotations.isPresent(ConditionalOnBean.class)) {
      var spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
      matchOutcome = evaluateConditionalOnBean(spec, matchOutcome.getConditionMessage());
      if (!matchOutcome.isMatch()) {
        return matchOutcome;
      }
    }
    if (metadata.isAnnotated(ConditionalOnSingleCandidate.class)) {
      var spec = new SingleCandidateSpec(context, metadata, metadata.getAnnotations());
      matchOutcome = evaluateConditionalOnSingleCandidate(spec, matchOutcome.getConditionMessage());
      if (!matchOutcome.isMatch()) {
        return matchOutcome;
      }
    }
    if (metadata.isAnnotated(ConditionalOnMissingBean.class)) {
      var spec = new Spec<>(context, metadata, annotations, ConditionalOnMissingBean.class);
      matchOutcome = evaluateConditionalOnMissingBean(spec, matchOutcome.getConditionMessage());
      if (!matchOutcome.isMatch()) {
        return matchOutcome;
      }
    }
    return matchOutcome;
  }

  private ConditionOutcome evaluateConditionalOnBean(Spec<ConditionalOnBean> spec, ConditionMessage matchMessage) {
    MatchResult matchResult = getMatchingBeans(spec);
    if (matchResult.isNoneMatch()) {
      String reason = createOnBeanNoMatchReason(matchResult);
      return ConditionOutcome.noMatch(spec.message().because(reason));
    }
    return ConditionOutcome.match(spec.message(matchMessage)
            .found("bean", "beans")
            .items(ConditionMessage.Style.QUOTE, matchResult.namesOfAllMatches));
  }

  private ConditionOutcome evaluateConditionalOnSingleCandidate(Spec<ConditionalOnSingleCandidate> spec, ConditionMessage matchMessage) {
    MatchResult matchResult = getMatchingBeans(spec);
    if (matchResult.isNoneMatch()) {
      return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
    }
    Set<String> allBeans = matchResult.namesOfAllMatches;
    if (allBeans.size() == 1) {
      return ConditionOutcome
              .match(spec.message(matchMessage).found("a single bean").items(ConditionMessage.Style.QUOTE, allBeans));
    }
    var beanDefinitions = getBeanDefinitions(spec.context.getBeanFactory(), allBeans, spec.getStrategy() == SearchStrategy.ALL);
    List<String> primaryBeans = getPrimaryBeans(beanDefinitions);
    if (primaryBeans.size() == 1) {
      return ConditionOutcome.match(spec.message(matchMessage)
              .found("a single primary bean '%s' from beans".formatted(primaryBeans.get(0)))
              .items(ConditionMessage.Style.QUOTE, allBeans));
    }
    if (primaryBeans.size() > 1) {
      return ConditionOutcome
              .noMatch(spec.message().found("multiple primary beans").items(ConditionMessage.Style.QUOTE, primaryBeans));
    }
    List<String> nonFallbackBeans = getNonFallbackBeans(beanDefinitions);
    if (nonFallbackBeans.size() == 1) {
      return ConditionOutcome.match(spec.message(matchMessage)
              .found("a single non-fallback bean '%s' from beans".formatted(nonFallbackBeans.get(0)))
              .items(ConditionMessage.Style.QUOTE, allBeans));
    }
    return ConditionOutcome.noMatch(spec.message().found("multiple beans").items(ConditionMessage.Style.QUOTE, allBeans));
  }

  private ConditionOutcome evaluateConditionalOnMissingBean(Spec<ConditionalOnMissingBean> spec, ConditionMessage matchMessage) {
    MatchResult matchResult = getMatchingBeans(spec);
    if (matchResult.isAnyMatched()) {
      String reason = createOnMissingBeanNoMatchReason(matchResult);
      return ConditionOutcome.noMatch(spec.message().because(reason));
    }
    return ConditionOutcome.match(spec.message(matchMessage).didNotFind("any beans").atAll());
  }

  protected final MatchResult getMatchingBeans(Spec<?> spec) {
    ClassLoader classLoader = spec.context.getClassLoader();
    ConfigurableBeanFactory beanFactory = spec.context.getBeanFactory();
    boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
    Set<Class<?>> parameterizedContainers = spec.parameterizedContainers;
    if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
      BeanFactory parent = beanFactory.getParentBeanFactory();
      Assert.isInstanceOf(ConfigurableBeanFactory.class, parent, "Unable to use SearchStrategy.ANCESTORS");
      beanFactory = (ConfigurableBeanFactory) parent;
    }
    MatchResult result = new MatchResult();
    Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(classLoader,
            beanFactory, considerHierarchy, spec.ignoredTypes, parameterizedContainers);
    for (String type : spec.types) {
      var typeMatchedDefinitions = getBeanDefinitionsForType(classLoader, considerHierarchy, beanFactory, type, parameterizedContainers);
      Set<String> typeMatchedNames = matchedNamesFrom(typeMatchedDefinitions,
              (name, definition) -> isCandidate(name, definition, beansIgnoredByType) && !ScopedProxyUtils.isScopedTarget(name));

      if (typeMatchedNames.isEmpty()) {
        result.recordUnmatchedType(type);
      }
      else {
        result.recordMatchedType(type, typeMatchedNames);
      }
    }
    for (String annotation : spec.annotations) {
      var annotationMatchedDefinitions = getBeanDefinitionsForAnnotation(classLoader, beanFactory, annotation, considerHierarchy);
      Set<String> annotationMatchedNames = matchedNamesFrom(annotationMatchedDefinitions,
              (name, definition) -> isCandidate(name, definition, beansIgnoredByType));
      if (annotationMatchedNames.isEmpty()) {
        result.recordUnmatchedAnnotation(annotation);
      }
      else {
        result.recordMatchedAnnotation(annotation, annotationMatchedNames);
      }
    }

    for (String beanName : spec.names) {
      if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
        result.recordMatchedName(beanName);
      }
      else {
        result.recordUnmatchedName(beanName);
      }
    }
    return result;
  }

  private Set<String> matchedNamesFrom(Map<String, BeanDefinition> namedDefinitions, BiPredicate<String, BeanDefinition> filter) {
    LinkedHashSet<String> matchedNames = new LinkedHashSet<>(namedDefinitions.size());
    for (Map.Entry<String, BeanDefinition> namedDefinition : namedDefinitions.entrySet()) {
      if (filter.test(namedDefinition.getKey(), namedDefinition.getValue())) {
        matchedNames.add(namedDefinition.getKey());
      }
    }
    return matchedNames;
  }

  private boolean isCandidate(String name, BeanDefinition definition, Set<String> ignoredBeans) {
    return (!ignoredBeans.contains(name))
            && (definition == null || (definition.isAutowireCandidate() && isDefaultCandidate(definition)));
  }

  private boolean isDefaultCandidate(BeanDefinition definition) {
    if (definition instanceof AbstractBeanDefinition abd) {
      return abd.isDefaultCandidate();
    }
    return true;
  }

  private Set<String> getNamesOfBeansIgnoredByType(@Nullable ClassLoader classLoader, BeanFactory beanFactory,
          boolean considerHierarchy, Set<String> ignoredTypes, Set<Class<?>> parameterizedContainers) {
    Set<String> result = null;
    for (String ignoredType : ignoredTypes) {
      Collection<String> ignoredNames = getBeanDefinitionsForType(classLoader, considerHierarchy, beanFactory,
              ignoredType, parameterizedContainers)
              .keySet();
      result = addAll(result, ignoredNames);
    }
    return result != null ? result : Collections.emptySet();
  }

  private Map<String, BeanDefinition> getBeanDefinitionsForType(@Nullable ClassLoader classLoader,
          boolean considerHierarchy, BeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
    try {
      return getBeanDefinitionsForType(beanFactory, considerHierarchy,
              resolve(type, classLoader), parameterizedContainers);
    }
    catch (ClassNotFoundException | NoClassDefFoundError ex) {
      return Collections.emptyMap();
    }
  }

  private Map<String, BeanDefinition> getBeanDefinitionsForType(BeanFactory beanFactory,
          boolean considerHierarchy, Class<?> type, Set<Class<?>> parameterizedContainers) {
    Map<String, BeanDefinition> result = collectBeanDefinitionsForType(beanFactory,
            considerHierarchy, type, parameterizedContainers, null);
    return result != null ? result : Collections.emptyMap();
  }

  private Map<String, BeanDefinition> collectBeanDefinitionsForType(BeanFactory beanFactory,
          boolean considerHierarchy, Class<?> type, Set<Class<?>> parameterizedContainers, Map<String, BeanDefinition> result) {

    result = putAll(result, beanFactory.getBeanNamesForType(type, true, false), beanFactory);
    for (Class<?> container : parameterizedContainers) {
      ResolvableType generic = ResolvableType.forClassWithGenerics(container, type);
      result = putAll(result, beanFactory.getBeanNamesForType(generic, true, false), beanFactory);
    }
    if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hbf) {
      BeanFactory parent = hbf.getParentBeanFactory();
      if (parent != null) {
        result = collectBeanDefinitionsForType(parent, considerHierarchy, type, parameterizedContainers, result);
      }
    }
    return result;
  }

  private Map<String, BeanDefinition> getBeanDefinitionsForAnnotation(ClassLoader classLoader,
          ConfigurableBeanFactory beanFactory, String type, boolean considerHierarchy) throws LinkageError {
    Map<String, BeanDefinition> result = null;
    try {
      result = collectBeanDefinitionsForAnnotation(beanFactory,
              resolveAnnotationType(classLoader, type), considerHierarchy, result);
    }
    catch (ClassNotFoundException ex) {
      // Continue
    }
    return result != null ? result : Collections.emptyMap();
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Annotation> resolveAnnotationType(ClassLoader classLoader, String type) throws ClassNotFoundException {
    return (Class<? extends Annotation>) resolve(type, classLoader);
  }

  private Map<String, BeanDefinition> collectBeanDefinitionsForAnnotation(BeanFactory beanFactory,
          Class<? extends Annotation> annotationType, boolean considerHierarchy, Map<String, BeanDefinition> result) {
    result = putAll(result, getBeanNamesForAnnotation(beanFactory, annotationType), beanFactory);
    if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hierarchical) {
      BeanFactory parent = hierarchical.getParentBeanFactory();
      if (parent != null) {
        result = collectBeanDefinitionsForAnnotation(parent, annotationType, considerHierarchy, result);
      }
    }
    return result;
  }

  private Set<String> getBeanNamesForAnnotation(BeanFactory beanFactory, Class<? extends Annotation> annotationType) {
    LinkedHashSet<String> foundBeanNames = new LinkedHashSet<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
      if (beanFactory instanceof ConfigurableBeanFactory cbf) {
        BeanDefinition beanDefinition = cbf.getBeanDefinition(beanName);
        if (beanDefinition != null && beanDefinition.isAbstract()) {
          continue;
        }
      }
      if (beanFactory.findAnnotationOnBean(beanName, annotationType, false).isPresent()) {
        foundBeanNames.add(beanName);
      }
    }
    if (beanFactory instanceof SingletonBeanRegistry singletonBeanRegistry) {
      for (String beanName : singletonBeanRegistry.getSingletonNames()) {
        if (beanFactory.findAnnotationOnBean(beanName, annotationType).isPresent()) {
          foundBeanNames.add(beanName);
        }
      }
    }
    return foundBeanNames;
  }

  private boolean containsBean(ConfigurableBeanFactory beanFactory, String beanName, boolean considerHierarchy) {
    if (considerHierarchy) {
      return beanFactory.containsBean(beanName);
    }
    return beanFactory.containsLocalBean(beanName);
  }

  private String createOnBeanNoMatchReason(MatchResult matchResult) {
    StringBuilder reason = new StringBuilder();
    appendMessageForNoMatches(reason, matchResult.unmatchedAnnotations, "annotated with");
    appendMessageForNoMatches(reason, matchResult.unmatchedTypes, "of type");
    appendMessageForNoMatches(reason, matchResult.unmatchedNames, "named");
    return reason.toString();
  }

  private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
    if (!unmatched.isEmpty()) {
      if (!reason.isEmpty()) {
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
    appendMessageForMatches(reason, matchResult.matchedAnnotations, "annotated with");
    appendMessageForMatches(reason, matchResult.matchedTypes, "of type");
    if (!matchResult.matchedNames.isEmpty()) {
      if (!reason.isEmpty()) {
        reason.append(" and ");
      }
      reason.append("found beans named ");
      reason.append(StringUtils.collectionToDelimitedString(matchResult.matchedNames, ", "));
    }
    return reason.toString();
  }

  private void appendMessageForMatches(StringBuilder reason, Map<String, Collection<String>> matches, String description) {
    if (!matches.isEmpty()) {
      for (Map.Entry<String, Collection<String>> entry : matches.entrySet()) {
        if (!reason.isEmpty()) {
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

  private Map<String, BeanDefinition> getBeanDefinitions(ConfigurableBeanFactory beanFactory, Set<String> beanNames, boolean considerHierarchy) {
    HashMap<String, BeanDefinition> definitions = new HashMap<>(beanNames.size());
    for (String beanName : beanNames) {
      BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
      definitions.put(beanName, beanDefinition);
    }
    return definitions;
  }

  private List<String> getPrimaryBeans(Map<String, BeanDefinition> beanDefinitions) {
    return getMatchingBeans(beanDefinitions, BeanDefinition::isPrimary);
  }

  private List<String> getNonFallbackBeans(Map<String, BeanDefinition> beanDefinitions) {
    return getMatchingBeans(beanDefinitions, Predicate.not(BeanDefinition::isFallback));
  }

  private List<String> getMatchingBeans(Map<String, BeanDefinition> beanDefinitions, Predicate<BeanDefinition> test) {
    ArrayList<String> matches = new ArrayList<>();
    for (Map.Entry<String, BeanDefinition> namedBeanDefinition : beanDefinitions.entrySet()) {
      if (test.test(namedBeanDefinition.getValue())) {
        matches.add(namedBeanDefinition.getKey());
      }
    }
    return matches;
  }

  @Nullable
  private BeanDefinition findBeanDefinition(ConfigurableBeanFactory beanFactory, String beanName, boolean considerHierarchy) {
    if (beanFactory.containsBeanDefinition(beanName)) {
      return beanFactory.getBeanDefinition(beanName);
    }
    if (considerHierarchy && beanFactory.getParentBeanFactory() instanceof ConfigurableBeanFactory configurable) {
      return findBeanDefinition(configurable, beanName, true);
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

  private static Map<String, BeanDefinition> putAll(Map<String, BeanDefinition> result, Collection<String> beanNames, BeanFactory beanFactory) {
    if (CollectionUtils.isEmpty(beanNames)) {
      return result;
    }
    if (result == null) {
      result = new LinkedHashMap<>();
    }
    for (String beanName : beanNames) {
      if (beanFactory instanceof ConfigurableBeanFactory clbf) {
        try {
          result.put(beanName, clbf.getBeanDefinition(beanName));
        }
        catch (NoSuchBeanDefinitionException ex) {
          result.put(beanName, null);
        }
      }
      else {
        result.put(beanName, null);
      }
    }
    return result;
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  /**
   * A search specification extracted from the underlying annotation.
   */
  static class Spec<A extends Annotation> {

    public final ConditionContext context;

    public final Class<? extends Annotation> annotationType;

    public final Set<String> names;
    public final Set<String> types;
    public final Set<String> annotations;
    public final Set<String> ignoredTypes;

    @Nullable
    private final SearchStrategy strategy;

    public final Set<Class<?>> parameterizedContainers;

    Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations, Class<A> annotationType) {
      MultiValueMap<String, Object> attributes = annotations.stream(annotationType)
              .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
              .collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
      this.annotationType = annotationType;
      this.context = context;
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
      var result = new LinkedHashSet<String>();
      for (String attributeName : attributeNames) {
        List<Object> values = attributes.getOrDefault(attributeName, Collections.emptyList());
        for (Object value : values) {
          if (value instanceof String[] strings) {
            merge(result, strings);
          }
          else if (value instanceof String str) {
            merge(result, str);
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
      ClassLoader classLoader = context.getClassLoader();
      var resolved = new LinkedHashSet<Class<?>>(classNames.size());
      for (String className : classNames) {
        try {
          resolved.add(resolve(className, classLoader));
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

    private Set<String> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
      if (metadata instanceof MethodMetadata && metadata.isAnnotated(Component.class.getName())) {
        return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
      }
      return Collections.emptySet();
    }

    private Set<String> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
      try {
        Class<?> returnType = getReturnType(context, metadata);
        return Collections.singleton(returnType.getName());
      }
      catch (Throwable ex) {
        throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
      }
    }

    private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata)
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

    private boolean isBeanMethod(@Nullable Method method) {
      return method != null
              && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).isPresent(Component.class);
    }

    private SearchStrategy getStrategy() {
      return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
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
        string.append(strategy.toString().toLowerCase(Locale.ROOT));
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

    SingleCandidateSpec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
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
      if (types.size() != 1) {
        throw new IllegalArgumentException("%s annotations must specify only one type (got %s)"
                .formatted(getAnnotationName(), StringUtils.collectionToCommaDelimitedString(types)));
      }
    }

  }

  /**
   * Results collected during the condition evaluation.
   */
  static final class MatchResult {

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

    boolean isNoneMatch() {
      return !this.unmatchedAnnotations.isEmpty()
              || !this.unmatchedNames.isEmpty()
              || !this.unmatchedTypes.isEmpty();
    }

    boolean isAnyMatched() {
      return (!this.matchedAnnotations.isEmpty())
              || (!this.matchedNames.isEmpty())
              || (!this.matchedTypes.isEmpty());
    }

  }

  /**
   * Exception thrown when the bean type cannot be deduced.
   */
  static final class BeanTypeDeductionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
      super("Failed to deduce bean type for %s.%s".formatted(className, beanMethodName), cause);
    }

  }

}
