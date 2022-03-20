/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.beans.factory.config;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface used by a {@link ConfigurableBeanFactory},
 * representing a target scope to hold bean instances in.
 * This allows for extending the BeanFactory's standard scopes
 * {@link Scope#SINGLETON "singleton"} and {@link Scope#PROTOTYPE "prototype"}
 * with custom further scopes, registered for a
 * {@link ConfigurableBeanFactory#registerScope(String, Scope) specific key}.
 *
 * <p>{@link cn.taketoday.context.ApplicationContext} implementations
 * such as a {@link cn.taketoday.web.WebApplicationContext}
 * may register additional standard scopes specific to their environment,
 * e.g. {@link cn.taketoday.web.WebApplicationContext#SCOPE_REQUEST "request"}
 * and {@link cn.taketoday.web.WebApplicationContext#SCOPE_SESSION "session"},
 * based on this Scope SPI.
 *
 * <p>Even if its primary use is for extended scopes in a web environment,
 * this SPI is completely generic: It provides the ability to get and put
 * objects from any underlying storage mechanism, such as an HTTP session
 * or a custom conversation mechanism. The name passed into this class's
 * {@code get} and {@code remove} methods will identify the
 * target object in the current scope.
 *
 * <p>{@code Scope} implementations are expected to be thread-safe.
 * One {@code Scope} instance can be used with multiple bean factories
 * at the same time, if desired (unless it explicitly wants to be aware of
 * the containing BeanFactory), with any number of threads accessing
 * the {@code Scope} concurrently from any number of factories.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author TODAY 2018-07-02 22:38:57
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @since 3.0
 */
public interface Scope {
  /**
   * @since 2.1.7
   */
  String SINGLETON = "singleton";

  /**
   * @since 2.1.7
   */
  String PROTOTYPE = "prototype";

  /**
   * Return the object with the given {@link BeanDefinition} from the underlying
   * scope, {@link Supplier#get()} creating it} if
   * not found in the underlying storage mechanism.
   * <p>
   * This is the central operation of a Scope, and the only operation that is
   * absolutely required.
   *
   * @param beanName the name of the object to retrieve
   * @param objectFactory the {@link Supplier} to use to create the scoped object
   * if it is not present in the underlying storage mechanism
   * @return the desired object (never {@code null})
   * @throws IllegalStateException if the underlying scope is not currently active
   */
  Object get(String beanName, Supplier<?> objectFactory);

  /**
   * Remove the object with the given {@code name} from the underlying scope.
   * <p>Returns {@code null} if no object was found; otherwise
   * returns the removed {@code Object}.
   * <p>Note that an implementation should also remove a registered destruction
   * callback for the specified object, if any. It does, however, <i>not</i>
   * need to <i>execute</i> a registered destruction callback in this case,
   * since the object will be destroyed by the caller (if appropriate).
   * <p><b>Note: This is an optional operation.</b> Implementations may throw
   * {@link UnsupportedOperationException} if they do not support explicitly
   * removing an object.
   *
   * @param name the name of the object to remove
   * @return the removed object, or {@code null} if no object was present
   * @throws IllegalStateException if the underlying scope is not currently active
   * @see #registerDestructionCallback
   */
  @Nullable
  Object remove(String name);

  /**
   * Register a callback to be executed on destruction of the specified
   * object in the scope (or at destruction of the entire scope, if the
   * scope does not destroy individual objects but rather only terminates
   * in its entirety).
   * <p><b>Note: This is an optional operation.</b> This method will only
   * be called for scoped beans with actual destruction configuration
   * (DisposableBean, destroy-method, DestructionAwareBeanPostProcessor).
   * Implementations should do their best to execute a given callback
   * at the appropriate time. If such a callback is not supported by the
   * underlying runtime environment at all, the callback <i>must be
   * ignored and a corresponding warning should be logged</i>.
   * <p>Note that 'destruction' refers to automatic destruction of
   * the object as part of the scope's own lifecycle, not to the individual
   * scoped object having been explicitly removed by the application.
   * If a scoped object gets removed via this facade's {@link #remove(String)}
   * method, any registered destruction callback should be removed as well,
   * assuming that the removed object will be reused or manually destroyed.
   *
   * @param name the name of the object to execute the destruction callback for
   * @param callback the destruction callback to be executed.
   * Note that the passed-in Runnable will never throw an exception,
   * so it can safely be executed without an enclosing try-catch block.
   * Furthermore, the Runnable will usually be serializable, provided
   * that its target object is serializable as well.
   * @throws IllegalStateException if the underlying scope is not currently active
   * @see DisposableBean
   * @see DestructionAwareBeanPostProcessor
   */
  void registerDestructionCallback(String name, Runnable callback);

  /**
   * Resolve the contextual object for the given key, if any.
   * E.g. the HttpServletRequest object for key "request".
   *
   * @param key the contextual key
   * @return the corresponding object, or {@code null} if none found
   * @throws IllegalStateException if the underlying scope is not currently active
   * @since 4.0
   */
  @Nullable
  Object resolveContextualObject(String key);

  /**
   * Return the <em>conversation ID</em> for the current underlying scope, if any.
   * <p>The exact meaning of the conversation ID depends on the underlying
   * storage mechanism. In the case of session-scoped objects, the
   * conversation ID would typically be equal to (or derived from) the
   * {@link jakarta.servlet.http.HttpSession#getId() session ID}; in the
   * case of a custom conversation that sits within the overall session,
   * the specific ID for the current conversation would be appropriate.
   * <p><b>Note: This is an optional operation.</b> It is perfectly valid to
   * return {@code null} in an implementation of this method if the
   * underlying storage mechanism has no obvious candidate for such an ID.
   *
   * @return the conversation ID, or {@code null} if there is no
   * conversation ID for the current scope
   * @throws IllegalStateException if the underlying scope is not currently active
   * @since 4.0
   */
  @Nullable
  default String getConversationId() {
    return null;
  }
}
