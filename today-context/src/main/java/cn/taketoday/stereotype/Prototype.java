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

package cn.taketoday.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates that an annotated class is a prototype Bean.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-07-2 20:44:15
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Prototype {

  /**
   * Alias for {@link Component#value}.
   */
  @AliasFor(annotation = Component.class)
  String[] value() default {};

  /**
   * The optional name of a method to call on the bean instance during
   * initialization. Not commonly used, given that the method may be called
   * programmatically directly within the body of a Bean-annotated method.
   * <p>
   * The default value is {@code ""}, indicating no init method to be called.
   *
   * @see InitializingBean
   * @see cn.taketoday.context.ConfigurableApplicationContext#refresh()
   */
  @AliasFor(annotation = Component.class)
  String[] initMethods() default {};

  /**
   * The optional names of a method to call on the bean instance upon closing the
   * application context, for example a {@code close()} method on a JDBC
   * {@code DataSource} implementation, or a Hibernate {@code SessionFactory}
   * object. The method must have no arguments but may throw any exception.
   * <p>
   * Note: Only invoked on beans whose lifecycle is under the full control of the
   * factory, which is always the case for singletons but not guaranteed for any
   * other scope.
   *
   * @see DisposableBean
   * @see cn.taketoday.context.ConfigurableApplicationContext#close()
   */
  @AliasFor(annotation = Component.class)
  String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

  /**
   * Is this bean a candidate for getting autowired into some other bean?
   * <p>Default is {@code true}; set this to {@code false} for internal delegates
   * that are not meant to get in the way of beans of the same type in other places.
   *
   * @since 4.0
   */
  @AliasFor(annotation = Component.class)
  boolean autowireCandidate() default true;

  /**
   * Alias for {@link Scope#proxyMode()}
   *
   * @since 4.0
   */
  @AliasFor(annotation = Scope.class, attribute = "proxyMode")
  ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

  /**
   * The bootstrap mode for this bean: default is the main pre-instantiation thread
   * for non-lazy singleton beans and the caller thread for prototype beans.
   * <p>Set {@link Component.Bootstrap#BACKGROUND} to allow for instantiating this bean on a
   * background thread. For a non-lazy singleton, a background pre-instantiation
   * thread can be used then, while still enforcing the completion at the end of
   * {@link cn.taketoday.context.ConfigurableApplicationContext#refresh()}.
   * For a lazy singleton, a background pre-instantiation thread can be used as well
   * - with completion allowed at a later point, enforcing it when actually accessed.
   *
   * @see Lazy
   * @since 5.0
   */
  @AliasFor(annotation = Component.class)
  Component.Bootstrap bootstrap() default Component.Bootstrap.DEFAULT;

}
