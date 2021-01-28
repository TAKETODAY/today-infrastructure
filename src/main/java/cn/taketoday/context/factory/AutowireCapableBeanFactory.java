/**
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
package cn.taketoday.context.factory;

import java.util.Set;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.exception.BeanInitializingException;
import cn.taketoday.context.exception.ContextException;

/**
 * @author TODAY
 * 2020/9/13 10:54
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

  /**
   * Fully create a new bean instance of the given class.
   * <p>
   * Performs full initialization of the bean, including all applicable
   * {@link BeanPostProcessor BeanPostProcessors}.
   * <p>
   * Note: This is intended for creating a fresh instance, populating annotated
   * fields and methods as well as applying all standard bean initialization
   * callbacks. It does <i>not</i> imply traditional by-name or by-type autowiring
   * of properties; use {@link #createBean(Class, boolean)} for those purposes.
   *
   * @param beanClass
   *         the class of the bean to create
   *
   * @return the new bean instance
   *
   * @throws ContextException
   *         if instantiation or wiring failed
   */
  default <T> T createBean(Class<T> beanClass) {
    return createBean(beanClass, false);
  }

  /**
   * Fully create a new bean instance of the given class with the specified
   * autowire strategy. All constants defined in this interface are supported
   * here.
   * <p>
   * Performs full initialization of the bean, including all applicable
   * {@link BeanPostProcessor BeanPostProcessors}.
   *
   * @param beanClass
   *         the class of the bean to create whether to perform a dependency
   *         check for objects (not applicable to autowiring a constructor,
   *         thus ignored there)
   * @param cacheBeanDef
   *         cache bean definition
   *
   * @return the new bean instance
   *
   * @throws ContextException
   *         if instantiation or wiring failed
   */
  <T> T createBean(Class<T> beanClass, boolean cacheBeanDef);

  /**
   * Populate the given bean instance through applying after-instantiation
   * callbacks and bean property post-processing (e.g. for annotation-driven
   * injection).
   * <p>
   * Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
   * callbacks.
   *
   * @param existingBean
   *         the existing bean instance
   *
   * @throws ContextException
   *         if wiring failed
   */
  void autowireBean(Object existingBean);

  /**
   * Autowire the bean properties of the given bean instance by name or type.
   * <p>
   * Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
   * callbacks or perform any further initialization of the bean.
   *
   * @param existingBean
   *         the existing bean instance
   *
   * @throws ContextException
   *         if wiring failed
   */
  void autowireBeanProperties(Object existingBean);

  /**
   * Initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean definition of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param existingBean
   *         the existing bean instance
   * @param beanName
   *         the name of the bean
   *
   * @return the bean instance to use, either the original or a wrapped one
   *
   * @throws BeanInitializingException
   *         if the initialization failed
   */
  Object initializeBean(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Fully initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean definition of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param existingBean
   *         the existing bean instance
   * @param def
   *         the bean def of the bean
   *
   * @return the bean instance to use, either the original or a wrapped one
   *
   * @throws BeanInitializingException
   *         if the initialization failed
   */
  Object initializeBean(final Object existingBean, final BeanDefinition def)
          throws BeanInitializingException;

  /**
   * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
   * instance, invoking their {@code postProcessBeforeInitialization} methods. The
   * returned bean instance may be a wrapper around the original.
   *
   * @param existingBean
   *         the existing bean instance
   * @param beanName
   *         the name of the bean
   *
   * @throws BeanInitializingException
   *         if any post-processing failed
   * @see BeanPostProcessor#postProcessBeforeInitialization
   */
  Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
   * instance, invoking their {@code postProcessAfterInitialization} methods. The
   * returned bean instance may be a wrapper around the original.
   *
   * @param existingBean
   *         the existing bean instance
   * @param beanName
   *         the name of the bean
   *
   * @throws BeanInitializingException
   *         if any post-processing failed
   * @see BeanPostProcessor#postProcessAfterInitialization
   */
  Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Destroy the given bean instance (typically coming from {@link #createBean}),
   * applying the {@link DisposableBean} contract as well as registered
   * {@link DestructionBeanPostProcessor DestructionBeanPostProcessor}.
   * <p>
   * Any exception that arises during destruction should be caught and logged
   * instead of propagated to the caller of this method.
   *
   * @param existingBean
   *         the bean instance to destroy
   */
  void destroyBean(Object existingBean);

  /**
   * Load {@link Import} beans from input bean classes
   * <p>
   * importBeans will register target beans's BeanDefinition
   *
   * @param beans
   *         Input bean classes
   *
   * @since 3.0
   */
  void importBeans(Class<?>... beans);

  /**
   * Load {@link Import} beans from input {@link BeanDefinition}
   *
   * @param def
   *         Input {@link BeanDefinition}
   *
   * @since 3.0
   */
  void importBeans(BeanDefinition def);

  /**
   * Load {@link Import} beans from input {@link BeanDefinition}s
   *
   * @param defs
   *         Input {@link BeanDefinition}s
   *
   * @since 3.0
   */
  void importBeans(Set<BeanDefinition> defs);

}
