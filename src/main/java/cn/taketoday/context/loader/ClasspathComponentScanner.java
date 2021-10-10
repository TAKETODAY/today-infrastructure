/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.loader;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.context.Conditional;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Nullable;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

import static cn.taketoday.core.Constant.VALUE;
import static cn.taketoday.core.annotation.AnnotationUtils.getAttributesArray;

/**
 * @author TODAY 2021/10/2 23:38
 * @since 4.0
 */
public class ClasspathComponentScanner {
  private static final Logger log = LoggerFactory.getLogger(ClasspathComponentScanner.class);

  /** @since 2.1.7 Scan candidates */
  private CandidateComponentScanner componentScanner = CandidateComponentScanner.getSharedInstance();
  private final BeanDefinitionRegistry registry;

  public ClasspathComponentScanner(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  protected CandidateComponentScanner createCandidateComponentScanner() {
    return CandidateComponentScanner.getSharedInstance();
  }

  protected Set<Class<?>> getComponentCandidates(String[] locations) {
    CandidateComponentScanner scanner = getCandidateComponentScanner();
    if (ObjectUtils.isEmpty(locations)) {
      // Candidates have not been set or scanned
      if (scanner.getCandidates() == null) {
        return scanner.scan();// scan all class path
      }
      return scanner.getScanningCandidates();
    }
    return scanner.scan(locations);
  }

  private CandidateComponentScanner getCandidateComponentScanner() {
    return componentScanner;
  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * <p>
   * {@link CandidateComponentScanner} will scan the classes from given package
   * locations. And register the {@link BeanDefinition}s using
   * loadBeanDefinition(Class)
   *
   * @param locations
   *         package locations
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  public void scan(String... locations) throws BeanDefinitionStoreException {
    // Loading candidates components
    log.info("Loading candidates components");
    Set<Class<?>> candidates = getComponentCandidates(locations);
    log.info("There are [{}] candidates components in [{}]", candidates.size(), this);

  }

  /**
   * Load bean definition with given bean class.
   * <p>
   * The candidate bean class can't be abstract and must pass the condition which
   * {@link Conditional} is annotated.
   *
   * @param candidate
   *         Candidate bean class the class will be load
   *
   * @return returns a new BeanDefinition if {@link #transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #register(Class)
   */
  @Nullable
  public List<BeanDefinition> load(Class<?> candidate) {
    // don't load abstract class
    if (canRegister(candidate)) {
      return register(candidate);
    }
    return null;
  }

  /**
   * Load bean definitions with given bean collection.
   *
   * @param candidates
   *         candidates beans collection
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  public void load(Collection<Class<?>> candidates) {
    for (Class<?> candidate : candidates) {
      // don't load abstract class
      if (canRegister(candidate)) {
        doRegister(candidate, null);
      }
    }
  }

  private boolean canRegister(Class<?> candidate) {
    return !Modifier.isAbstract(candidate.getModifiers())
            && conditionEvaluator.passCondition(candidate);
  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * <p>
   * {@link CandidateComponentScanner} will scan the classes from given package
   * locations. And register the {@link BeanDefinition}s using
   * loadBeanDefinition(Class)
   *
   * @param locations
   *         package locations
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #load(Class)
   * @since 4.0
   */
  public void load(String... locations) throws BeanDefinitionStoreException {
    load(new CandidateComponentScanner().scan(locations));
  }

  /**
   * Load bean definition with given bean class and bean name.
   * <p>
   * If the provided bean class annotated {@link Component} annotation will
   * register beans with given {@link Component} metadata.
   * <p>
   * Otherwise register a bean will given default metadata: use the default bean
   * name creator create the default bean name, use default bean scope
   * {@link Scope#SINGLETON} , empty initialize method ,empty property value and
   * empty destroy method.
   *
   * @param name
   *         Bean name
   * @param beanClass
   *         Bean class
   *
   * @return returns a new BeanDefinition if {@link #transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  public List<BeanDefinition> load(String name, Class<?> beanClass) {
    return Collections.singletonList(getRegistered(name, beanClass, null));
  }

  /**
   * Load bean definition with given bean class and bean name.
   * <p>
   * If the provided bean class annotated {@link Component} annotation will
   * register beans with given {@link Component} metadata.
   * <p>
   * Otherwise register a bean will given default metadata: use the default bean
   * name creator create the default bean name, use default bean scope
   * {@link Scope#SINGLETON} , empty initialize method ,empty property value and
   * empty destroy method.
   *
   * @param name
   *         default bean name
   * @param beanClass
   *         Bean class
   * @param ignoreAnnotation
   *         ignore {@link Component} scanning
   *
   * @return returns a new BeanDefinition if {@link #transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  public List<BeanDefinition> load(String name, Class<?> beanClass, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException {
    if (ignoreAnnotation) {
      return Collections.singletonList(getRegistered(name, beanClass, null));
    }
    AnnotationAttributes[] annotationAttributes = getAttributesArray(beanClass, Component.class);
    if (ObjectUtils.isEmpty(annotationAttributes)) {
      return Collections.singletonList(getRegistered(name, beanClass, null));
    }
    ArrayList<BeanDefinition> definitions = new ArrayList<>();
    for (AnnotationAttributes attributes : annotationAttributes) {
      doRegister(beanClass, name, attributes, definitions::add);
    }
    return definitions;
  }

  @Nullable
  private BeanDefinition getRegistered(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    BeanDefinition newDef = BeanDefinitionBuilder.defaults(name, beanClass, attributes);
    return register(name, newDef);
  }

  private BeanDefinition register(String name, BeanDefinition newDef) {
    return null;
  }

  public List<BeanDefinition> register(Class<?> candidate) {
    ArrayList<BeanDefinition> defs = new ArrayList<>();
    doRegister(candidate, defs::add);
    return defs;
  }

  private void doRegister(Class<?> candidate, Consumer<BeanDefinition> registeredConsumer) {
    AnnotationAttributes[] annotationAttributes = getAttributesArray(candidate, Component.class);
    if (ObjectUtils.isNotEmpty(annotationAttributes)) {
      String defaultBeanName = createBeanName(candidate);
      for (AnnotationAttributes attributes : annotationAttributes) {
        doRegister(candidate, defaultBeanName, attributes, registeredConsumer);
      }
    }
  }

  private void doRegister(
          Class<?> candidate, String defaultBeanName,
          AnnotationAttributes attributes, Consumer<BeanDefinition> registeredConsumer) {
    for (String name : BeanDefinitionBuilder.determineName(
            defaultBeanName, attributes.getStringArray(VALUE))) {
      BeanDefinition registered = getRegistered(name, candidate, attributes);
      if (registered != null && registeredConsumer != null) { // none null BeanDefinition
        registeredConsumer.accept(registered);
      }
    }
  }

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type
   *         type
   *
   * @return bean name
   *
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return ClassUtils.getShortName(type);
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }
}
