/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.NullValue;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * Simple object instantiation strategy for use in a BeanFactory.
 *
 * <p>Does not support Method Injection, although it provides hooks for subclasses
 * to override to add Method Injection support, for example by overriding methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/10 22:06
 */
public class InstantiationStrategy {

  private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

  /**
   * Return the factory method currently being invoked or {@code null} if none.
   * <p>Allows factory method implementations to determine whether the current
   * caller is the container itself as opposed to user code.
   */
  @Nullable
  public static Method getCurrentlyInvokedFactoryMethod() {
    return currentlyInvokedFactoryMethod.get();
  }

  /**
   * Invoke the given {@code instanceSupplier} with the factory method exposed
   * as being invoked.
   *
   * @param method the factory method to expose
   * @param instanceSupplier the instance supplier
   * @param <T> the type of the instance
   * @return the result of the instance supplier
   * @since 5.0
   */
  public static <T> T instantiateWithFactoryMethod(Method method, Supplier<T> instanceSupplier) {
    Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
    try {
      currentlyInvokedFactoryMethod.set(method);
      return instanceSupplier.get();
    }
    finally {
      if (priorInvokedFactoryMethod != null) {
        currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
      }
      else {
        currentlyInvokedFactoryMethod.remove();
      }
    }
  }

  /**
   * Return an instance of the bean with the given name in this factory.
   *
   * @param bd the bean definition
   * @param beanName the name of the bean when it is created in this context.
   * The name can be {@code null} if we are autowiring a bean which doesn't
   * belong to the factory.
   * @param owner the owning BeanFactory
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) throws BeansException {
    // Don't override the class with CGLIB if no overrides.
    if (bd.hasMethodOverrides()) {
      // Must generate CGLIB subclass.
      return instantiateWithMethodInjection(bd, beanName, owner);
    }
    else {
      Constructor<?> constructorToUse;
      synchronized(bd.constructorArgumentLock) {
        constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
        if (constructorToUse == null) {
          final Class<?> clazz = bd.getBeanClass();
          if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
          }
          try {
            constructorToUse = clazz.getDeclaredConstructor();
            bd.resolvedConstructorOrFactoryMethod = constructorToUse;
          }
          catch (Throwable ex) {
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
          }
        }
      }
      return BeanUtils.newInstance(constructorToUse);
    }
  }

  /**
   * Subclasses can override this method, which is implemented to throw
   * UnsupportedOperationException, if they can instantiate an object with
   * the Method Injection specified in the given RootBeanDefinition.
   * Instantiation should use a no-arg constructor.
   */
  protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
    throw new UnsupportedOperationException("Method Injection not supported in InstantiationStrategy");
  }

  /**
   * Return an instance of the bean with the given name in this factory,
   * creating it via the given constructor.
   *
   * @param bd the bean definition
   * @param beanName the name of the bean when it is created in this context.
   * The name can be {@code null} if we are autowiring a bean which doesn't
   * belong to the factory.
   * @param owner the owning BeanFactory
   * @param ctor the constructor to use
   * @param args the constructor arguments to apply
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  public Object instantiate(RootBeanDefinition bd, @Nullable String beanName,
          BeanFactory owner, Constructor<?> ctor, @Nullable Object... args) throws BeansException {
    if (bd.hasMethodOverrides()) {
      return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
    }
    else {
      return BeanUtils.newInstance(ctor, args);
    }
  }

  /**
   * Subclasses can override this method, which is implemented to throw
   * UnsupportedOperationException, if they can instantiate an object with
   * the Method Injection specified in the given RootBeanDefinition.
   * Instantiation should use the given constructor and parameters.
   */
  protected Object instantiateWithMethodInjection(RootBeanDefinition bd,
          @Nullable String beanName, BeanFactory owner, @Nullable Constructor<?> ctor, @Nullable Object... args) {

    throw new UnsupportedOperationException("Method Injection not supported in InstantiationStrategy");
  }

  /**
   * Return an instance of the bean with the given name in this factory,
   * creating it via the given factory method.
   *
   * @param merged the bean definition
   * @param owner the owning BeanFactory
   * @param factoryBean the factory bean instance to call the factory method on,
   * or {@code null} in case of a static factory method
   * @param factoryMethod the factory method to use
   * @param args the factory method arguments to apply
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   * @see NullValue#INSTANCE
   */
  @SuppressWarnings("NullAway")
  public Object instantiate(RootBeanDefinition merged, @Nullable String beanName, BeanFactory owner,
          @Nullable Object factoryBean, final Method factoryMethod, @Nullable Object @Nullable ... args) throws BeansException {
    return instantiateWithFactoryMethod(factoryMethod, () -> {
      try {
        ReflectionUtils.makeAccessible(factoryMethod);
        Object result = factoryMethod.invoke(factoryBean, args);
        if (result == null) {
          result = NullValue.INSTANCE;
        }
        return result;
      }
      catch (IllegalArgumentException ex) {
        if (factoryBean != null && !factoryMethod.getDeclaringClass().isAssignableFrom(factoryBean.getClass())) {
          throw new BeanInstantiationException(factoryMethod,
                  "Illegal factory instance for factory method '" + factoryMethod.getName() + "'; " +
                          "instance: " + factoryBean.getClass().getName(), ex);
        }
        throw new BeanInstantiationException(factoryMethod,
                "Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
                        "args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
      }
      catch (IllegalAccessException | InaccessibleObjectException ex) {
        throw new BeanInstantiationException(factoryMethod,
                "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
      }
      catch (InvocationTargetException ex) {
        String msg = "Factory method '" + factoryMethod.getName() +
                "' threw exception with message: " + ex.getTargetException().getMessage();
        if (merged.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
                ((ConfigurableBeanFactory) owner).isCurrentlyInCreation(merged.getFactoryBeanName())) {
          msg = "Circular reference involving containing bean '" + merged.getFactoryBeanName() + "' - consider " +
                  "declaring the factory method as static for independence from its containing instance. " + msg;
        }
        throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
      }
    });
  }

  /**
   * Determine the actual class for the given bean definition, as instantiated at runtime.
   */
  public Class<?> getActualBeanClass(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
    return bd.getBeanClass();
  }

}
