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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.test.context.junit4.InfraRunner;

/**
 * Annotation that can be used to apply Mockito spies to a Spring
 * {@link ApplicationContext}. Can be used as a class level annotation or on fields in
 * either {@code @Configuration} classes, or test classes that are
 * {@link RunWith @RunWith} the {@link InfraRunner}.
 * <p>
 * Spies can be applied by type or by {@link #name() bean name}. All beans in the context
 * of a matching type (including subclasses) will be wrapped with the spy. If no existing
 * bean is defined a new one will be added. Dependencies that are known to the application
 * context but are not beans (such as those
 * {@link cn.taketoday.beans.factory.config.ConfigurableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found and a spied bean will be added to the context
 * alongside the existing dependency.
 * <p>
 * When {@code @SpyBean} is used on a field, as well as being registered in the
 * application context, the spy will also be injected into the field. Typical usage might
 * be: <pre class="code">
 * &#064;RunWith(Runner.class)
 * public class ExampleTests {
 *
 *     &#064;SpyBean
 *     private ExampleService service;
 *
 *     &#064;Autowired
 *     private UserOfService userOfService;
 *
 *     &#064;Test
 *     public void testUserOfService() {
 *         String actual = this.userOfService.makeUse();
 *         assertEquals("Was: Hello", actual);
 *         verify(this.service).greet();
 *     }
 *
 *     &#064;Configuration
 *     &#064;Import(UserOfService.class) // A &#064;Component injected with ExampleService
 *     static class Config {
 *     }
 *
 *
 * }
 * </pre> If there is more than one bean of the requested type, qualifier metadata must be
 * specified at field level: <pre class="code">
 * &#064;RunWith(Runner.class)
 * public class ExampleTests {
 *
 *     &#064;SpyBean
 *     &#064;Qualifier("example")
 *     private ExampleService service;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * This annotation is {@code @Repeatable} and may be specified multiple times when working
 * with Java 8 or contained within a {@link SpyBeans @SpyBeans} annotation.
 *
 * @author Phillip Webb
 * @see MockitoPostProcessor
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(SpyBeans.class)
public @interface SpyBean {

  /**
   * The name of the bean to spy. If not specified the name will either be generated or,
   * if the spy is for an existing bean, the existing name will be used.
   *
   * @return the name of the bean
   */
  String name() default "";

  /**
   * The classes to spy. This is an alias of {@link #classes()} which can be used for
   * brevity if no other attributes are defined. See {@link #classes()} for details.
   *
   * @return the classes to spy
   */
  @AliasFor("classes")
  Class<?>[] value() default {};

  /**
   * The classes to spy. Each class specified here will result in a spy being applied.
   * Classes can be omitted when the annotation is used on a field.
   * <p>
   * When {@code @SpyBean} also defines a {@code name} this attribute can only contain a
   * single value.
   * <p>
   * If this is the only specified attribute consider using the {@code value} alias
   * instead.
   *
   * @return the classes to spy
   */
  @AliasFor("value")
  Class<?>[] classes() default {};

  /**
   * The reset mode to apply to the spied bean. The default is {@link MockReset#AFTER}
   * meaning that spies are automatically reset after each test method is invoked.
   *
   * @return the reset mode
   */
  MockReset reset() default MockReset.AFTER;

  /**
   * Indicates that Mockito methods such as {@link Mockito#verify(Object) verify(mock)}
   * should use the {@code target} of AOP advised beans, rather than the proxy itself.
   * If set to {@code false} you may need to use the result of
   * {@link cn.taketoday.test.util.AopTestUtils#getUltimateTargetObject(Object)
   * AopTestUtils.getUltimateTargetObject(...)} when calling Mockito methods.
   *
   * @return {@code true} if the target of AOP advised beans is used or {@code false} if
   * the proxy is used directly
   */
  boolean proxyTargetAware() default true;

}
