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

package cn.taketoday.framework;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.FilterType;
import cn.taketoday.context.annotation.config.AutoConfigurationExcludeFilter;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.annotation.config.TypeExcludeFilter;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates a {@link Configuration configuration} class that declares one or more
 * {@link Bean @Bean} methods and also triggers {@link EnableAutoConfiguration
 * auto-configuration} and {@link ComponentScan component scanning}. This is a convenience
 * annotation that is equivalent to declaring {@code @Configuration},
 * {@code @EnableAutoConfiguration} and {@code @ComponentScan}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/3 13:28
 */
@Inherited
@Documented
@Configuration
@EnableAutoConfiguration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan(excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
})
public @interface InfraApplication {

  /**
   * Exclude specific auto-configuration classes such that they will never be applied.
   *
   * @return the classes to exclude
   */
  @AliasFor(annotation = EnableAutoConfiguration.class)
  Class<?>[] exclude() default {};

  /**
   * Exclude specific auto-configuration class names such that they will never be
   * applied.
   *
   * @return the class names to exclude
   */
  @AliasFor(annotation = EnableAutoConfiguration.class)
  String[] excludeName() default {};

  /**
   * Base packages to scan for annotated components. Use {@link #scanBasePackageClasses}
   * for a type-safe alternative to String-based package names.
   * <p>
   * <strong>Note:</strong> this setting is an alias for
   * {@link ComponentScan @ComponentScan} only.
   *
   * @return base packages to scan
   */
  @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
  String[] scanBasePackages() default {};

  /**
   * Type-safe alternative to {@link #scanBasePackages} for specifying the packages to
   * scan for annotated components. The package of each class specified will be scanned.
   * <p>
   * Consider creating a special no-op marker class or interface in each package that
   * serves no purpose other than being referenced by this attribute.
   * <p>
   * <strong>Note:</strong> this setting is an alias for
   * {@link ComponentScan @ComponentScan} only.
   *
   * @return base packages to scan
   */
  @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
  Class<?>[] scanBasePackageClasses() default {};

  /**
   * The {@link BeanNameGenerator} class to be used for naming detected components
   * within the Infra container.
   * <p>
   * The default value of the {@link BeanNameGenerator} interface itself indicates that
   * the scanner used to process this {@code @InfraApplication} annotation should
   * use its inherited bean name generator, e.g. the default
   * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
   * application context at bootstrap time.
   *
   * @return {@link BeanNameGenerator} to use
   * @see Application#setBeanNameGenerator(BeanNameGenerator)
   */
  @AliasFor(annotation = ComponentScan.class, attribute = "nameGenerator")
  Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

  /**
   * Specify whether {@link Bean @Bean} methods should get proxied in order to enforce
   * bean lifecycle behavior, e.g. to return shared singleton bean instances even in
   * case of direct {@code @Bean} method calls in user code. This feature requires
   * method interception, implemented through a runtime-generated CGLIB subclass which
   * comes with limitations such as the configuration class and its methods not being
   * allowed to declare {@code final}.
   * <p>
   * The default is {@code true}, allowing for 'inter-bean references' within the
   * configuration class as well as for external calls to this configuration's
   * {@code @Bean} methods, e.g. from another configuration class. If this is not needed
   * since each of this particular configuration's {@code @Bean} methods is
   * self-contained and designed as a plain factory method for container use, switch
   * this flag to {@code false} in order to avoid CGLIB subclass processing.
   * <p>
   * Turning off bean method interception effectively processes {@code @Bean} methods
   * individually like when declared on non-{@code @Configuration} classes, a.k.a.
   * "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore behaviorally
   * equivalent to removing the {@code @Configuration} stereotype.
   *
   * @return whether to proxy {@code @Bean} methods
   */
  @AliasFor(annotation = Configuration.class)
  boolean proxyBeanMethods() default true;

}
