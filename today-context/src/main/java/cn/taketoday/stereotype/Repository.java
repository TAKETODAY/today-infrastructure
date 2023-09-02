/*
 * Copyright 2017 - 2023 the original author or authors.
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
package cn.taketoday.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates that an annotated class is a "Repository", originally defined by
 * Domain-Driven Design (Evans, 2003) as "a mechanism for encapsulating storage,
 * retrieval, and search behavior which emulates a collection of objects".
 *
 * <p>Teams implementing traditional Jakarta EE patterns such as "Data Access Object"
 * may also apply this stereotype to DAO classes, though care should be taken to
 * understand the distinction between Data Access Object and DDD-style repositories
 * before doing so. This annotation is a general-purpose stereotype and individual teams
 * may narrow their semantics and use as appropriate.
 *
 * <p>A class thus annotated is eligible for Infra
 * {@link cn.taketoday.dao.DataAccessException DataAccessException} translation
 * when used in conjunction with a {@link
 * cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * PersistenceExceptionTranslationPostProcessor}. The annotated class is also clarified as
 * to its role in the overall application architecture for the purpose of tooling,
 * aspects, etc.
 *
 * <p>This annotation also serves as a specialization of {@link Component @Component},
 * allowing for implementation classes to be autodetected through classpath scanning.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Component
 * @see Service
 * @see cn.taketoday.dao.DataAccessException
 * @see cn.taketoday.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * @since 2018-07-02 20:45:00
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Repository {

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

}
