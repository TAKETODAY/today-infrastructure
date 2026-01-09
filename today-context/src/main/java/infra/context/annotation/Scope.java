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

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.CustomScopeConfigurer;
import infra.core.annotation.AliasFor;
import infra.stereotype.Component;

/**
 * When used as a type-level annotation in conjunction with
 * {@link Component @Component},
 * {@code @Scope} indicates the name of a scope to use for instances of
 * the annotated type.
 *
 * <p>When used as a method-level annotation in conjunction with
 * {@link Bean @Bean}, {@code @Scope} indicates the name of a scope to use
 * for the instance returned from the method.
 *
 * <p><b>NOTE:</b> {@code @Scope} annotations are only introspected on the
 * concrete bean class (for annotated components) or the factory method
 * (for {@code @Bean} methods). In contrast to XML bean definitions,
 * there is no notion of bean definition inheritance, and inheritance
 * hierarchies at the class level are irrelevant for metadata purposes.
 *
 * <p>In this context, <em>scope</em> means the lifecycle of an instance,
 * such as {@code singleton}, {@code prototype}, and so forth. Scopes
 * provided out of the box in Framework may be referred to using the
 * {@code SCOPE_*} constants available in the {@link ConfigurableBeanFactory}
 * and {@code WebApplicationContext} interfaces.
 *
 * <p>To register additional custom scopes, see
 * {@link CustomScopeConfigurer CustomScopeConfigurer}.
 *
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 * @author TODAY 2021/10/26 15:33
 * @see Bean
 * @see Component
 * @since 4.0
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {

  /**
   * Alias for {@link #scopeName}.
   *
   * @see #scopeName
   */
  @AliasFor("scopeName")
  String value() default "";

  /**
   * Specifies the name of the scope to use for the annotated component/bean.
   * <p>Defaults to an empty string ({@code ""}) which implies
   * {@link infra.beans.factory.config.Scope#SINGLETON SCOPE_SINGLETON}.
   *
   * @see BeanDefinition#SCOPE_PROTOTYPE
   * @see BeanDefinition#SCOPE_SINGLETON
   * @see infra.web.RequestContext#SCOPE_REQUEST
   * @see infra.web.RequestContext#SCOPE_SESSION
   * @see #value
   * @since 4.0
   */
  @AliasFor("value")
  String scopeName() default "";

  /**
   * Specifies whether a component should be configured as a scoped proxy
   * and if so, whether the proxy should be interface-based or subclass-based.
   * <p>Defaults to {@link ScopedProxyMode#DEFAULT}, which typically indicates
   * that no scoped proxy should be created unless a different default
   * has been configured at the component-scan instruction level.
   * <p>Analogous to {@code <aop:scoped-proxy/>} support in Infra XML.
   *
   * @see ScopedProxyMode
   */
  ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
