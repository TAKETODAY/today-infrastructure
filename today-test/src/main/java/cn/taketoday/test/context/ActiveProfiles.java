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

package cn.taketoday.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * {@code ActiveProfiles} is a class-level annotation that is used to declare
 * which <em>active bean definition profiles</em> should be used when loading
 * an {@link cn.taketoday.context.ApplicationContext ApplicationContext}
 * for test classes.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>As of Spring Framework 5.3, this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @see SmartContextLoader
 * @see MergedContextConfiguration
 * @see ContextConfiguration
 * @see ActiveProfilesResolver
 * @see cn.taketoday.context.ApplicationContext
 * @see cn.taketoday.context.annotation.Profile
 * @since 3.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ActiveProfiles {

  /**
   * Alias for {@link #profiles}.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #profiles}, but it may be used <em>instead</em> of {@link #profiles}.
   */
  @AliasFor("profiles")
  String[] value() default {};

  /**
   * The bean definition profiles to activate.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value}, but it may be used <em>instead</em> of {@link #value}.
   */
  @AliasFor("value")
  String[] profiles() default {};

  /**
   * The type of {@link ActiveProfilesResolver} to use for resolving the active
   * bean definition profiles programmatically.
   *
   * @see ActiveProfilesResolver
   * @since 4.0
   */
  Class<? extends ActiveProfilesResolver> resolver() default ActiveProfilesResolver.class;

  /**
   * Whether or not bean definition profiles from superclasses should be
   * <em>inherited</em>.
   * <p>The default value is {@code true}, which means that a test
   * class will <em>inherit</em> bean definition profiles defined by a
   * test superclass. Specifically, the bean definition profiles for a test
   * class will be appended to the list of bean definition profiles
   * defined by a test superclass. Thus, subclasses have the option of
   * <em>extending</em> the list of bean definition profiles.
   * <p>If {@code inheritProfiles} is set to {@code false}, the bean
   * definition profiles for the test class will <em>shadow</em> and
   * effectively replace any bean definition profiles defined by a superclass.
   * <p>In the following example, the {@code ApplicationContext} for
   * {@code BaseTest} will be loaded using only the &quot;base&quot;
   * bean definition profile; beans defined in the &quot;extended&quot; profile
   * will therefore not be loaded. In contrast, the {@code ApplicationContext}
   * for {@code ExtendedTest} will be loaded using the &quot;base&quot;
   * <strong>and</strong> &quot;extended&quot; bean definition profiles.
   * <pre class="code">
   * &#064;ActiveProfiles(&quot;base&quot;)
   * &#064;ContextConfiguration
   * public class BaseTest {
   *     // ...
   * }
   *
   * &#064;ActiveProfiles(&quot;extended&quot;)
   * &#064;ContextConfiguration
   * public class ExtendedTest extends BaseTest {
   *     // ...
   * }
   * </pre>
   * <p>Note: {@code @ActiveProfiles} can be used when loading an
   * {@code ApplicationContext} from path-based resource locations or
   * annotated classes.
   *
   * @see ContextConfiguration#locations
   * @see ContextConfiguration#classes
   * @see ContextConfiguration#inheritLocations
   */
  boolean inheritProfiles() default true;

}
