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

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.CustomScopeConfigurer;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Component;

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
   * {@link cn.taketoday.beans.factory.config.Scope#SINGLETON SCOPE_SINGLETON}.
   *
   * @see BeanDefinition#SCOPE_PROTOTYPE
   * @see BeanDefinition#SCOPE_SINGLETON
   * @see cn.taketoday.web.WebApplicationContext#SCOPE_REQUEST
   * @see cn.taketoday.web.WebApplicationContext#SCOPE_SESSION
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
   * <p>Analogous to {@code <aop:scoped-proxy/>} support in Framework XML.
   *
   * @see ScopedProxyMode
   */
  ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
