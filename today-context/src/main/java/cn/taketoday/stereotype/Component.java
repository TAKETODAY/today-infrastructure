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

import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates that the annotated class is a <em>component</em>.
 *
 * <p>Such classes are considered as candidates for auto-detection
 * when using annotation-based configuration and classpath scanning.
 *
 * <p>A component may optionally specify a logical component name via the
 * {@link #value value} attribute of this annotation.
 *
 * <p>Other class-level annotations may be considered as identifying
 * a component as well, typically a special kind of component &mdash;
 * for example, the {@link Repository @Repository} annotation or AspectJ's
 * {@link org.aspectj.lang.annotation.Aspect @Aspect} annotation. Note, however,
 * that the {@code @Aspect} annotation does not automatically make a class
 * eligible for classpath scanning.
 *
 * <p>Any annotation meta-annotated with {@code @Component} is considered a
 * <em>stereotype</em> annotation which makes the annotated class eligible for
 * classpath scanning. For example, {@link Service @Service},
 * {@link Controller @Controller}, and {@link Repository @Repository} are
 * stereotype annotations. Stereotype annotations may also support configuration
 * of a logical component name by overriding the {@link #value} attribute of this
 * annotation via {@link cn.taketoday.core.annotation.AliasFor @AliasFor}.
 *
 * <p>As of 4.0, support for configuring the name of a stereotype
 * component by convention (i.e., via a {@code String value()} attribute without
 * {@code @AliasFor}) is deprecated and will be removed in a future version of the
 * framework. Consequently, custom stereotype annotations must use {@code @AliasFor}
 * to declare an explicit alias for this annotation's {@link #value} attribute.
 * See the source code declaration of {@link Repository#value()} and
 * {@link cn.taketoday.web.annotation.ControllerAdvice#name()
 * ControllerAdvice.name()} for concrete examples.
 *
 * @author Mark Fisher
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Repository
 * @see Service
 * @see Controller
 * @since 2018-07-2 22:46:39
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
   * @see cn.taketoday.beans.factory.InitializingBean
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

  /**
   * Is this bean a candidate for getting autowired into some other bean based on
   * the plain type, without any further indications such as a qualifier match?
   * <p>Default is {@code true}; set this to {@code false} for restricted delegates
   * that are supposed to be injectable in certain areas but are not meant to get
   * in the way of beans of the same type in other places.
   * <p>This is a variation of {@link #autowireCandidate()} which does not disable
   * injection in general, just enforces an additional indication such as a qualifier.
   *
   * @see #autowireCandidate()
   * @since 4.0
   */
  boolean defaultCandidate() default true;

  /**
   * The bootstrap mode for this bean: default is the main pre-instantiation thread
   * for non-lazy singleton beans and the caller thread for prototype beans.
   * <p>Set {@link Bootstrap#BACKGROUND} to allow for instantiating this bean on a
   * background thread. For a non-lazy singleton, a background pre-instantiation
   * thread can be used then, while still enforcing the completion at the end of
   * {@link cn.taketoday.context.ConfigurableApplicationContext#refresh()}.
   * For a lazy singleton, a background pre-instantiation thread can be used as well
   * - with completion allowed at a later point, enforcing it when actually accessed.
   *
   * @see Lazy
   * @since 4.0
   */
  Bootstrap bootstrap() default Bootstrap.DEFAULT;

  /**
   * Local enumeration for the bootstrap mode.
   *
   * @see #bootstrap()
   * @since 4.0
   */
  enum Bootstrap {

    /**
     * Constant to indicate the main pre-instantiation thread for non-lazy
     * singleton beans and the caller thread for prototype beans.
     */
    DEFAULT,

    /**
     * Allow for instantiating a bean on a background thread.
     * <p>For a non-lazy singleton, a background pre-instantiation thread
     * can be used while still enforcing the completion on context refresh.
     * For a lazy singleton, a background pre-instantiation thread can be used
     * with completion allowed at a later point (when actually accessed).
     */
    BACKGROUND,
  }

}
