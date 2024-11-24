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

package infra.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.Ordered;
import infra.core.annotation.AliasFor;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.Order;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.jdbc.SqlScriptsTestExecutionListener;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.transaction.TransactionalTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;

/**
 * {@code TestExecutionListeners} defines class-level metadata for configuring
 * which {@link TestExecutionListener TestExecutionListeners} should be
 * registered with a {@link TestContextManager}.
 *
 * <p>{@code @TestExecutionListeners} is used to register listeners for a
 * particular test class, its subclasses, and its nested classes. If you wish to
 * register a listener globally, you should register it via the automatic discovery
 * mechanism described in {@link TestExecutionListener}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>. this annotation will
 * be inherited from an enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * <h3>Switching to default {@code TestExecutionListener} implementations</h3>
 *
 * <p>If you extend a class that is annotated with {@code @TestExecutionListeners}
 * and you need to switch to using the <em>default</em> set of listeners, you
 * can annotate your class with the following.
 *
 * <pre class="code">
 * // Switch to default listeners
 * &#064;TestExecutionListeners(listeners = {}, inheritListeners = false, mergeMode = MERGE_WITH_DEFAULTS)
 * class MyTests extends BaseTests {
 *     // ...
 * }
 * </pre>
 *
 * @author Sam Brannen
 * @see TestExecutionListener
 * @see TestContextManager
 * @see ContextConfiguration
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestExecutionListeners {

  /**
   * Alias for {@link #listeners}.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #listeners}, but it may be used instead of {@link #listeners}.
   */
  @AliasFor("listeners")
  Class<? extends TestExecutionListener>[] value() default {};

  /**
   * The {@link TestExecutionListener TestExecutionListeners} to register with
   * the {@link TestContextManager}.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value}, but it may be used instead of {@link #value}.
   *
   * @see MockTestExecutionListener
   * @see DirtiesContextBeforeModesTestExecutionListener
   * @see ApplicationEventsTestExecutionListener
   * @see DependencyInjectionTestExecutionListener
   * @see DirtiesContextTestExecutionListener
   * @see TransactionalTestExecutionListener
   * @see SqlScriptsTestExecutionListener
   * @see EventPublishingTestExecutionListener
   */
  @AliasFor("value")
  Class<? extends TestExecutionListener>[] listeners() default {};

  /**
   * Whether or not {@link #listeners TestExecutionListeners} from superclasses
   * should be <em>inherited</em>.
   * <p>The default value is {@code true}, which means that an annotated
   * class will <em>inherit</em> the listeners defined by an annotated
   * superclass. Specifically, the listeners for an annotated class will be
   * appended to the list of listeners defined by an annotated superclass.
   * Thus, subclasses have the option of <em>extending</em> the list of
   * listeners. In the following example, {@code AbstractBaseTest} will
   * be configured with {@code DependencyInjectionTestExecutionListener}
   * and {@code DirtiesContextTestExecutionListener}; whereas,
   * {@code TransactionalTest} will be configured with
   * {@code DependencyInjectionTestExecutionListener},
   * {@code DirtiesContextTestExecutionListener}, <strong>and</strong>
   * {@code TransactionalTestExecutionListener}, in that order.
   * <pre class="code">
   * &#064;TestExecutionListeners({
   *     DependencyInjectionTestExecutionListener.class,
   *     DirtiesContextTestExecutionListener.class
   * })
   * public abstract class AbstractBaseTest {
   * 	 // ...
   * }
   *
   * &#064;TestExecutionListeners(TransactionalTestExecutionListener.class)
   * public class TransactionalTest extends AbstractBaseTest {
   * 	 // ...
   * }</pre>
   * <p>If {@code inheritListeners} is set to {@code false}, the listeners for
   * the annotated class will <em>shadow</em> and effectively replace any
   * listeners defined by a superclass.
   */
  boolean inheritListeners() default true;

  /**
   * The <em>merge mode</em> to use when {@code @TestExecutionListeners} is
   * declared on a class that does <strong>not</strong> inherit listeners
   * from a superclass.
   * <p>Can be set to {@link MergeMode#MERGE_WITH_DEFAULTS MERGE_WITH_DEFAULTS}
   * to have locally declared listeners <em>merged</em> with the default
   * listeners.
   * <p>The mode is ignored if listeners are inherited from a superclass.
   * <p>Defaults to {@link MergeMode#REPLACE_DEFAULTS REPLACE_DEFAULTS}
   * for backwards compatibility.
   *
   * @see MergeMode
   */
  MergeMode mergeMode() default MergeMode.REPLACE_DEFAULTS;

  /**
   * Enumeration of <em>modes</em> that dictate whether or not explicitly
   * declared listeners are merged with the default listeners when
   * {@code @TestExecutionListeners} is declared on a class that does
   * <strong>not</strong> inherit listeners from a superclass.
   */
  enum MergeMode {

    /**
     * Indicates that locally declared listeners should replace the default
     * listeners.
     */
    REPLACE_DEFAULTS,

    /**
     * Indicates that locally declared listeners should be merged with the
     * default listeners.
     * <p>The merging algorithm ensures that duplicates are removed from
     * the list and that the resulting set of merged listeners is sorted
     * according to the semantics of
     * {@link AnnotationAwareOrderComparator
     * AnnotationAwareOrderComparator}. If a listener implements
     * {@link Ordered Ordered} or is annotated
     * with {@link Order @Order} it can
     * influence the position in which it is merged with the defaults; otherwise,
     * locally declared listeners will simply be appended to the list of default
     * listeners when merged.
     */
    MERGE_WITH_DEFAULTS
  }

}
