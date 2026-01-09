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

package infra.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.context.annotation.Lazy;
import infra.core.annotation.AliasFor;

/**
 * Indicates that an annotated class is a "Controller" (e.g. a web controller).
 *
 * <p>This annotation serves as a specialization of {@link Component @Component},
 * allowing for implementation classes to be autodetected through classpath scanning.
 * It is typically used in combination with annotated handler methods based on the
 * {@link infra.web.annotation.RequestMapping} annotation.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Component
 * @since 2018-08-23 11:16
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Controller {

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
   * @see infra.context.ConfigurableApplicationContext#refresh()
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
   * @see infra.context.ConfigurableApplicationContext#close()
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
   * The bootstrap mode for this bean: default is the main pre-instantiation thread
   * for non-lazy singleton beans and the caller thread for prototype beans.
   * <p>Set {@link Component.Bootstrap#BACKGROUND} to allow for instantiating this bean on a
   * background thread. For a non-lazy singleton, a background pre-instantiation
   * thread can be used then, while still enforcing the completion at the end of
   * {@link infra.context.ConfigurableApplicationContext#refresh()}.
   * For a lazy singleton, a background pre-instantiation thread can be used as well
   * - with completion allowed at a later point, enforcing it when actually accessed.
   *
   * @see Lazy
   * @since 5.0
   */
  @AliasFor(annotation = Component.class)
  Component.Bootstrap bootstrap() default Component.Bootstrap.DEFAULT;

}
