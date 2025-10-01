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

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their is singleton implementation returning {@code false}
 * does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their is singleton implementation returns {@code false};
 * the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @param <T> the bean type
 * @author Juergen Hoeller
 * @author TODAY 2021/3/9 14:06
 * @since 3.0
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

  /**
   * Is the object managed by this factory a prototype? That is,
   * will {@link #getObject()} always return an independent instance?
   * <p>The prototype status of the FactoryBean itself will generally
   * be provided by the owning {@link BeanFactory}; usually, it has to be
   * defined as singleton there.
   * <p>This method is supposed to strictly check for independent instances;
   * it should not return {@code true} for scoped objects or other
   * kinds of non-singleton, non-independent objects. For this reason,
   * this is not simply the inverted form of {@link #isSingleton()}.
   * <p>The default implementation returns {@code false}.
   *
   * @return whether the exposed object is a prototype
   * @see #getObject()
   * @see #isSingleton()
   * @since 4.0
   */
  default boolean isPrototype() {
    return false;
  }

  /**
   * Does this FactoryBean expect eager initialization, that is,
   * eagerly initialize itself as well as expect eager initialization
   * of its singleton object (if any)?
   * <p>A standard FactoryBean is not expected to initialize eagerly:
   * Its {@link #getObject()} will only be called for actual access, even
   * in case of a singleton object. Returning {@code true} from this
   * method suggests that {@link #getObject()} should be called eagerly,
   * also applying post-processors eagerly. This may make sense in case
   * of a is singleton object, in particular if post-processors expect
   * to be applied on startup.
   * <p>The default implementation returns {@code false}.
   *
   * @return whether eager initialization applies
   * @see ConfigurableBeanFactory#preInstantiateSingletons()
   */
  default boolean isEagerInit() {
    return false;
  }

  /**
   * Return an instance of the given type, if supported by this factory.
   * <p>By default, this supports the primary type exposed by the factory, as
   * indicated by {@link #getObjectType()} and returned by {@link #getObject()}.
   * Specific factories may support additional types for dependency injection.
   *
   * @param type the requested type
   * @return a corresponding instance managed by this factory,
   * or {@code null} if none available
   * @throws Exception in case of creation errors
   * @see #getObject()
   * @see #supportsType(Class)
   * @since 5.0
   */
  @Nullable
  @SuppressWarnings("unchecked")
  default <S> S getObject(Class<S> type) throws Exception {
    Class<?> objectType = getObjectType();
    return objectType != null && type.isAssignableFrom(objectType) ? (S) getObject() : null;
  }

  /**
   * Determine whether this factory supports the requested type.
   * <p>By default, this supports the primary type exposed by the factory, as
   * indicated by {@link #getObjectType()}. Specific factories may support
   * additional types for dependency injection.
   *
   * @param type the requested type
   * @return {@code true} if {@link #getObject(Class)} is able to
   * return a corresponding instance, {@code false} otherwise
   * @see #getObject(Class)
   * @see #getObjectType()
   * @since 5.0
   */
  default boolean supportsType(Class<?> type) {
    Class<?> objectType = getObjectType();
    return objectType != null && type.isAssignableFrom(objectType);
  }

}
