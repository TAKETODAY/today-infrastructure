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

package infra.beans.factory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.beans.BeanUtils;
import infra.beans.BeansException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.core.ResolvableType;
import infra.core.env.Environment;

/**
 * Used in {@link BeanRegistrar#register(BeanRegistry, Environment)} to expose
 * programmatic bean registration capabilities.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface BeanRegistry {

  /**
   * Register a bean from the given bean class, which will be instantiated
   * using the related {@link BeanUtils#getConstructor resolvable constructor}
   * if any.
   *
   * @param beanClass the class of the bean
   * @return the generated bean name
   */
  <T> String registerBean(Class<T> beanClass);

  /**
   * Register a bean from the given bean class, customizing it with the customizer
   * callback. The bean will be instantiated using the supplier that can be
   * configured in the customizer callback, or will be tentatively instantiated
   * with its {@link BeanUtils#getConstructor resolvable constructor}
   * otherwise.
   *
   * @param beanClass the class of the bean
   * @param customizer callback to customize other bean properties than the name
   * @return the generated bean name
   */
  <T> String registerBean(Class<T> beanClass, Consumer<Spec<T>> customizer);

  /**
   * Register a bean from the given bean class, which will be instantiated
   * using the related {@link BeanUtils#getConstructor resolvable constructor}
   * if any.
   *
   * @param name the name of the bean
   * @param beanClass the class of the bean
   */
  <T> void registerBean(String name, Class<T> beanClass);

  /**
   * Register a bean from the given bean class, customizing it with the customizer
   * callback. The bean will be instantiated using the supplier that can be
   * configured in the customizer callback, or will be tentatively instantiated with its
   * {@link BeanUtils#getConstructor resolvable constructor} otherwise.
   *
   * @param name the name of the bean
   * @param beanClass the class of the bean
   * @param customizer callback to customize other bean properties than the name
   */
  <T> void registerBean(String name, Class<T> beanClass, Consumer<Spec<T>> customizer);

  /**
   * Specification for customizing a bean.
   *
   * @param <T> the bean type
   */
  interface Spec<T> {

    /**
     * Allow for instantiating this bean on a background thread.
     *
     * @see AbstractBeanDefinition#setBackgroundInit(boolean)
     */
    Spec<T> backgroundInit();

    /**
     * Set a human-readable description of this bean.
     *
     * @see BeanDefinition#setDescription(String)
     */
    Spec<T> description(String description);

    /**
     * Configure this bean as a fallback autowire candidate.
     *
     * @see BeanDefinition#setFallback(boolean)
     * @see #primary
     */
    Spec<T> fallback();

    /**
     * Hint that this bean has an infrastructure role, meaning it has no
     * relevance to the end-user.
     *
     * @see BeanDefinition#setRole(int)
     * @see BeanDefinition#ROLE_INFRASTRUCTURE
     */
    Spec<T> infrastructure();

    /**
     * Configure this bean as lazily initialized.
     *
     * @see BeanDefinition#setLazyInit(boolean)
     */
    Spec<T> lazyInit();

    /**
     * Configure this bean as not a candidate for getting autowired into some
     * other bean.
     *
     * @see BeanDefinition#setAutowireCandidate(boolean)
     */
    Spec<T> notAutowirable();

    /**
     * The sort order of this bean. This is analogous to the
     * {@code @Order} annotation.
     *
     * @see AbstractBeanDefinition#ORDER_ATTRIBUTE
     */
    Spec<T> order(int order);

    /**
     * Configure this bean as a primary autowire candidate.
     *
     * @see BeanDefinition#setPrimary(boolean)
     * @see #fallback
     */
    Spec<T> primary();

    /**
     * Configure this bean with a prototype scope.
     *
     * @see BeanDefinition#setScope(String)
     * @see BeanDefinition#SCOPE_PROTOTYPE
     */
    Spec<T> prototype();

    /**
     * Set the supplier to construct a bean instance.
     *
     * @see AbstractBeanDefinition#setInstanceSupplier(Supplier)
     */
    Spec<T> supplier(Function<SupplierContext, T> supplier);
  }

  /**
   * Context available from the bean instance supplier designed to give access
   * to bean dependencies.
   */
  interface SupplierContext {

    /**
     * Return the bean instance that uniquely matches the given object type,
     * if any.
     *
     * @param requiredType type the bean must match; can be an interface or
     * superclass
     * @return an instance of the single bean matching the required type
     * @see BeanFactory#getBean(String)
     */
    <T> T bean(Class<T> requiredType) throws BeansException;

    /**
     * Return an instance, which may be shared or independent, of the
     * specified bean.
     *
     * @param name the name of the bean to retrieve
     * @param requiredType type the bean must match; can be an interface or superclass
     * @return an instance of the bean.
     * @see BeanFactory#getBean(String, Class)
     */
    <T> T bean(String name, Class<T> requiredType) throws BeansException;

    /**
     * Return a provider for the specified bean, allowing for lazy on-demand retrieval
     * of instances, including availability and uniqueness options.
     * <p>For matching a generic type, consider {@link #beanProvider(ResolvableType)}.
     *
     * @param requiredType type the bean must match; can be an interface or superclass
     * @return a corresponding provider handle
     * @see BeanFactory#getBeanProvider(Class)
     */
    <T> ObjectProvider<T> beanProvider(Class<T> requiredType);

    /**
     * Return a provider for the specified bean, allowing for lazy on-demand retrieval
     * of instances, including availability and uniqueness options. This variant allows
     * for specifying a generic type to match, similar to reflective injection points
     * with generic type declarations in method/constructor parameters.
     * <p>Note that collections of beans are not supported here, in contrast to reflective
     * injection points. For programmatically retrieving a list of beans matching a
     * specific type, specify the actual bean type as an argument here and subsequently
     * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
     * <p>Also, generics matching is strict here, as per the Java assignment rules.
     * For lenient fallback matching with unchecked semantics (similar to the 'unchecked'
     * Java compiler warning), consider calling {@link #beanProvider(Class)} with the
     * raw type as a second step if no full generic match is
     * {@link ObjectProvider#getIfAvailable() available} with this variant.
     *
     * @param requiredType type the bean must match; can be a generic type declaration
     * @return a corresponding provider handle
     * @see BeanFactory#getBeanProvider(ResolvableType)
     */
    <T> ObjectProvider<T> beanProvider(ResolvableType requiredType);
  }
}
