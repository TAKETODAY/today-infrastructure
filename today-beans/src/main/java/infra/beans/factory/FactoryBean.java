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

import infra.beans.factory.config.BeanDefinition;
import infra.core.AttributeAccessor;
import infra.lang.Nullable;

/**
 * Interface to be implemented by objects used within a {@link BeanFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryBean is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryBeans can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryBean}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link infra.aop.framework.ProxyFactoryBean} or the
 * {@link infra.jndi.JndiObjectFactoryBean}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryBean} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * Invocations of {@link #getObjectType()} and {@link #getObject()} may arrive early
 * in the bootstrap process, even ahead of any post-processor setup. If you need access
 * to other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p><b>The container is only responsible for managing the lifecycle of the FactoryBean
 * instance, not the lifecycle of the objects created by the FactoryBean.</b> Therefore,
 * a destroy method on an exposed bean object (such as {@link java.io.Closeable#close()})
 * will <i>not</i> be called automatically. Instead, a FactoryBean should implement
 * {@link DisposableBean} and delegate any such close call to the underlying object.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. Thus, there is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryBean itself (or the like).
 *
 * @param <T> the bean type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see BeanFactory
 * @see infra.aop.framework.ProxyFactoryBean
 * @see infra.jndi.JndiObjectFactoryBean
 * @since 2018-08-03 17:38
 */
public interface FactoryBean<T> {

  /**
   * The name of an attribute that can be
   * {@link AttributeAccessor#setAttribute set} on a
   * {@link BeanDefinition} so that
   * factory beans can signal their object type when it can't be deduced from
   * the factory bean class.
   *
   * @since 4.0
   */
  String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   * <p>As with a {@link BeanFactory}, this allows support for both the
   * Singleton and Prototype design patterns.
   * <p>If this FactoryBean is not fully initialized yet at the time of
   * the call (for example because it is involved in a circular reference),
   * throw a corresponding {@link FactoryBeanNotInitializedException}.
   * <p>FactoryBeans are allowed to return {@code null} objects. The bean
   * factory will consider this as a normal value to be used and will not throw
   * a {@code FactoryBeanNotInitializedException} in this case. However,
   * FactoryBean implementations are encouraged to throw
   * {@code FactoryBeanNotInitializedException} themselves, as appropriate.
   *
   * @return an instance of the bean (can be {@code null})
   * @throws Exception in case of creation errors
   * @see FactoryBeanNotInitializedException
   * @since 4.0
   */
  @Nullable
  T getObject() throws Exception;

  /**
   * Return the type of object that this FactoryBean creates,
   * or {@code null} if not known in advance.
   * <p>This allows one to check for specific types of beans without
   * instantiating objects, for example on autowiring.
   * <p>In the case of implementations that create a singleton object,
   * this method should try to avoid singleton creation as far as possible;
   * it should rather estimate the type in advance.
   * For prototypes, returning a meaningful type here is advisable too.
   * <p>This method can be called <i>before</i> this FactoryBean has
   * been fully initialized. It must not rely on state created during
   * initialization; of course, it can still use such state if available.
   * <p><b>NOTE:</b> Autowiring will simply ignore FactoryBeans that return
   * {@code null} here. Therefore, it is highly recommended to implement
   * this method properly, using the current state of the FactoryBean.
   *
   * @return the type of object that this FactoryBean creates,
   * or {@code null} if not known at the time of the call
   * @see BeanFactory#getBeansOfType
   * @since 2.1.2
   */
  @Nullable
  Class<?> getObjectType();

  /**
   * Is the object managed by this factory a singleton? That is,
   * will {@link #getObject()} always return the same object
   * (a reference that can be cached)?
   * <p><b>NOTE:</b> If a FactoryBean indicates that it holds a singleton
   * object, the object returned from {@code getObject()} might get cached
   * by the owning BeanFactory. Hence, do not return {@code true}
   * unless the FactoryBean always exposes the same reference.
   * <p>The singleton status of the FactoryBean itself will generally
   * be provided by the owning BeanFactory; usually, it has to be
   * defined as singleton there.
   * <p><b>NOTE:</b> This method returning {@code false} does not
   * necessarily indicate that returned objects are independent instances.
   * An implementation of the extended {@link SmartFactoryBean} interface
   * may explicitly indicate independent instances through its
   * {@link SmartFactoryBean#isPrototype()} method. Plain {@link FactoryBean}
   * implementations which do not implement this extended interface are
   * simply assumed to always return independent instances if the
   * {@code isSingleton()} implementation returns {@code false}.
   * <p>The default implementation returns {@code true}, since a
   * {@code FactoryBean} typically manages a singleton instance.
   *
   * @return whether the exposed object is a singleton
   * @see #getObject()
   * @see SmartFactoryBean#isPrototype()
   */
  default boolean isSingleton() {
    return true;
  }

}
