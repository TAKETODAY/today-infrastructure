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
package cn.taketoday.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.core.annotation.AliasFor;

/**
 * This annotation indicates that an annotated element is a bean component in
 * your application
 *
 * @author TODAY 2018-07-2 22:46:39
 */
@Indexed
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Component {

  /**
   * Alias for {@link #name}.
   * <p>Intended to be used when no other attributes are needed, for example:
   * {@code @Component("customBeanName")}.
   *
   * @see #name
   */
  @AliasFor("name")
  String[] value() default {};

  /**
   * The name of this bean, or if several names, a primary bean name plus aliases.
   * <p>If left unspecified, the name of the bean is the name of the annotated method.
   * If specified, the method name is ignored.
   * <p>The bean name and aliases may also be configured via the {@link #value}
   * attribute if no other attributes are declared.
   *
   * @see #value
   */
  @AliasFor("value")
  String[] name() default {};

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
  String[] initMethods() default {};

  /**
   * The optional name of a method to call on the bean instance upon closing the
   * application context, for example a {@code close()} method on a JDBC
   * {@code DataSource} implementation, or a Hibernate {@code SessionFactory} object.
   * The method must have no arguments but may throw any exception.
   * <p>As a convenience to the user, the container will attempt to infer a destroy
   * method against an object returned from the {@code @Bean} method. For example, given
   * an {@code @Bean} method returning an Apache Commons DBCP {@code BasicDataSource},
   * the container will notice the {@code close()} method available on that object and
   * automatically register it as the {@code destroyMethod}. This 'destroy method
   * inference' is currently limited to detecting only public, no-arg methods named
   * 'close' or 'shutdown'. The method may be declared at any level of the inheritance
   * hierarchy and will be detected regardless of the return type of the {@code @Bean}
   * method (i.e., detection occurs reflectively against the bean instance itself at
   * creation time).
   * <p>To disable destroy method inference for a particular {@code @Bean}, specify an
   * empty string as the value, e.g. {@code @Bean(destroyMethod="")}. Note that the
   * {@link cn.taketoday.beans.factory.DisposableBean} callback interface will
   * nevertheless get detected and the corresponding destroy method invoked: In other
   * words, {@code destroyMethod=""} only affects custom close/shutdown methods and
   * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} declared close methods.
   * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
   * factory, which is always the case for singletons but not guaranteed for any
   * other scope.
   *
   * @see cn.taketoday.beans.factory.DisposableBean
   * @see cn.taketoday.context.ConfigurableApplicationContext#close()
   */
  String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

  /**
   * Is this bean a candidate for getting autowired into some other bean?
   * <p>Default is {@code true}; set this to {@code false} for internal delegates
   * that are not meant to get in the way of beans of the same type in other places.
   *
   * @since 4.0
   */
  boolean autowireCandidate() default true;

}
