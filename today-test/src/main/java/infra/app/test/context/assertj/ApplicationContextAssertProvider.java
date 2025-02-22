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

package infra.app.test.context.assertj;

import org.assertj.core.api.AssertProvider;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Supplier;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * An {@link ApplicationContext} that additionally supports AssertJ style assertions. Can
 * be used to decorate an existing application context or an application context that
 * failed to start.
 * <p>
 * Assertions can be applied using the standard AssertJ {@code assertThat(...)} style (see
 * {@link ApplicationContextAssert} for a complete list). For example: <pre class="code">
 * assertThat(applicationContext).hasSingleBean(MyBean.class);
 * </pre>
 * <p>
 * If the original {@link ApplicationContext} is needed for any reason the
 * {@link #getSourceApplicationContext()} method can be used.
 * <p>
 * Any {@link ApplicationContext} method called on a context that has failed to start will
 * throw an {@link IllegalStateException}.
 *
 * @param <C> the application context type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AssertableApplicationContext
 * @see AssertableWebApplicationContext
 * @see AssertableReactiveWebApplicationContext
 * @see ApplicationContextAssert
 * @since 4.0
 */
public interface ApplicationContextAssertProvider<C extends ApplicationContext>
        extends ApplicationContext, AssertProvider<ApplicationContextAssert<C>>, Closeable {

  /**
   * Return an assert for AspectJ.
   *
   * @return an AspectJ assert
   * @deprecated to prevent accidental use. Prefer standard AssertJ
   * {@code assertThat(context)...} calls instead.
   */
  @Deprecated
  @Override
  ApplicationContextAssert<C> assertThat();

  /**
   * Return the original source {@link ApplicationContext}.
   *
   * @return the source application context
   * @throws IllegalStateException if the source context failed to start
   */
  C getSourceApplicationContext();

  /**
   * Return the original source {@link ApplicationContext}, casting it to the requested
   * type.
   *
   * @param <T> the context type
   * @param requiredType the required context type
   * @return the source application context
   * @throws IllegalStateException if the source context failed to start
   */
  <T extends C> T getSourceApplicationContext(Class<T> requiredType);

  /**
   * Return the failure that caused application context to fail or {@code null} if the
   * context started without issue.
   *
   * @return the startup failure or {@code null}
   */
  Throwable getStartupFailure();

  @Override
  void close();

  /**
   * Factory method to create a new {@link ApplicationContextAssertProvider} instance.
   *
   * @param <T> the assert provider type
   * @param <C> the context type
   * @param type the type of {@link ApplicationContextAssertProvider} required (must be
   * an interface)
   * @param contextType the type of {@link ApplicationContext} being managed (must be an
   * interface)
   * @param contextSupplier a supplier that will either return a fully configured
   * {@link ApplicationContext} or throw an exception if the context fails to start.
   * @return a {@link ApplicationContextAssertProvider} instance
   */
  @SuppressWarnings("unchecked")
  static <T extends ApplicationContextAssertProvider<C>, C extends ApplicationContext> T get(Class<T> type,
          Class<? extends C> contextType, Supplier<? extends C> contextSupplier) {
    return get(type, contextType, contextSupplier, new Class<?>[0]);
  }

  /**
   * Factory method to create a new {@link ApplicationContextAssertProvider} instance.
   *
   * @param <T> the assert provider type
   * @param <C> the context type
   * @param type the type of {@link ApplicationContextAssertProvider} required (must be
   * an interface)
   * @param contextType the type of {@link ApplicationContext} being managed (must be an
   * interface)
   * @param contextSupplier a supplier that will either return a fully configured
   * {@link ApplicationContext} or throw an exception if the context fails to start.
   * @param additionalContextInterfaces and additional context interfaces to add to the
   * proxy
   * @return a {@link ApplicationContextAssertProvider} instance
   */
  @SuppressWarnings("unchecked")
  static <T extends ApplicationContextAssertProvider<C>, C extends ApplicationContext> T get(Class<T> type,
          Class<? extends C> contextType, Supplier<? extends C> contextSupplier,
          Class<?>... additionalContextInterfaces) {
    Assert.notNull(type, "Type is required");
    Assert.isTrue(type.isInterface(), "Type must be an interface");
    Assert.notNull(contextType, "ContextType is required");
    Assert.isTrue(contextType.isInterface(), "ContextType must be an interface");
    Class<?>[] interfaces = merge(new Class<?>[] { type, contextType }, additionalContextInterfaces);
    return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces,
            new AssertProviderApplicationContextInvocationHandler(contextType, contextSupplier));
  }

  private static Class<?>[] merge(Class<?>[] classes, Class<?>[] additional) {
    if (ObjectUtils.isEmpty(additional)) {
      return classes;
    }
    Class<?>[] result = Arrays.copyOf(classes, classes.length + additional.length);
    System.arraycopy(additional, 0, result, classes.length, additional.length);
    return result;
  }

}
