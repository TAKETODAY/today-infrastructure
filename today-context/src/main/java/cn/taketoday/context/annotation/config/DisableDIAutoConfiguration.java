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

package cn.taketoday.context.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.context.annotation.AnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicates that a class provides configuration that can be automatically applied by
 * Infra Unify. Auto-configuration classes are regular
 * {@link Configuration @Configuration} with the exception that
 * {@literal Configuration#proxyBeanMethods() proxyBeanMethods} is always {@code false}.
 * <p>
 * They are located using {@link ImportCandidates} and the {@link cn.taketoday.lang.TodayStrategies}
 * mechanism (keyed against {@link EnableAutoConfiguration}).
 * <p>
 * Generally auto-configuration classes are marked as {@link Conditional @Conditional}
 * (most often using {@link ConditionalOnClass @ConditionalOnClass} and
 * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/5 20:48
 */
@AutoConfiguration
@DisableDependencyInjection
@DisableAllDependencyInjection
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DisableDIAutoConfiguration {

  /**
   * Explicitly specify the name of the Framework bean definition associated with the
   * {@code @AutoConfiguration} class. If left unspecified (the common case), a bean
   * name will be automatically generated.
   * <p>
   * The custom name applies only if the {@code @AutoConfiguration} class is picked up
   * via component scanning or supplied directly to an
   * {@link AnnotationConfigApplicationContext}. If the {@code @AutoConfiguration} class
   * is registered as a traditional XML bean definition, the name/id of the bean element
   * will take precedence.
   *
   * @return the explicit component name, if any (or empty String otherwise)
   * @see AnnotationBeanNameGenerator
   */
  @AliasFor(annotation = AutoConfiguration.class)
  String value() default "";

  /**
   * The auto-configure classes that should have not yet been applied.
   *
   * @return the classes
   */
  @AliasFor(annotation = AutoConfiguration.class)
  Class<?>[] before() default {};

  /**
   * The names of the auto-configure classes that should have not yet been applied.
   *
   * @return the class names
   */
  @AliasFor(annotation = AutoConfiguration.class)
  String[] beforeName() default {};

  /**
   * The auto-configure classes that should have already been applied.
   *
   * @return the classes
   */
  @AliasFor(annotation = AutoConfiguration.class)
  Class<?>[] after() default {};

  /**
   * The names of the auto-configure classes that should have already been applied.
   *
   * @return the class names
   */
  @AliasFor(annotation = AutoConfiguration.class)
  String[] afterName() default {};

}
