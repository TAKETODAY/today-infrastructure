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

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.EnvironmentAware;
import infra.context.ResourceLoaderAware;
import infra.core.annotation.AliasFor;
import infra.core.type.filter.TypeFilter;

/**
 * Configures component scanning directives for use with {@link Configuration @Configuration}
 * classes.
 *
 * <p>Provides support comparable to Infra {@code <context:component-scan>}
 * XML namespace element.
 *
 * <p>Either {@link #basePackageClasses} or {@link #basePackages} (or its alias
 * {@link #value}) may be specified to define specific packages to scan. If specific
 * packages are not defined, scanning will occur recursively beginning with the
 * package of the class that declares this annotation.
 *
 * <p>Note that the {@code <context:component-scan>} element has an
 * {@code annotation-config} attribute; however, this annotation does not. This is because
 * in almost all cases when using {@code @ComponentScan}, default annotation config
 * processing (e.g. processing {@code @Autowired} and friends) is assumed. Furthermore,
 * when using {@link AnnotationConfigApplicationContext}, annotation config processors are
 * always registered, meaning that any attempt to disable them at the
 * {@code @ComponentScan} level would be ignored.
 *
 * <p>See {@link Configuration @Configuration}'s Javadoc for usage examples.
 *
 * <p>{@code @ComponentScan} can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation. {@code @ComponentScan} may also be used as a <em>meta-annotation</em>
 * to create custom <em>composed annotations</em> with attribute overrides.
 *
 * <p>Locally declared {@code @ComponentScan} annotations always take precedence
 * over and effectively <em>hide</em> {@code @ComponentScan} meta-annotations,
 * which allows explicit local configuration to override configuration that is
 * <em>meta-present</em> (including composed annotations meta-annotated with
 * {@code @ComponentScan}).
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Configuration
 * @see ClassPathBeanDefinitionScanner
 * @since 2019-11-13 23:52
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ComponentScans.class)
public @interface ComponentScan {

  /**
   * Alias for {@link #basePackages}.
   * <p>Allows for more concise annotation declarations if no other attributes
   * are needed &mdash; for example, {@code @ComponentScan("org.my.pkg")}
   * instead of {@code @ComponentScan(basePackages = "org.my.pkg")}.
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Base packages to scan for annotated components.
   * <p>{@link #value} is an alias for (and mutually exclusive with) this
   * attribute.
   * <p>Use {@link #basePackageClasses} for a type-safe alternative to
   * String-based package names.
   */
  @AliasFor("value")
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages
   * to scan for annotated components. The package of each class specified will be scanned.
   * <p>Consider creating a special no-op marker class or interface in each package
   * that serves no purpose other than being referenced by this attribute.
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * The {@link BeanNameGenerator} class to be used for naming detected components
   * within the IoC.
   * <p>The default value of the {@link BeanNameGenerator} interface itself indicates
   * that the scanner used to process this {@code @ComponentScan} annotation should
   * use its inherited bean name generator, e.g. the default
   * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
   * application context at bootstrap time.
   *
   * @see AnnotationBeanNameGenerator
   * @see FullyQualifiedAnnotationBeanNameGenerator
   */
  Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

  /**
   * The {@link ScopeMetadataResolver} to be used for resolving the scope of detected components.
   */
  Class<? extends ScopeMetadataResolver> scopeResolver() default ScopeMetadataResolver.class;

  /**
   * Indicates whether proxies should be generated for detected components, which may be
   * necessary when using scopes in a proxy-style fashion.
   * <p>The default is defer to the default behavior of the component scanner used to
   * execute the actual scan.
   * <p>Note that setting this attribute overrides any value set for {@link #scopeResolver}.
   *
   * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
   */
  ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

  /**
   * Controls the class files eligible for component detection.
   * <p>Consider use of {@link #includeFilters} and {@link #excludeFilters}
   * for a more flexible approach.
   */
  String resourcePattern() default ClassPathBeanDefinitionScanner.DEFAULT_RESOURCE_PATTERN;

  /**
   * Indicates whether automatic detection of classes annotated with {@code @Component}
   * {@code @Repository}, {@code @Service}, or {@code @Controller} should be enabled.
   */
  boolean useDefaultFilters() default true;

  /**
   * Specifies which types are eligible for component scanning.
   * <p>Further narrows the set of candidate components from everything in {@link #basePackages}
   * to everything in the base packages that matches the given filter or filters.
   * <p>Note that these filters will be applied in addition to the default filters, if specified.
   * Any type under the specified base packages which matches a given filter will be included,
   * even if it does not match the default filters (i.e. is not annotated with {@code @Component}).
   *
   * @see #resourcePattern()
   * @see #useDefaultFilters()
   */
  Filter[] includeFilters() default {};

  /**
   * Specifies which types are not eligible for component scanning.
   *
   * @see #resourcePattern
   */
  Filter[] excludeFilters() default {};

  /**
   * Specify whether scanned beans should be registered for lazy initialization.
   * <p>Default is {@code false}; switch this to {@code true} when desired.
   */
  boolean lazyInit() default false;

  /**
   * Declares the type filter to be used as an {@linkplain ComponentScan#includeFilters
   * include filter} or {@linkplain ComponentScan#excludeFilters exclude filter}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({})
  @interface Filter {

    /**
     * The type of filter to use.
     * <p>Default is {@link FilterType#ANNOTATION}.
     *
     * @see #classes
     * @see #pattern
     */
    FilterType type() default FilterType.ANNOTATION;

    /**
     * Alias for {@link #classes}.
     *
     * @see #classes
     */
    @AliasFor("classes")
    Class<?>[] value() default {};

    /**
     * The class or classes to use as the filter.
     * <p>The following table explains how the classes will be interpreted
     * based on the configured value of the {@link #type} attribute.
     * <table border="1">
     * <tr><th>{@code FilterType}</th><th>Class Interpreted As</th></tr>
     * <tr><td>{@link FilterType#ANNOTATION ANNOTATION}</td>
     * <td>the annotation itself</td></tr>
     * <tr><td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
     * <td>the type that detected components should be assignable to</td></tr>
     * <tr><td>{@link FilterType#CUSTOM CUSTOM}</td>
     * <td>an implementation of {@link TypeFilter}</td></tr>
     * </table>
     * <p>When multiple classes are specified, <em>OR</em> logic is applied
     * &mdash; for example, "include types annotated with {@code @Foo} OR {@code @Bar}".
     * <p>Custom {@link TypeFilter TypeFilters} may optionally implement any of the
     * following {@link Aware Aware} interfaces, and
     * their respective methods will be called prior to {@link TypeFilter#match match}:
     * <ul>
     * <li>{@link EnvironmentAware EnvironmentAware}</li>
     * <li>{@link BeanFactoryAware BeanFactoryAware}
     * <li>{@link BeanClassLoaderAware BeanClassLoaderAware}
     * <li>{@link ResourceLoaderAware ResourceLoaderAware}
     * </ul>
     * <p>Specifying zero classes is permitted but will have no effect on component
     * scanning.
     *
     * @see #value
     * @see #type
     */
    @AliasFor("value")
    Class<?>[] classes() default {};

    /**
     * The pattern (or patterns) to use for the filter, as an alternative
     * to specifying a Class {@link #value}.
     * <p>If {@link #type} is set to {@link FilterType#REGEX REGEX},
     * this is a regex pattern for the fully-qualified class names to match.
     *
     * @see #type
     * @see #classes
     */
    String[] pattern() default {};

  }

}
