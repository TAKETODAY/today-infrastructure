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
import java.util.Comparator;
import java.util.function.Supplier;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.NonNull;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
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
    return (T) createPrototype(defToUse);
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

  @Override
  protected Object createBeanInstance(BeanDefinition def) {
    if (hasInstantiationAwareBeanPostProcessors) {
      for (BeanPostProcessor processor : postProcessors) {
        if (processor instanceof InstantiationAwareBeanPostProcessor) {
          Object bean = ((InstantiationAwareBeanPostProcessor) processor).postProcessBeforeInstantiation(def);
          if (bean != null) {
            return bean;
          }
        }
      }
    }
    return instantiate(def);
  }

  private Object instantiate(BeanDefinition def) {
    Supplier<?> instanceSupplier = def.getInstanceSupplier();
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }

    BeanInstantiator instantiator;
    Executable executable;
    String factoryMethodName = def.getFactoryMethodName();
    // instantiate using factory-method
    if (factoryMethodName != null) {
      String factoryBeanName = def.getFactoryBeanName();

      Class<?> factoryClass;
      // a static factory-method ?
      boolean isStatic = false;
      if (factoryBeanName != null) {
        // instance method
        factoryClass = getType(factoryBeanName);
        if (factoryClass == null) {
          throw new IllegalStateException(
                  "factory-method: '" + factoryMethodName + "' its factory bean: '" +
                          factoryBeanName + "' not found in this factory: " + this);
        }
      }
      else {
        // bean class is its factory-class
        factoryClass = resolveBeanClass(def); ;
        isStatic = true;
      }

      Method factoryMethod = getFactoryMethod(def, factoryClass, factoryMethodName);
      MethodInvoker factoryMethodInvoker;
      if (def.isSingleton()) {
        // use java-reflect invoking
        factoryMethodInvoker = MethodInvoker.formReflective(factoryMethod);
      }
      else {
        // provide fast access the method
        factoryMethodInvoker = MethodInvoker.fromMethod(factoryMethod);
      }

      if (Modifier.isStatic(factoryMethod.getModifiers())) {
        instantiator = BeanInstantiator.fromStaticMethod(factoryMethodInvoker);
      }
      else {
        // this is not a FactoryBean just a factory
        Object factoryBean = getBean(factoryBeanName);
        instantiator = BeanInstantiator.fromMethod(factoryMethodInvoker, factoryBean);
      }
      executable = factoryMethod;
    }
    else {
      // use a suitable constructor
      Class<?> beanClass = resolveBeanClass(def);
      Constructor<?> constructor = BeanUtils.getConstructor(beanClass);
      if (def.isSingleton()) {
        // use java-reflect invoking
        instantiator = BeanInstantiator.fromReflective(constructor);
      }
      else {
        // provide fast access the method
        instantiator = BeanInstantiator.fromConstructor(constructor);
      }
      executable = constructor;
    }
    
    Object[] constructorArgs = def.getConstructorArgs();
    if (constructorArgs == null) {
      constructorArgs = getArgumentsResolver().resolve(executable, this);
    }
    return instantiator.instantiate(constructorArgs);
  }

  @NonNull
  private Method getFactoryMethod(BeanDefinition def, Class<?> factoryClass, String factoryName) {
    ArrayList<Method> candidates = new ArrayList<>();
    ReflectionUtils.doWithMethods(factoryClass, method -> {
      if (def.isFactoryMethod(method)) {
        candidates.add(method);
      }
    });

    if (candidates.isEmpty()) {
      throw new IllegalStateException(
              "factory method: '" + factoryName + "' not found in class: " + factoryClass.getName());
    }

    if (candidates.size() > 1) {
      candidates.sort(new Comparator<Method>() {
        @Override
        public int compare(Method o1, Method o2) {
          // static first, parameter
          int result = Boolean.compare(Modifier.isStatic(o1.getModifiers()), Modifier.isStatic(o2.getModifiers()));
          return result == 0 ? Integer.compare(o1.getParameterCount(), o2.getParameterCount()) : result;
        }
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
    Object existingBean = instantiate(prototypeDef);
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
    if (!definition.isSynthetic() && hasInstantiationAwareBeanPostProcessors) {
      for (BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof InstantiationAwareBeanPostProcessor) {
          InstantiationAwareBeanPostProcessor processor = (InstantiationAwareBeanPostProcessor) postProcessor;
          if (!processor.postProcessAfterInstantiation(bean, definition)) {
            return;
          }
        }
      }
    }

    applyPropertyValues(bean, definition);

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
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    // before properties
    for (BeanPostProcessor processor : getPostProcessors()) {
      try {
        ret = processor.postProcessBeforeInitialization(ret, prototypeDef);
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
    BeanDefinition prototypeDef = getPrototypeBeanDefinition(existingBean, beanName);
    // after properties
    for (BeanPostProcessor processor : getPostProcessors()) {
      try {
        ret = processor.postProcessAfterInitialization(ret, prototypeDef);
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

}
