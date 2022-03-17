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

package cn.taketoday.test.web.servlet.setup;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.web.context.WebApplicationContext;

/**
 * Contract for customizing a {@code ConfigurableMockMvcBuilder} in some
 * specific way, e.g. a 3rd party library that wants to provide shortcuts for
 * setting up a MockMvc.
 *
 * <p>An implementation of this interface can be plugged in via
 * {@link ConfigurableMockMvcBuilder#apply} with instances of this type likely
 * created via static methods, e.g.:
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.mySetup;
 *
 * // ...
 *
 * MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @see MockMvcConfigurerAdapter
 * @since 4.0
 */
public interface MockMvcConfigurer {

  /**
   * Invoked immediately when this {@code MockMvcConfigurer} is added via
   * {@link ConfigurableMockMvcBuilder#apply}.
   *
   * @param builder the builder for the MockMvc
   */
  default void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
  }

  /**
   * Invoked when the MockMvc instance is about to be created with the MockMvc
   * builder and the Spring WebApplicationContext that will be passed to the
   * {@code DispatcherServlet}.
   *
   * @param builder the builder for the MockMvc
   * @param context the Spring configuration
   * @return a post processor to be applied to every request performed
   * through the {@code MockMvc} instance.
   */
  @Nullable
  default RequestPostProcessor beforeMockMvcCreated(
          ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {

    return null;
  }

}
