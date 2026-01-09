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

package infra.test.web.mock.setup;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.test.web.mock.request.RequestPostProcessor;

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
   * builder and the Infra WebApplicationContext that will be passed to the
   * {@code DispatcherHandler}.
   *
   * @param builder the builder for the MockMvc
   * @param context the Infra configuration
   * @return a post processor to be applied to every request performed
   * through the {@code MockMvc} instance.
   */
  @Nullable
  default RequestPostProcessor beforeMockMvcCreated(
          ConfigurableMockMvcBuilder<?> builder, ApplicationContext context) {

    return null;
  }

}
