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

package cn.taketoday.framework.web.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.annotation.AliasFor;
import jakarta.servlet.Filter;

/**
 * {@link Conditional @Conditional} that only matches when no {@link Filter} beans of the
 * specified type are contained in the {@link BeanFactory}. This condition will detect
 * both directly registered {@link Filter} beans as well as those registered via a
 * {@link FilterRegistrationBean}.
 * <p>
 * When placed on a {@code @Bean} method, the bean class defaults to the return type of
 * the factory method or the type of the {@link Filter} if the bean is a
 * {@link FilterRegistrationBean}:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAutoConfiguration {
 *
 *     &#064;ConditionalOnMissingFilterBean
 *     &#064;Bean
 *     public MyFilter myFilter() {
 *         ...
 *     }
 *
 * }</pre>
 * <p>
 * In the sample above the condition will match if no bean of type {@code MyFilter} or
 * {@code FilterRegistrationBean<MyFilter>} is already contained in the
 * {@link BeanFactory}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 22:10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ConditionalOnMissingBean(parameterizedContainer = FilterRegistrationBean.class)
public @interface ConditionalOnMissingFilterBean {

  /**
   * The filter bean type that must not be present.
   *
   * @return the bean type
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class)
  Class<? extends Filter>[] value() default {};

}
