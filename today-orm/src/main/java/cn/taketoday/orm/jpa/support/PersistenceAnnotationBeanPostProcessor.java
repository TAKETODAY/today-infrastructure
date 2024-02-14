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

package cn.taketoday.orm.jpa.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.InjectionMetadata;
import cn.taketoday.beans.factory.annotation.InjectionMetadata.InjectedElement;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.config.NamedBeanHolder;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.jndi.JndiLocatorDelegate;
import cn.taketoday.jndi.JndiTemplate;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;
import cn.taketoday.orm.jpa.EntityManagerFactoryUtils;
import cn.taketoday.orm.jpa.EntityManagerProxy;
import cn.taketoday.orm.jpa.ExtendedEntityManagerCreator;
import cn.taketoday.orm.jpa.SharedEntityManagerCreator;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.SynchronizationType;

/**
 * BeanPostProcessor that processes {@link PersistenceUnit}
 * and {@link PersistenceContext} annotations, for injection of
 * the corresponding JPA resources {@link EntityManagerFactory}
 * and {@link EntityManager}. Any such annotated fields or methods
 * in any Framework-managed object will automatically be injected.
 *
 * <p>This post-processor will inject sub-interfaces of {@code EntityManagerFactory}
 * and {@code EntityManager} if the annotated fields or methods are declared as such.
 * The actual type will be verified early, with the exception of a shared ("transactional")
 * {@code EntityManager} reference, where type mismatches might be detected as late
 * as on the first actual invocation.
 *
 * <p>Note: In the present implementation, PersistenceAnnotationBeanPostProcessor
 * only supports {@code @PersistenceUnit} and {@code @PersistenceContext}
 * with the "unitName" attribute, or no attribute at all (for the default unit).
 * If those annotations are present with the "name" attribute at the class level,
 * they will simply be ignored, since those only serve as deployment hint
 * (as per the Jakarta EE specification).
 *
 * <p>This post-processor can either obtain EntityManagerFactory beans defined
 * in the Framework application context (the default), or obtain EntityManagerFactory
 * references from JNDI ("persistence unit references"). In the bean case,
 * the persistence unit name will be matched against the actual deployed unit,
 * with the bean name used as fallback unit name if no deployed name found.
 * Typically, Framework's {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * will be used for setting up such EntityManagerFactory beans. Alternatively,
 * such beans may also be obtained from JNDI, e.g. using the {@code jee:jndi-lookup}
 * XML configuration element (with the bean name matching the requested unit name).
 * In both cases, the post-processor definition will look as simple as this:
 *
 * <pre class="code">
 * &lt;bean class="cn.taketoday.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"/&gt;</pre>
 *
 * In the JNDI case, specify the corresponding JNDI names in this post-processor's
 * {@link #setPersistenceUnits "persistenceUnits" map}, typically with matching
 * {@code persistence-unit-ref} entries in the Jakarta EE deployment descriptor.
 * By default, those names are considered as resource references (according to the
 * Jakarta EE resource-ref convention), located underneath the "java:comp/env/" namespace.
 * For example:
 *
 * <pre class="code">
 * &lt;bean class="cn.taketoday.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceUnits"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/unit1"/&gt;
 *       &lt;entry key="unit2" value="persistence/unit2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * In this case, the specified persistence units will always be resolved in JNDI
 * rather than as Framework-defined beans. The entire persistence unit deployment,
 * including the weaving of persistent classes, is then up to the Jakarta EE server.
 * Persistence contexts (i.e. EntityManager references) will be built based on
 * those server-provided EntityManagerFactory references, using Framework's own
 * transaction synchronization facilities for transactional EntityManager handling
 * (typically with Framework's {@code @Transactional} annotation for demarcation
 * and {@link cn.taketoday.transaction.jta.JtaTransactionManager} as backend).
 *
 * <p>If you prefer the Jakarta EE server's own EntityManager handling, specify entries
 * in this post-processor's {@link #setPersistenceContexts "persistenceContexts" map}
 * (or {@link #setExtendedPersistenceContexts "extendedPersistenceContexts" map},
 * typically with matching {@code persistence-context-ref} entries in the
 * Jakarta EE deployment descriptor. For example:
 *
 * <pre class="code">
 * &lt;bean class="cn.taketoday.orm.jpa.support.PersistenceAnnotationBeanPostProcessor"&gt;
 *   &lt;property name="persistenceContexts"&gt;
 *     &lt;map/gt;
 *       &lt;entry key="unit1" value="persistence/context1"/&gt;
 *       &lt;entry key="unit2" value="persistence/context2"/&gt;
 *     &lt;/map/gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * If the application only obtains EntityManager references in the first place,
 * this is all you need to specify. If you need EntityManagerFactory references
 * as well, specify entries for both "persistenceUnits" and "persistenceContexts",
 * pointing to matching JNDI locations.
 *
 * <p><b>NOTE: In general, do not inject EXTENDED EntityManagers into STATELESS beans,
 * i.e. do not use {@code @PersistenceContext} with type {@code EXTENDED} in
 * Framework beans defined with scope 'singleton' (Framework's default scope).</b>
 * Extended EntityManagers are <i>not</i> thread-safe, hence they must not be used
 * in concurrently accessed beans (which Framework-managed singletons usually are).
 *
 * <p>Note: A default PersistenceAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom PersistenceAnnotationBeanPostProcessor bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PersistenceUnit
 * @see PersistenceContext
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PersistenceAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor,
        DestructionAwareBeanPostProcessor, DependenciesBeanPostProcessor, BeanRegistrationAotProcessor,
        MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, Serializable {

  @Nullable
  private Object jndiEnvironment;

  private boolean resourceRef = true;

  @Nullable
  private transient Map<String, String> persistenceUnits;

  @Nullable
  private transient Map<String, String> persistenceContexts;

  @Nullable
  private transient Map<String, String> extendedPersistenceContexts;

  private transient String defaultPersistenceUnitName = "";

  private int order = Ordered.LOWEST_PRECEDENCE - 4;

  @Nullable
  private transient BeanFactory beanFactory;

  private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

  private final Map<Object, EntityManager> extendedEntityManagersToClose = new ConcurrentHashMap<>(16);

  /**
   * Set the JNDI template to use for JNDI lookups.
   *
   * @see cn.taketoday.jndi.JndiAccessor#setJndiTemplate
   */
  public void setJndiTemplate(Object jndiTemplate) {
    this.jndiEnvironment = jndiTemplate;
  }

  /**
   * Set the JNDI environment to use for JNDI lookups.
   *
   * @see cn.taketoday.jndi.JndiAccessor#setJndiEnvironment
   */
  public void setJndiEnvironment(Properties jndiEnvironment) {
    this.jndiEnvironment = jndiEnvironment;
  }

  /**
   * Set whether the lookup occurs in a Jakarta EE container, i.e. if the prefix
   * "java:comp/env/" needs to be added if the JNDI name doesn't already
   * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
   *
   * @see cn.taketoday.jndi.JndiLocatorSupport#setResourceRef
   */
  public void setResourceRef(boolean resourceRef) {
    this.resourceRef = resourceRef;
  }

  /**
   * Specify the persistence units for EntityManagerFactory lookups,
   * as a Map from persistence unit name to persistence unit JNDI name
   * (which needs to resolve to an EntityManagerFactory instance).
   * <p>JNDI names specified here should refer to {@code persistence-unit-ref}
   * entries in the Jakarta EE deployment descriptor, matching the target persistence unit.
   * <p>In case of no unit name specified in the annotation, the specified value
   * for the {@link #setDefaultPersistenceUnitName default persistence unit}
   * will be taken (by default, the value mapped to the empty String),
   * or simply the single persistence unit if there is only one.
   * <p>This is mainly intended for use in a Jakarta EE environment, with all lookup
   * driven by the standard JPA annotations, and all EntityManagerFactory
   * references obtained from JNDI. No separate EntityManagerFactory bean
   * definitions are necessary in such a scenario.
   * <p>If no corresponding "persistenceContexts"/"extendedPersistenceContexts"
   * are specified, {@code @PersistenceContext} will be resolved to
   * EntityManagers built on top of the EntityManagerFactory defined here.
   * Note that those will be Infra-managed EntityManagers, which implement
   * transaction synchronization based on Infra facilities.
   * If you prefer the Jakarta EE server's own EntityManager handling,
   * specify corresponding "persistenceContexts"/"extendedPersistenceContexts".
   */
  public void setPersistenceUnits(Map<String, String> persistenceUnits) {
    this.persistenceUnits = persistenceUnits;
  }

  /**
   * Specify the <i>transactional</i> persistence contexts for EntityManager lookups,
   * as a Map from persistence unit name to persistence context JNDI name
   * (which needs to resolve to an EntityManager instance).
   * <p>JNDI names specified here should refer to {@code persistence-context-ref}
   * entries in the Jakarta EE deployment descriptors, matching the target persistence unit
   * and being set up with persistence context type {@code Transaction}.
   * <p>In case of no unit name specified in the annotation, the specified value
   * for the {@link #setDefaultPersistenceUnitName default persistence unit}
   * will be taken (by default, the value mapped to the empty String),
   * or simply the single persistence unit if there is only one.
   * <p>This is mainly intended for use in a Jakarta EE environment, with all
   * lookup driven by the standard JPA annotations, and all EntityManager
   * references obtained from JNDI. No separate EntityManagerFactory bean
   * definitions are necessary in such a scenario, and all EntityManager
   * handling is done by the Jakarta EE server itself.
   */
  public void setPersistenceContexts(Map<String, String> persistenceContexts) {
    this.persistenceContexts = persistenceContexts;
  }

  /**
   * Specify the <i>extended</i> persistence contexts for EntityManager lookups,
   * as a Map from persistence unit name to persistence context JNDI name
   * (which needs to resolve to an EntityManager instance).
   * <p>JNDI names specified here should refer to {@code persistence-context-ref}
   * entries in the Jakarta EE deployment descriptors, matching the target persistence unit
   * and being set up with persistence context type {@code Extended}.
   * <p>In case of no unit name specified in the annotation, the specified value
   * for the {@link #setDefaultPersistenceUnitName default persistence unit}
   * will be taken (by default, the value mapped to the empty String),
   * or simply the single persistence unit if there is only one.
   * <p>This is mainly intended for use in a Jakarta EE environment, with all
   * lookup driven by the standard JPA annotations, and all EntityManager
   * references obtained from JNDI. No separate EntityManagerFactory bean
   * definitions are necessary in such a scenario, and all EntityManager
   * handling is done by the Jakarta EE server itself.
   */
  public void setExtendedPersistenceContexts(Map<String, String> extendedPersistenceContexts) {
    this.extendedPersistenceContexts = extendedPersistenceContexts;
  }

  /**
   * Specify the default persistence unit name, to be used in case
   * of no unit name specified in an {@code @PersistenceUnit} /
   * {@code @PersistenceContext} annotation.
   * <p>This is mainly intended for lookups in the application context,
   * indicating the target persistence unit name (typically matching
   * the bean name), but also applies to lookups in the
   * {@link #setPersistenceUnits "persistenceUnits"} /
   * {@link #setPersistenceContexts "persistenceContexts"} /
   * {@link #setExtendedPersistenceContexts "extendedPersistenceContexts"} map,
   * avoiding the need for duplicated mappings for the empty String there.
   * <p>Default is to check for a single EntityManagerFactory bean
   * in the Infra application context, if any. If there are multiple
   * such factories, either specify this default persistence unit name
   * or explicitly refer to named persistence units in your annotations.
   */
  public void setDefaultPersistenceUnitName(@Nullable String unitName) {
    this.defaultPersistenceUnitName = (unitName != null ? unitName : "");
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    findInjectionMetadata(beanDefinition, beanType, beanName);
  }

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    String beanName = registeredBean.getBeanName();
    RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
    InjectionMetadata metadata = findInjectionMetadata(beanDefinition, beanClass, beanName);
    Collection<InjectedElement> injectedElements = metadata.getInjectedElements(
            beanDefinition.getPropertyValues());
    if (CollectionUtils.isNotEmpty(injectedElements)) {
      return new AotContribution(beanClass, injectedElements);
    }
    return null;
  }

  private InjectionMetadata findInjectionMetadata(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    InjectionMetadata metadata = findPersistenceMetadata(beanName, beanType, null);
    metadata.checkConfigMembers(beanDefinition);
    return metadata;
  }

  @Override
  public void resetBeanDefinition(String beanName) {
    this.injectionMetadataCache.remove(beanName);
  }

  @Override
  public PropertyValues processDependencies(@Nullable PropertyValues pvs, Object bean, String beanName) {
    InjectionMetadata metadata = findPersistenceMetadata(beanName, bean.getClass(), pvs);
    try {
      metadata.inject(bean, beanName, pvs);
      return pvs;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(beanName, "Injection of persistence dependencies failed", ex);
    }
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) {
    EntityManager emToClose = this.extendedEntityManagersToClose.remove(bean);
    EntityManagerFactoryUtils.closeEntityManager(emToClose);
  }

  @Override
  public boolean requiresDestruction(Object bean) {
    return this.extendedEntityManagersToClose.containsKey(bean);
  }

  private InjectionMetadata findPersistenceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
    // Fall back to class name as cache key, for backwards compatibility with custom callers.
    String cacheKey = StringUtils.isNotEmpty(beanName) ? beanName : clazz.getName();
    // Quick check on the concurrent map first, with minimal locking.
    InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
      synchronized(this.injectionMetadataCache) {
        metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
          if (metadata != null) {
            metadata.clear(pvs);
          }
          metadata = buildPersistenceMetadata(clazz);
          this.injectionMetadataCache.put(cacheKey, metadata);
        }
      }
    }
    return metadata;
  }

  private InjectionMetadata buildPersistenceMetadata(Class<?> clazz) {
    if (!AnnotationUtils.isCandidateClass(clazz, PersistenceContext.class, PersistenceUnit.class)) {
      return InjectionMetadata.EMPTY;
    }

    List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      ArrayList<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

      ReflectionUtils.doWithLocalFields(targetClass, field -> {
        if (field.isAnnotationPresent(PersistenceContext.class)
                || field.isAnnotationPresent(PersistenceUnit.class)) {
          if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Persistence annotations are not supported on static fields");
          }
          currElements.add(new PersistenceElement(field, field, null));
        }
      });

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
          return;
        }
        if ((bridgedMethod.isAnnotationPresent(PersistenceContext.class)
                || bridgedMethod.isAnnotationPresent(PersistenceUnit.class))
                && method.equals(ReflectionUtils.getMostSpecificMethod(method, clazz))) {
          if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("Persistence annotations are not supported on static methods");
          }
          if (method.getParameterCount() != 1) {
            throw new IllegalStateException("Persistence annotation requires a single-arg method: " + method);
          }
          PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
          currElements.add(new PersistenceElement(method, bridgedMethod, pd));
        }
      });

      elements.addAll(0, currElements);
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);

    return InjectionMetadata.forElements(elements, clazz);
  }

  /**
   * Return a specified persistence unit for the given unit name,
   * as defined through the "persistenceUnits" map.
   *
   * @param unitName the name of the persistence unit
   * @return the corresponding EntityManagerFactory,
   * or {@code null} if none found
   * @see #setPersistenceUnits
   */
  @Nullable
  protected EntityManagerFactory getPersistenceUnit(@Nullable String unitName) {
    if (this.persistenceUnits != null) {
      String unitNameForLookup = unitName != null ? unitName : "";
      if (unitNameForLookup.isEmpty()) {
        unitNameForLookup = this.defaultPersistenceUnitName;
      }
      String jndiName = persistenceUnits.get(unitNameForLookup);
      if (jndiName == null && unitNameForLookup.isEmpty() && persistenceUnits.size() == 1) {
        jndiName = persistenceUnits.values().iterator().next();
      }
      if (jndiName != null) {
        try {
          return lookup(jndiName, EntityManagerFactory.class);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Could not obtain EntityManagerFactory [" + jndiName + "] from JNDI", ex);
        }
      }
    }
    return null;
  }

  /**
   * Return a specified persistence context for the given unit name, as defined
   * through the "persistenceContexts" (or "extendedPersistenceContexts") map.
   *
   * @param unitName the name of the persistence unit
   * @param extended whether to obtain an extended persistence context
   * @return the corresponding EntityManager, or {@code null} if none found
   * @see #setPersistenceContexts
   * @see #setExtendedPersistenceContexts
   */
  @Nullable
  protected EntityManager getPersistenceContext(@Nullable String unitName, boolean extended) {
    Map<String, String> contexts = (extended ? extendedPersistenceContexts : persistenceContexts);
    if (contexts != null) {
      String unitNameForLookup = (unitName != null ? unitName : "");
      if (unitNameForLookup.isEmpty()) {
        unitNameForLookup = defaultPersistenceUnitName;
      }
      String jndiName = contexts.get(unitNameForLookup);
      if (jndiName == null && unitNameForLookup.isEmpty() && contexts.size() == 1) {
        jndiName = contexts.values().iterator().next();
      }
      if (jndiName != null) {
        try {
          return lookup(jndiName, EntityManager.class);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Could not obtain EntityManager [" + jndiName + "] from JNDI", ex);
        }
      }
    }
    return null;
  }

  /**
   * Find an EntityManagerFactory with the given name in the current Infra
   * application context, falling back to a single default EntityManagerFactory
   * (if any) in case of no unit name specified.
   *
   * @param unitName the name of the persistence unit (may be {@code null} or empty)
   * @param requestingBeanName the name of the requesting bean
   * @return the EntityManagerFactory
   * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
   */
  protected EntityManagerFactory findEntityManagerFactory(@Nullable String unitName, @Nullable String requestingBeanName)
          throws NoSuchBeanDefinitionException {

    String unitNameForLookup = (unitName != null ? unitName : "");
    if (unitNameForLookup.isEmpty()) {
      unitNameForLookup = defaultPersistenceUnitName;
    }
    if (!unitNameForLookup.isEmpty()) {
      return findNamedEntityManagerFactory(unitNameForLookup, requestingBeanName);
    }
    else {
      return findDefaultEntityManagerFactory(requestingBeanName);
    }
  }

  /**
   * Find an EntityManagerFactory with the given name in the current
   * Infra application context.
   *
   * @param unitName the name of the persistence unit (never empty)
   * @param requestingBeanName the name of the requesting bean
   * @return the EntityManagerFactory
   * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
   */
  protected EntityManagerFactory findNamedEntityManagerFactory(String unitName, @Nullable String requestingBeanName)
          throws NoSuchBeanDefinitionException {
    Assert.state(beanFactory != null, "BeanFactory required for EntityManagerFactory bean lookup");
    EntityManagerFactory emf = EntityManagerFactoryUtils.findEntityManagerFactory(beanFactory, unitName);
    if (requestingBeanName != null && beanFactory instanceof ConfigurableBeanFactory cbf) {
      cbf.registerDependentBean(unitName, requestingBeanName);
    }
    return emf;
  }

  /**
   * Find a single default EntityManagerFactory in the Infra application context.
   *
   * @return the default EntityManagerFactory
   * @throws NoSuchBeanDefinitionException if there is no single EntityManagerFactory in the context
   */
  protected EntityManagerFactory findDefaultEntityManagerFactory(@Nullable String requestingBeanName)
          throws NoSuchBeanDefinitionException {
    Assert.state(beanFactory != null, "BeanFactory required for EntityManagerFactory bean lookup");
    if (beanFactory instanceof ConfigurableBeanFactory clbf) {
      // Fancy variant with dependency registration
      NamedBeanHolder<EntityManagerFactory> emfHolder = clbf.resolveNamedBean(EntityManagerFactory.class);
      if (requestingBeanName != null) {
        clbf.registerDependentBean(emfHolder.getBeanName(), requestingBeanName);
      }
      return emfHolder.getBeanInstance();
    }
    else {
      // Plain variant: just find a default bean
      return beanFactory.getBean(EntityManagerFactory.class);
    }
  }

  /**
   * Perform a JNDI lookup for the given resource by name.
   * <p>Called for EntityManagerFactory and EntityManager lookup
   * when JNDI names are mapped for specific persistence units.
   *
   * @param jndiName the JNDI name to look up
   * @param requiredType the required type of the object
   * @return the obtained object
   * @throws Exception if the JNDI lookup failed
   */
  protected <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
    return new LocatorDelegate().lookup(jndiName, requiredType);
  }

  /**
   * Separate inner class to isolate the JNDI API dependency
   * (for compatibility with Google App Engine's API white list).
   */
  private class LocatorDelegate {

    public <T> T lookup(String jndiName, Class<T> requiredType) throws Exception {
      JndiLocatorDelegate locator = new JndiLocatorDelegate();
      if (jndiEnvironment instanceof JndiTemplate jndiTemplate) {
        locator.setJndiTemplate(jndiTemplate);
      }
      else if (jndiEnvironment instanceof Properties properties) {
        locator.setJndiEnvironment(properties);
      }
      else if (jndiEnvironment != null) {
        throw new IllegalStateException("Illegal 'jndiEnvironment' type: " + jndiEnvironment.getClass());
      }
      locator.setResourceRef(resourceRef);
      return locator.lookup(jndiName, requiredType);
    }
  }

  /**
   * Class representing injection information about an annotated field
   * or setter method.
   */
  private class PersistenceElement extends InjectionMetadata.InjectedElement {

    private final String unitName;

    @Nullable
    private PersistenceContextType type;

    private boolean synchronizedWithTransaction = false;

    @Nullable
    private Properties properties;

    public PersistenceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
      super(member, pd);
      PersistenceContext pc = ae.getAnnotation(PersistenceContext.class);
      PersistenceUnit pu = ae.getAnnotation(PersistenceUnit.class);
      Class<?> resourceType = EntityManager.class;
      if (pc != null) {
        if (pu != null) {
          throw new IllegalStateException("Member may only be annotated with either " +
                  "@PersistenceContext or @PersistenceUnit, not both: " + member);
        }
        Properties properties = null;
        PersistenceProperty[] pps = pc.properties();
        if (ObjectUtils.isNotEmpty(pps)) {
          properties = new Properties();
          for (PersistenceProperty pp : pps) {
            properties.setProperty(pp.name(), pp.value());
          }
        }
        this.unitName = pc.unitName();
        this.type = pc.type();
        this.synchronizedWithTransaction = SynchronizationType.SYNCHRONIZED.equals(pc.synchronization());
        this.properties = properties;
      }
      else {
        resourceType = EntityManagerFactory.class;
        this.unitName = pu.unitName();
      }
      checkResourceType(resourceType);
    }

    /**
     * Resolve the object against the application context.
     */
    @Override
    protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
      // Resolves to EntityManagerFactory or EntityManager.
      if (type != null) {
        return (type == PersistenceContextType.EXTENDED ?
                resolveExtendedEntityManager(target, requestingBeanName) :
                resolveEntityManager(requestingBeanName));
      }
      else {
        // OK, so we need an EntityManagerFactory...
        return resolveEntityManagerFactory(requestingBeanName);
      }
    }

    private EntityManagerFactory resolveEntityManagerFactory(@Nullable String requestingBeanName) {
      // Obtain EntityManagerFactory from JNDI?
      EntityManagerFactory emf = getPersistenceUnit(unitName);
      if (emf == null) {
        // Need to search for EntityManagerFactory beans.
        emf = findEntityManagerFactory(unitName, requestingBeanName);
      }
      return emf;
    }

    private EntityManager resolveEntityManager(@Nullable String requestingBeanName) {
      // Obtain EntityManager reference from JNDI?
      EntityManager em = getPersistenceContext(unitName, false);
      if (em == null) {
        // No pre-built EntityManager found -> build one based on factory.
        // Obtain EntityManagerFactory from JNDI?
        EntityManagerFactory emf = getPersistenceUnit(unitName);
        if (emf == null) {
          // Need to search for EntityManagerFactory beans.
          emf = findEntityManagerFactory(unitName, requestingBeanName);
        }
        // Inject a shared transactional EntityManager proxy.
        if (emf instanceof EntityManagerFactoryInfo emfInfo && emfInfo.getEntityManagerInterface() != null) {
          // Create EntityManager based on the info's vendor-specific type
          // (which might be more specific than the field's type).
          em = SharedEntityManagerCreator.createSharedEntityManager(
                  emf, properties, synchronizedWithTransaction);
        }
        else {
          // Create EntityManager based on the field's type.
          em = SharedEntityManagerCreator.createSharedEntityManager(
                  emf, properties, synchronizedWithTransaction, getResourceType());
        }
      }
      return em;
    }

    private EntityManager resolveExtendedEntityManager(Object target, @Nullable String requestingBeanName) {
      // Obtain EntityManager reference from JNDI?
      EntityManager em = getPersistenceContext(unitName, true);
      if (em == null) {
        // No pre-built EntityManager found -> build one based on factory.
        // Obtain EntityManagerFactory from JNDI?
        EntityManagerFactory emf = getPersistenceUnit(unitName);
        if (emf == null) {
          // Need to search for EntityManagerFactory beans.
          emf = findEntityManagerFactory(unitName, requestingBeanName);
        }
        // Inject a container-managed extended EntityManager.
        em = ExtendedEntityManagerCreator.createContainerManagedEntityManager(
                emf, properties, synchronizedWithTransaction);
      }
      if (em instanceof EntityManagerProxy emp && beanFactory != null && requestingBeanName != null &&
              beanFactory.containsBean(requestingBeanName) && !beanFactory.isPrototype(requestingBeanName)) {
        extendedEntityManagersToClose.put(target, emp.getTargetEntityManager());
      }
      return em;
    }
  }

  private static class AotContribution implements BeanRegistrationAotContribution {

    private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";

    private static final String INSTANCE_PARAMETER = "instance";

    private final Class<?> target;

    private final List<InjectedElement> injectedElements;

    AotContribution(Class<?> target, Collection<InjectedElement> injectedElements) {
      this.target = target;
      this.injectedElements = List.copyOf(injectedElements);
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      GeneratedClass generatedClass = generationContext.getGeneratedClasses()
              .addForFeatureComponent("PersistenceInjection", this.target, type -> {
                type.addJavadoc("Persistence injection for {@link $T}.", this.target);
                type.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
              });
      GeneratedMethod generatedMethod = generatedClass.getMethods().add("apply", method -> {
        method.addJavadoc("Apply the persistence injection.");
        method.addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                javax.lang.model.element.Modifier.STATIC);
        method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
        method.addParameter(this.target, INSTANCE_PARAMETER);
        method.returns(this.target);
        method.addCode(generateMethodCode(generationContext.getRuntimeHints(), generatedClass));
      });
      beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
    }

    private CodeBlock generateMethodCode(RuntimeHints hints, GeneratedClass generatedClass) {
      CodeBlock.Builder code = CodeBlock.builder();
      if (this.injectedElements.size() == 1) {
        code.add(generateInjectedElementMethodCode(hints, generatedClass, this.injectedElements.get(0)));
      }
      else {
        for (InjectedElement injectedElement : this.injectedElements) {
          code.addStatement(applyInjectedElement(hints, generatedClass, injectedElement));
        }
      }
      code.addStatement("return $L", INSTANCE_PARAMETER);
      return code.build();
    }

    private CodeBlock applyInjectedElement(RuntimeHints hints, GeneratedClass generatedClass, InjectedElement injectedElement) {
      String injectedElementName = injectedElement.getMember().getName();
      GeneratedMethod generatedMethod = generatedClass.getMethods().add(new String[] { "apply", injectedElementName }, method -> {
        method.addJavadoc("Apply the persistence injection for '$L'.", injectedElementName);
        method.addModifiers(javax.lang.model.element.Modifier.PRIVATE,
                javax.lang.model.element.Modifier.STATIC);
        method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
        method.addParameter(this.target, INSTANCE_PARAMETER);
        method.addCode(generateInjectedElementMethodCode(hints, generatedClass, injectedElement));
      });
      var argumentCodeGenerator = ArgumentCodeGenerator.of(RegisteredBean.class, REGISTERED_BEAN_PARAMETER)
              .and(this.target, INSTANCE_PARAMETER);
      return generatedMethod.toMethodReference().toInvokeCodeBlock(argumentCodeGenerator, generatedClass.getName());
    }

    private CodeBlock generateInjectedElementMethodCode(RuntimeHints hints, GeneratedClass generatedClass,
            InjectedElement injectedElement) {

      CodeBlock.Builder code = CodeBlock.builder();
      var injectionCodeGenerator = new InjectionCodeGenerator(generatedClass.getName(), hints);
      CodeBlock resourceToInject = generateResourceToInjectCode(generatedClass.getMethods(),
              (PersistenceElement) injectedElement);
      code.add(injectionCodeGenerator.generateInjectionCode(
              injectedElement.getMember(), INSTANCE_PARAMETER,
              resourceToInject));
      return code.build();
    }

    private CodeBlock generateResourceToInjectCode(
            GeneratedMethods generatedMethods, PersistenceElement injectedElement) {

      String unitName = injectedElement.unitName;
      boolean requireEntityManager = (injectedElement.type != null);
      if (!requireEntityManager) {
        return CodeBlock.of(
                "$T.findEntityManagerFactory($L.getBeanFactory(), $S)",
                EntityManagerFactoryUtils.class, REGISTERED_BEAN_PARAMETER, unitName);
      }
      String[] methodNameParts = { "get", unitName, "EntityManager" };
      GeneratedMethod generatedMethod = generatedMethods.add(methodNameParts, method ->
              generateGetEntityManagerMethod(method, injectedElement));
      return CodeBlock.of("$L($L)", generatedMethod.getName(), REGISTERED_BEAN_PARAMETER);
    }

    private void generateGetEntityManagerMethod(MethodSpec.Builder method, PersistenceElement injectedElement) {
      String unitName = injectedElement.unitName;
      Properties properties = injectedElement.properties;
      method.addJavadoc("Get the '$L' {@link $T}.",
              (StringUtils.isNotEmpty(unitName)) ? unitName : "default",
              EntityManager.class);
      method.addModifiers(javax.lang.model.element.Modifier.PUBLIC,
              javax.lang.model.element.Modifier.STATIC);
      method.returns(EntityManager.class);
      method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
      method.addStatement(
              "$T entityManagerFactory = $T.findEntityManagerFactory($L.getBeanFactory(), $S)",
              EntityManagerFactory.class, EntityManagerFactoryUtils.class, REGISTERED_BEAN_PARAMETER, unitName);
      boolean hasProperties = CollectionUtils.isNotEmpty(properties);
      if (hasProperties) {
        method.addStatement("$T properties = new Properties()",
                Properties.class);
        for (String propertyName : new TreeSet<>(properties.stringPropertyNames())) {
          method.addStatement("properties.put($S, $S)", propertyName, properties.getProperty(propertyName));
        }
      }
      method.addStatement(
              "return $T.createSharedEntityManager(entityManagerFactory, $L, $L)",
              SharedEntityManagerCreator.class,
              (hasProperties) ? "properties" : null,
              injectedElement.synchronizedWithTransaction);
    }
  }

}
