/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.annotation.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.AnnotationBeanNameGenerator;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.annotation.AliasFor;
import infra.lang.TodayStrategies;

/**
 * Indicates that a class provides configuration that can be automatically applied by
 * Framework Unify. Auto-configuration classes are regular
 * {@link Configuration @Configuration} with the exception that
 * {@literal Configuration#proxyBeanMethods() proxyBeanMethods} is always {@code false}.
 * <p>
 * They are located using {@link ImportCandidates} and the {@link TodayStrategies}
 * mechanism (keyed against {@link EnableAutoConfiguration}).
 * <p>
 * Generally auto-configuration classes are marked as {@link Conditional @Conditional}
 * (most often using {@link ConditionalOnClass @ConditionalOnClass} and
 * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableAutoConfiguration
 * @see AutoConfigureBefore
 * @see AutoConfigureAfter
 * @see Conditional
 * @see ConditionalOnClass
 * @see ConditionalOnMissingBean
 * @since 4.0 2022/3/5 22:44
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore
@AutoConfigureAfter
public @interface AutoConfiguration {

  /**
   * Explicitly specify the name of the Infra bean definition associated with the
   * {@code @AutoConfiguration} class. If left unspecified (the common case), a bean
   * name will be automatically generated.
   * <p>
   * The custom name applies only if the {@code @AutoConfiguration} class is picked up
   * through component scanning or supplied directly to an
   * {@link AnnotationConfigApplicationContext}. If the {@code @AutoConfiguration} class
   * is registered as a traditional XML bean definition, the name/id of the bean element
   * will take precedence.
   *
   * @return the explicit component name, if any (or empty String otherwise)
   * @see AnnotationBeanNameGenerator
   */
  @AliasFor(annotation = Configuration.class)
  String value() default "";

  /**
   * The auto-configuration classes that should have not yet been applied.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation. In order to use this annotation as a
   * meta-annotation, only use the {@link #beforeName} attribute.
   *
   * @return the classes
   */
  @AliasFor(annotation = AutoConfigureBefore.class, attribute = "value")
  Class<?>[] before() default {};

  /**
   * The names of the auto-configuration classes that should have not yet been applied.
   * In the unusual case that an auto-configuration class is not a top-level class, its
   * name should use {@code $} to separate it from its containing class, for example
   * {@code com.example.Outer$NestedAutoConfiguration}.
   *
   * @return the class names
   */
  @AliasFor(annotation = AutoConfigureBefore.class, attribute = "name")
  String[] beforeName() default {};

  /**
   * The auto-configuration classes that should have already been applied.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation. In order to use this annotation as a
   * meta-annotation, only use the {@link #afterName} attribute.
   *
   * @return the classes
   */
  @AliasFor(annotation = AutoConfigureAfter.class, attribute = "value")
  Class<?>[] after() default {};

  /**
   * The names of the auto-configuration classes that should have already been applied.
   * In the unusual case that an auto-configuration class is not a top-level class, its
   * class name should use {@code $} to separate it from its containing class, for
   * example {@code com.example.Outer$NestedAutoConfiguration}.
   *
   * @return the class names
   */
  @AliasFor(annotation = AutoConfigureAfter.class, attribute = "name")
  String[] afterName() default {};

}

