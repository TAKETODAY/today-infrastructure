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

package infra.test.context.bean.override.mockito;

import org.mockito.Answers;
import org.mockito.MockSettings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.test.context.bean.override.BeanOverride;

/**
 * {@code @MockitoBean} is an annotation that can be used in test classes to
 * override a bean in the test's
 * {@link infra.context.ApplicationContext ApplicationContext}
 * with a Mockito mock.
 *
 * <p>{@code @MockitoBean} can be applied in the following ways.
 * <ul>
 * <li>On a non-static field in a test class or any of its superclasses.</li>
 * <li>On a non-static field in an enclosing class for a {@code @Nested} test class
 * or in any class in the type hierarchy or enclosing class hierarchy above the
 * {@code @Nested} test class.</li>
 * <li>At the type level on a test class or any superclass or implemented interface
 * in the type hierarchy above the test class.</li>
 * <li>At the type level on an enclosing class for a {@code @Nested} test class
 * or on any class or interface in the type hierarchy or enclosing class hierarchy
 * above the {@code @Nested} test class.</li>
 * </ul>
 *
 * <p>When {@code @MockitoBean} is declared on a field, the bean to mock is inferred
 * from the type of the annotated field. If multiple candidates exist in the
 * {@code ApplicationContext}, a {@code @Qualifier} annotation can be declared
 * on the field to help disambiguate. In the absence of a {@code @Qualifier}
 * annotation, the name of the annotated field will be used as a <em>fallback
 * qualifier</em>. Alternatively, you can explicitly specify a bean name to mock
 * by setting the {@link #value() value} or {@link #name() name} attribute.
 *
 * <p>When {@code @MockitoBean} is declared at the type level, the type of bean
 * (or beans) to mock must be supplied via the {@link #types() types} attribute.
 * If multiple candidates exist in the {@code ApplicationContext}, you can
 * explicitly specify a bean name to mock by setting the {@link #name() name}
 * attribute. Note, however, that the {@code types} attribute must contain a
 * single type if an explicit bean {@code name} is configured.
 *
 * <p>A bean will be created if a corresponding bean does not exist. However, if
 * you would like for the test to fail when a corresponding bean does not exist,
 * you can set the {@link #enforceOverride() enforceOverride} attribute to {@code true}
 * &mdash; for example,  {@code @MockitoBean(enforceOverride = true)}.
 *
 * <p>Dependencies that are known to the application context but are not beans
 * (such as those
 * {@linkplain infra.beans.factory.config.ConfigurableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found, and a mocked bean will be added to
 * the context alongside the existing dependency.
 *
 * <p><strong>WARNING</strong>: Using {@code @MockitoBean} in conjunction with
 * {@code @ContextHierarchy} can lead to undesirable results since each
 * {@code @MockitoBean} will be applied to all context hierarchy levels by default.
 * To ensure that a particular {@code @MockitoBean} is applied to a single context
 * hierarchy level, set the {@link #contextName() contextName} to match a
 * configured {@code @ContextConfiguration}
 * {@link infra.test.context.ContextConfiguration#name() name}.
 * See the Javadoc for {@link infra.test.context.ContextHierarchy @ContextHierarchy}
 * for further details and examples.
 *
 * <p><strong>NOTE</strong>: When mocking a non-singleton bean, the non-singleton
 * bean will be replaced with a singleton mock, and the corresponding bean definition
 * will be converted to a singleton. Consequently, if you mock a prototype or scoped
 * bean, the mock will be treated as a singleton. Similarly, when mocking a bean
 * created by a {@link infra.beans.factory.FactoryBean FactoryBean},
 * the {@code FactoryBean} will be replaced with a singleton mock of the type of
 * object created by the {@code FactoryBean}.
 *
 * <p>There are no restrictions on the visibility of a {@code @MockitoBean} field.
 * Such fields can therefore be {@code public}, {@code protected}, package-private
 * (default visibility), or {@code private} depending on the needs or coding
 * practices of the project.
 *
 * <p>{@code @MockitoBean} fields and type-level {@code @MockitoBean} declarations
 * will be inherited from an enclosing test class by default. See
 * {@link infra.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * <p>{@code @MockitoBean} may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> &mdash; for example, to define common mock
 * configuration in a single annotation that can be reused across a test suite.
 * {@code @MockitoBean} can also be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation at the type level &mdash; for example, to mock several beans by
 * {@link #name() name}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @see infra.test.context.bean.override.mockito.MockitoBeans @MockitoBeans
 * @see infra.test.context.bean.override.mockito.MockitoSpyBean @MockitoSpyBean
 * @see infra.test.context.bean.override.convention.TestBean @TestBean
 * @since 5.0
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MockitoBeans.class)
@BeanOverride(MockitoBeanOverrideProcessor.class)
public @interface MockitoBean {

  /**
   * Alias for {@link #name() name}.
   * <p>Intended to be used when no other attributes are needed &mdash; for
   * example, {@code @MockitoBean("customBeanName")}.
   *
   * @see #name()
   */
  @AliasFor("name")
  String value() default "";

  /**
   * Name of the bean to mock.
   * <p>If left unspecified, the bean to mock is selected according to the
   * configured {@link #types() types} or the annotated field's type, taking
   * qualifiers into account if necessary. See the {@linkplain MockitoBean
   * class-level documentation} for details.
   *
   * @see #value()
   */
  @AliasFor("value")
  String name() default "";

  /**
   * One or more types to mock.
   * <p>Defaults to none.
   * <p>Each type specified will result in a mock being created and registered
   * with the {@code ApplicationContext}.
   * <p>Types must be omitted when the annotation is used on a field.
   * <p>When {@code @MockitoBean} also defines a {@link #name name}, this attribute
   * can only contain a single value.
   *
   * @return the types to mock
   * @since 5.0
   */
  Class<?>[] types() default {};

  /**
   * The name of the context hierarchy level in which this {@code @MockitoBean}
   * should be applied.
   * <p>Defaults to an empty string which indicates that this {@code @MockitoBean}
   * should be applied to all application contexts.
   * <p>If a context name is configured, it must match a name configured via
   * {@code @ContextConfiguration(name=...)}.
   *
   * @see infra.test.context.ContextHierarchy @ContextHierarchy
   * @see infra.test.context.ContextConfiguration#name() @ContextConfiguration(name=...)
   * @since 5.0
   */
  String contextName() default "";

  /**
   * Extra interfaces that should also be declared by the mock.
   * <p>Defaults to none.
   *
   * @return any extra interfaces
   * @see MockSettings#extraInterfaces(Class...)
   */
  Class<?>[] extraInterfaces() default {};

  /**
   * The {@link Answers} type to use in the mock.
   * <p>Defaults to {@link Answers#RETURNS_DEFAULTS}.
   *
   * @return the answer type
   */
  Answers answers() default Answers.RETURNS_DEFAULTS;

  /**
   * Whether the generated mock is serializable.
   * <p>Defaults to {@code false}.
   *
   * @return {@code true} if the mock is serializable
   * @see MockSettings#serializable()
   */
  boolean serializable() default false;

  /**
   * The reset mode to apply to the mock.
   * <p>The default is {@link MockReset#AFTER} meaning that mocks are
   * automatically reset after each test method is invoked.
   *
   * @return the reset mode
   */
  MockReset reset() default MockReset.AFTER;

  /**
   * Whether to require the existence of the bean being mocked.
   * <p>Defaults to {@code false} which means that a mock will be created if a
   * corresponding bean does not exist.
   * <p>Set to {@code true} to cause an exception to be thrown if a corresponding
   * bean does not exist.
   *
   * @see infra.test.context.bean.override.BeanOverrideStrategy#REPLACE_OR_CREATE
   * @see infra.test.context.bean.override.BeanOverrideStrategy#REPLACE
   */
  boolean enforceOverride() default false;

}
