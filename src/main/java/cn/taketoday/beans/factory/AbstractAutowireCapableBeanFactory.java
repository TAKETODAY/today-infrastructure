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

package cn.taketoday.beans.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.beans.support.PropertyValuesBinder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * AutowireCapableBeanFactory abstract implementation
 *
 * @author TODAY 2021/10/1 23:06
 * @since 4.0
 */
public abstract class AbstractAutowireCapableBeanFactory
        extends AbstractBeanFactory implements AutowireCapableBeanFactory {
  private static final Logger log = LoggerFactory.getLogger(AbstractAutowireCapableBeanFactory.class);

  //---------------------------------------------------------------------
  // Implementation of AutowireCapableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createBean(Class<T> beanClass, boolean cacheBeanDef) {
    BeanDefinition defToUse;
    if (cacheBeanDef) {
      if ((defToUse = getBeanDefinition(beanClass)) == null) {
        defToUse = getPrototypeBeanDefinition(beanClass);
        registerBeanDefinition(defToUse.getName(), defToUse);
      }
    }
    else {
      defToUse = getPrototypeBeanDefinition(beanClass);
    }
    return (T) createBean(defToUse, null);
  }

  protected abstract BeanDefinition getBeanDefinition(Class<?> beanClass);

  @Override
  public void autowireBean(Object existingBean) {
    Class<Object> userClass = ClassUtils.getUserClass(existingBean);
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(userClass);
    if (log.isDebugEnabled()) {
      log.debug("Autowiring bean named: [{}].", prototypeDef.getName());
    }

    // apply properties
    applyPropertyValues(existingBean, prototypeDef);
  }

  protected Object createBean(BeanDefinition definition, @Nullable Object[] args) throws BeanCreationException {
    if (log.isTraceEnabled()) {
      log.trace("Creating instance of bean '{}'", definition.getName());
    }

    Class<?> resolvedClass = resolveBeanClass(definition);
    try {
      // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
      Object bean = resolveBeforeInstantiation(resolvedClass, definition);
      if (bean != null) {
        return bean;
      }
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(), definition.getName(),
              "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
      Object beanInstance = doCreateBean(definition, args);
      if (log.isTraceEnabled()) {
        log.trace("Finished creating instance of bean '{}'", definition.getName());
      }
      return beanInstance;
    }
    catch (BeanCreationException ex) {
      // A previously detected exception with proper bean creation context already,
      // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              definition.getResourceDescription(),
              definition.getName(), "Unexpected exception during bean creation", ex);
    }
  }

  /**
   * Actually create the specified bean. Pre-creation processing has already happened
   * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
   * <p>Differentiates between default bean instantiation, use of a
   * factory method, and autowiring a constructor.
   *
   * @param definition the merged bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   */
  protected Object doCreateBean(BeanDefinition definition, @Nullable Object[] args) throws BeanCreationException {
    Object bean = createBeanInstance(definition, args);
    try {
      // apply properties
      populateBean(bean, definition);
      // Initialize the bean instance.
      bean = initializeBean(bean, definition);
      // Register bean as disposable.
      try {
        registerDisposableBeanIfNecessary(definition.getName(), bean, definition);
      }
      catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
                definition.getResourceDescription(), definition.getName(), "Invalid destruction signature", ex);
      }
      return bean;
    }
    catch (Throwable ex) {
      if (ex instanceof BeanCreationException && definition.getName().equals(((BeanCreationException) ex).getBeanName())) {
        throw (BeanCreationException) ex;
      }
      else {
        throw new BeanCreationException(
                definition.getResourceDescription(), definition.getName(), "Initialization of bean failed", ex);
      }
    }

  }

  /**
   * Apply before-instantiation post-processors, resolving whether there is a
   * before-instantiation shortcut for the specified bean.
   *
   * @param beanClass bean class
   * @param definition the bean definition for the bean
   * @return the shortcut-determined bean instance, or {@code null} if none
   */
  @Nullable
  protected Object resolveBeforeInstantiation(Class<?> beanClass, BeanDefinition definition) {
    Object bean = null;
    if (!Boolean.FALSE.equals(definition.beforeInstantiationResolved)) {
      // Make sure bean class is actually resolved at this point.
      if (!definition.isSynthetic()) {
        bean = applyBeanPostProcessorsBeforeInstantiation(beanClass, definition.getName());
        if (bean != null) {
          bean = applyBeanPostProcessorsAfterInitialization(bean, definition.getName());
        }
      }
      definition.beforeInstantiationResolved = (bean != null);
    }
    return bean;
  }

  /**
   * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
   * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
   * <p>Any returned object will be used as the bean instead of actually instantiating
   * the target bean. A {@code null} return value from the post-processor will
   * result in the target bean being instantiated.
   *
   * @param beanClass the class of the bean to be instantiated
   * @param beanName the name of the bean
   * @return the bean object to use instead of a default instance of the target bean, or {@code null}
   * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
   */
  @Nullable
  protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
    for (InstantiationAwareBeanPostProcessor processor : postProcessors().instantiation) {
      Object result = processor.postProcessBeforeInstantiation(beanClass, beanName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  protected Object createBeanInstance(BeanDefinition mbd, @Nullable Object[] args) {
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }
    return instantiate(mbd, args);
  }

  @Override
  protected Object createBeanInstance(BeanDefinition def) {
    return createBeanInstance(def, null);
  }

  private Object instantiate(BeanDefinition def, @Nullable Object[] constructorArgs) {
    BeanInstantiator instantiator = resolveBeanInstantiator(def);
    if (constructorArgs == null) {
      constructorArgs = def.getConstructorArgs();
      if (constructorArgs == null) {
        constructorArgs = getArgumentsResolver().resolve(def.executable, this);
      }
    }
    return instantiator.instantiate(constructorArgs);
  }

  private BeanInstantiator resolveBeanInstantiator(BeanDefinition definition) {
    if (definition.instantiator == null) {
      String factoryMethodName = definition.getFactoryMethodName();
      // instantiate using factory-method
      if (factoryMethodName != null) {
        String factoryBeanName = definition.getFactoryBeanName();
        Class<?> factoryClass = getFactoryClass(definition, factoryBeanName);
        Method factoryMethod = getFactoryMethod(definition, factoryClass, factoryMethodName);
        MethodInvoker factoryMethodInvoker = determineMethodInvoker(definition, factoryMethod);
        if (Modifier.isStatic(factoryMethod.getModifiers())) {
          definition.instantiator = BeanInstantiator.fromStaticMethod(factoryMethodInvoker);
        }
        else {
          // this is not a FactoryBean just a factory
          Object factoryBean = getBean(factoryBeanName);
          definition.instantiator = BeanInstantiator.fromMethod(factoryMethodInvoker, factoryBean);
        }
        definition.executable = factoryMethod;
      }
      else {
        // use a suitable constructor
        Class<?> beanClass = resolveBeanClass(definition);
        Constructor<?> constructor = BeanUtils.getConstructor(beanClass);
        if (definition.isSingleton()) {
          // use java-reflect invoking
          definition.instantiator = BeanInstantiator.fromReflective(constructor);
        }
        else {
          // provide fast access the method
          definition.instantiator = BeanInstantiator.fromConstructor(constructor);
        }
        definition.executable = constructor;
      }
    }
    return definition.instantiator;
  }

  private MethodInvoker determineMethodInvoker(BeanDefinition definition, Method factoryMethod) {
    if (definition.isSingleton()) {
      // use java-reflect invoking
      return MethodInvoker.formReflective(factoryMethod);
    }
    else {
      // provide fast access the method
      return MethodInvoker.fromMethod(factoryMethod);
    }
  }

  private Class<?> getFactoryClass(BeanDefinition definition, String factoryBeanName) {
    Class<?> factoryClass;
    if (factoryBeanName != null) {
      // instance method
      factoryClass = getType(factoryBeanName);
    }
    else {
      // bean class is its factory-class
      factoryClass = resolveBeanClass(definition);
    }

    if (factoryClass == null) {
      throw new IllegalStateException(
              "factory-method: '" + definition.getFactoryMethodName() + "' its factory bean: '" +
                      factoryBeanName + "' not found in this factory: " + this);
    }
    return factoryClass;
  }

  @NonNull
  protected Method getFactoryMethod(BeanDefinition def, Class<?> factoryClass, String factoryMethodName) {
    ArrayList<Method> candidates = new ArrayList<>();
    ReflectionUtils.doWithMethods(factoryClass, method -> {
      if (def.isFactoryMethod(method)) {
        candidates.add(method);
      }
    }, ReflectionUtils.USER_DECLARED_METHODS);

    if (candidates.isEmpty()) {
      throw new IllegalStateException(
              "factory method: '" + factoryMethodName + "' not found in class: " + factoryClass.getName());
    }

    if (candidates.size() > 1) {
      candidates.sort((o1, o2) -> {
        // static first, parameter
        int result = Boolean.compare(Modifier.isPublic(o1.getModifiers()), Modifier.isPublic(o2.getModifiers()));
        if (result == 0) {
          result = Boolean.compare(Modifier.isStatic(o1.getModifiers()), Modifier.isStatic(o2.getModifiers()));
          return result == 0 ? Integer.compare(o1.getParameterCount(), o2.getParameterCount()) : result;
        }
        return result;
      });
    }
    if (log.isDebugEnabled()) {
      log.debug("bean-definition {} using factory-method {} to create bean instance", def, candidates.get(0));
    }
    return candidates.get(0);
  }

  @Override
  public Object autowire(Class<?> beanClass) throws BeansException {
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(beanClass);
    Object existingBean = instantiate(prototypeDef, null);
    applyPropertyValues(existingBean, prototypeDef);
    return existingBean;
  }

  @Override
  public Object configureBean(Object existingBean, String beanName) throws BeansException {
    BeanDefinition definition = getBeanDefinition(beanName);
    if (definition == null) {
      definition = getPrototypeBeanDefinition(existingBean.getClass());
    }
    else {
      definition = definition.cloneDefinition();
      definition.setScope(Scope.PROTOTYPE);
    }
    return initializeBean(existingBean, definition);
  }

  public void populateBean(Object bean, BeanDefinition definition) {
    // postProcess();

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    if (!definition.isSynthetic()) {
      String name = definition.getName();
      for (InstantiationAwareBeanPostProcessor processor : postProcessors().instantiation) {
        if (!processor.postProcessAfterInstantiation(bean, name)) {
          return;
        }
      }
    }

    applyPropertyValues(bean, definition);
  }

  @Override
  protected void applyPropertyValues(Object bean, BeanDefinition def) {
    // -----------------------------------------------
    // apply dependency injection (DI)
    // -----------------------------------------------

    // 1. apply map of property-values from bean definition
    Map<String, Object> propertyValues = def.getPropertyValues();
    if (CollectionUtils.isNotEmpty(propertyValues)) {
      PropertyValuesBinder dataBinder = new PropertyValuesBinder(bean);
      initPropertyValuesBinder(dataBinder);
      dataBinder.bind(propertyValues);
    }

    // 2. apply outside framework expanded
    if (!def.isSynthetic()) {
      for (DependenciesBeanPostProcessor processor : postProcessors().dependencies) {
        processor.postProcessDependencies(bean, def);
      }
    }
  }

  @Override
  public Object initializeBean(Object existingBean) throws BeanInitializingException {
    return initializeBean(existingBean, createBeanName(existingBean.getClass()));
  }

  @Override
  public Object initializeBean(Object existingBean, String beanName) {
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    return initializeBean(existingBean, prototypeDef);
  }

  @Override
  public Object applyBeanPostProcessorsBeforeInitialization(
          Object existingBean, String beanName
  ) {
    Object ret = existingBean;
    // before properties
    for (InitializationBeanPostProcessor processor : postProcessors().initialization) {
      try {
        ret = processor.postProcessBeforeInitialization(ret, beanName);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + existingBean + "] before properties set", e);
      }
    }
    return ret;
  }

  @Override
  public Object applyBeanPostProcessorsAfterInitialization(
          Object existingBean, String beanName
  ) {
    Object ret = existingBean;
    // after properties
    for (InitializationBeanPostProcessor processor : postProcessors().initialization) {
      try {
        ret = processor.postProcessAfterInitialization(ret, beanName);
      }
      catch (Exception e) {
        throw new BeanInitializingException(
                "An Exception Occurred When [" + existingBean + "] after properties set", e);
      }
    }
    return ret;
  }

  @Override
  public void destroyBean(Object existingBean) {
    destroyBean(existingBean, getPrototypeBeanDefinition(ClassUtils.getUserClass(existingBean)));
  }

  private BeanDefinition getPrototypeBeanDefinition(Object existingBean, String beanName) {
    BeanDefinition def = getPrototypeBeanDefinition(ClassUtils.getUserClass(existingBean));
    def.setName(beanName);
    return def;
  }

  //---------------------------------------------------------------------
  // Implementation of AbstractBeanFactory class
  //---------------------------------------------------------------------

  @Override
  protected BeanDefinition getPrototypeBeanDefinition(Class<?> beanClass) {
    BeanDefinition defaults = BeanDefinitionBuilder.defaults(beanClass);
    defaults.setScope(Scope.PROTOTYPE);
    return defaults;
  }

  @Override
  @Nullable
  protected Class<?> predictBeanType(BeanDefinition definition) {
    String factoryMethodName = definition.getFactoryMethodName();
    if (factoryMethodName != null) {
      return getTypeForFactoryMethod(definition);
    }
    return super.predictBeanType(definition);
  }

  /**
   * Determine the target type for the given bean definition which is based on
   * a factory method. Only called if there is no singleton instance registered
   * for the target bean already.
   * <p>This implementation determines the type matching {@link #createBean}'s
   * different creation strategies. As far as possible, we'll perform static
   * type checking to avoid creation of the target bean.
   *
   * @param def the merged bean definition for the bean
   * @return the type for the bean if determinable, or {@code null} otherwise
   * @see #createBean
   */
  @Nullable
  protected Class<?> getTypeForFactoryMethod(BeanDefinition def) {
    ResolvableType cachedReturnType = def.factoryMethodReturnType;
    if (cachedReturnType != null) {
      return cachedReturnType.resolve();
    }
    Method factoryMethod;
    Executable uniqueCandidate = def.executable;
    if (uniqueCandidate instanceof Method) {
      factoryMethod = ((Method) uniqueCandidate);
    }
    else {
      String factoryBeanName = def.getFactoryBeanName();
      Class<?> factoryClass = getFactoryClass(def, factoryBeanName);
      // If all factory methods have the same return type, return that type.
      // Can't clearly figure out exact method due to type converting / autowiring!
      factoryMethod = getFactoryMethod(def, factoryClass, def.getFactoryMethodName());
      def.executable = factoryMethod;
      return factoryMethod.getReturnType();
    }

    // Common return type found: all factory methods return same type. For a non-parameterized
    // unique candidate, cache the full type declaration context of the target factory method.
    cachedReturnType = ResolvableType.forReturnType(factoryMethod);
    return cachedReturnType.resolve();
  }

}
