/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when the specified classes are on
 * the classpath.
 * <p>
 * A {@code Class} {@link #value() value} can be safely specified on
 * {@code @Configuration} classes as the annotation metadata is parsed by using ASM before
 * the class is loaded. If a class reference cannot be used then a {@link #name() name}
 * {@code String} attribute can be used.
 * <p>
 * <b>Note:</b> Extra care must be taken when using {@code @ConditionalOnClass} on
 * {@code @Bean} methods where typically the return type is the target of the condition.
 * Before the condition on the method applies, the JVM will have loaded the class and
 * potentially processed method references which will fail if the class is not present. To
 * handle this scenario, a separate {@code @Configuration} class should be used to isolate
 * the condition. For example:
 * <pre>
 * {@code
 * @AutoConfiguration
 * public class MyAutoConfiguration {
 *
 *  @Configuration(proxyBeanMethods = false)
 *  @ConditionalOnClass(SomeService.class)
 *  public static class SomeServiceConfiguration {
 *
 *    @Bean
 *    @ConditionalOnMissingBean
 *    public SomeService someService() {
 * 	    return new SomeService();
 *    }
 *
 *  }
 * }
 * }
 *</pre>
 * @author TODAY
 * @since 2019-06-18 15:00
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

  /**
   * The classes that must be present. Since this annotation is parsed by loading class
   * bytecode, it is safe to specify classes here that may ultimately not be on the
   * classpath, only if this annotation is directly on the affected component and
   * <b>not</b> if this annotation is used as a composed, meta-annotation. In order to
   * use this annotation as a meta-annotation, only use the {@link #name} attribute.
   *
   * @return the classes that must be present
   */
  Class<?>[] value() default {};

  /**
   * The classes names that must be present.
   *
   * @return the class names that must be present.
   */
  String[] name() default {};

}
