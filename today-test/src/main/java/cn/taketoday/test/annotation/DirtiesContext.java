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

package cn.taketoday.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;

/**
 * Test annotation which indicates that the
 * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
 * associated with a test is <em>dirty</em> and should therefore be closed
 * and removed from the context cache.
 *
 * <p>Use this annotation if a test has modified the context &mdash; for
 * example, by modifying the state of a singleton bean, modifying the state
 * of an embedded database, etc. Subsequent tests that request the same
 * context will be supplied a new context.
 *
 * <p>{@code @DirtiesContext} may be used as a class-level and method-level
 * annotation within the same class or class hierarchy. In such scenarios, the
 * {@code ApplicationContext} will be marked as <em>dirty</em> before or
 * after any such annotated method as well as before or after the current test
 * class, depending on the configured {@link #methodMode} and {@link #classMode}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * <h3>Supported Test Phases</h3>
 * <ul>
 * <li><strong>Before current test class</strong>: when declared at the class
 * level with class mode set to {@link ClassMode#BEFORE_CLASS BEFORE_CLASS}</li>
 * <li><strong>Before each test method in current test class</strong>: when
 * declared at the class level with class mode set to
 * {@link ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD}</li>
 * <li><strong>Before current test method</strong>: when declared at the
 * method level with method mode set to
 * {@link MethodMode#BEFORE_METHOD BEFORE_METHOD}</li>
 * <li><strong>After current test method</strong>: when declared at the
 * method level with method mode set to
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD}</li>
 * <li><strong>After each test method in current test class</strong>: when
 * declared at the class level with class mode set to
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}</li>
 * <li><strong>After current test class</strong>: when declared at the
 * class level with class mode set to
 * {@link ClassMode#AFTER_CLASS AFTER_CLASS}</li>
 * </ul>
 *
 * <p>{@code BEFORE_*} modes are supported by the
 * {@link DirtiesContextBeforeModesTestExecutionListener DirtiesContextBeforeModesTestExecutionListener};
 * {@code AFTER_*} modes are supported by the
 * {@link DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Rod Johnson
 * @see ContextConfiguration
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @since 2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DirtiesContext {

  /**
   * The <i>mode</i> to use when a test method is annotated with
   * {@code @DirtiesContext}.
   * <p>Defaults to {@link MethodMode#AFTER_METHOD AFTER_METHOD}.
   * <p>Setting the method mode on an annotated test class has no meaning.
   * For class-level control, use {@link #classMode} instead.
   *
   * @since 4.0
   */
  MethodMode methodMode() default MethodMode.AFTER_METHOD;

  /**
   * The <i>mode</i> to use when a test class is annotated with
   * {@code @DirtiesContext}.
   * <p>Defaults to {@link ClassMode#AFTER_CLASS AFTER_CLASS}.
   * <p>Setting the class mode on an annotated test method has no meaning.
   * For method-level control, use {@link #methodMode} instead.
   *
   * @since 3.0
   */
  ClassMode classMode() default ClassMode.AFTER_CLASS;

  /**
   * The context cache clearing <em>mode</em> to use when a context is
   * configured as part of a hierarchy via
   * {@link ContextHierarchy @ContextHierarchy}.
   * <p>Defaults to {@link HierarchyMode#EXHAUSTIVE EXHAUSTIVE}.
   *
   * @since 4.0
   */
  HierarchyMode hierarchyMode() default HierarchyMode.EXHAUSTIVE;

  /**
   * Defines <i>modes</i> which determine how {@code @DirtiesContext} is
   * interpreted when used to annotate a test method.
   *
   * @since 4.0
   */
  enum MethodMode {

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> before the corresponding test method.
     */
    BEFORE_METHOD,

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> after the corresponding test method.
     */
    AFTER_METHOD
	}

  /**
   * Defines <i>modes</i> which determine how {@code @DirtiesContext} is
   * interpreted when used to annotate a test class.
   *
   * @since 3.0
   */
  enum ClassMode {

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> before the test class.
     *
     * @since 4.0
     */
    BEFORE_CLASS,

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> before each test method in the class.
     *
     * @since 4.0
     */
    BEFORE_EACH_TEST_METHOD,

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> after each test method in the class.
     */
    AFTER_EACH_TEST_METHOD,

    /**
     * The associated {@code ApplicationContext} will be marked as
     * <em>dirty</em> after the test class.
     */
    AFTER_CLASS
	}

  /**
   * Defines <i>modes</i> which determine how the context cache is cleared
   * when {@code @DirtiesContext} is used in a test whose context is
   * configured as part of a hierarchy via
   * {@link ContextHierarchy @ContextHierarchy}.
   *
   * @since 4.0
   */
  enum HierarchyMode {

    /**
     * The context cache will be cleared using an <em>exhaustive</em> algorithm
     * that includes not only the {@linkplain HierarchyMode#CURRENT_LEVEL current level}
     * but also all other context hierarchies that share an ancestor context
     * common to the current test.
     *
     * <p>All {@code ApplicationContexts} that reside in a subhierarchy of
     * the common ancestor context will be removed from the context cache and
     * closed.
     */
    EXHAUSTIVE,

    /**
     * The {@code ApplicationContext} for the <em>current level</em> in the
     * context hierarchy and all contexts in subhierarchies of the current
     * level will be removed from the context cache and closed.
     *
     * <p>The <em>current level</em> refers to the {@code ApplicationContext}
     * at the lowest level in the context hierarchy that is visible from the
     * current test.
     */
    CURRENT_LEVEL
	}

}
