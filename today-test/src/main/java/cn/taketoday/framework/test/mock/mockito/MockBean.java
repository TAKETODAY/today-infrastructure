/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import org.mockito.Answers;
import org.mockito.MockSettings;

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
 * Annotation that can be used to add mocks to a Infra {@link ApplicationContext}. Can be
 * used as a class level annotation or on fields in either {@code @Configuration} classes,
 * or test classes that are {@link RunWith @RunWith} the {@link InfraRunner}.
 * <p>
 * Mocks can be registered by type or by {@link #name() bean name}. When registered by
 * type, any existing single bean of a matching type (including subclasses) in the context
 * will be replaced by the mock. When registered by name, an existing bean can be
 * specifically targeted for replacement by a mock. In either case, if no existing bean is
 * defined a new one will be added. Dependencies that are known to the application context
 * but are not beans (such as those
 * {@link cn.taketoday.beans.factory.config.ConfigurableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found and a mocked bean will be added to the context
 * alongside the existing dependency.
 * <p>
 * When {@code @MockBean} is used on a field, as well as being registered in the
 * application context, the mock will also be injected into the field. Typical usage might
 * be: <pre class="code">
 * &#064;RunWith(Runner.class)
 * public class ExampleTests {
 *
 *     &#064;MockBean
 *     private ExampleService service;
 *
 *     &#064;Autowired
 *     private UserOfService userOfService;
 *
 *     &#064;Test
 *     public void testUserOfService() {
 *         given(this.service.greet()).willReturn("Hello");
 *         String actual = this.userOfService.makeUse();
 *         assertEquals("Was: Hello", actual);
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
 *     &#064;MockBean
 *     &#064;Qualifier("example")
 *     private ExampleService service;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * This annotation is {@code @Repeatable} and may be specified multiple times when working
 * with Java 8 or contained within an {@link MockBeans @MockBeans} annotation.
 *
 * @author Phillip Webb
 * @see MockitoPostProcessor
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MockBeans.class)
public @interface MockBean {

  /**
   * The name of the bean to register or replace. If not specified the name will either
   * be generated or, if the mock replaces an existing bean, the existing name will be
   * used.
   *
   * @return the name of the bean
   */
  String name() default "";

  /**
   * The classes to mock. This is an alias of {@link #classes()} which can be used for
   * brevity if no other attributes are defined. See {@link #classes()} for details.
   *
   * @return the classes to mock
   */
  @AliasFor("classes")
  Class<?>[] value() default {};

  /**
   * The classes to mock. Each class specified here will result in a mock being created
   * and registered with the application context. Classes can be omitted when the
   * annotation is used on a field.
   * <p>
   * When {@code @MockBean} also defines a {@code name} this attribute can only contain
   * a single value.
   * <p>
   * If this is the only specified attribute consider using the {@code value} alias
   * instead.
   *
   * @return the classes to mock
   */
  @AliasFor("value")
  Class<?>[] classes() default {};

  /**
   * Any extra interfaces that should also be declared on the mock. See
   * {@link MockSettings#extraInterfaces(Class...)} for details.
   *
   * @return any extra interfaces
   */
  Class<?>[] extraInterfaces() default {};

  /**
   * The {@link Answers} type to use on the mock.
   *
   * @return the answer type
   */
  Answers answer() default Answers.RETURNS_DEFAULTS;

  /**
   * If the generated mock is serializable. See {@link MockSettings#serializable()} for
   * details.
   *
   * @return if the mock is serializable
   */
  boolean serializable() default false;

  /**
   * The reset mode to apply to the mock bean. The default is {@link MockReset#AFTER}
   * meaning that mocks are automatically reset after each test method is invoked.
   *
   * @return the reset mode
   */
  MockReset reset() default MockReset.AFTER;

}
