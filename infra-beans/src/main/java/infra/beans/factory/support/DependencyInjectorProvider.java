/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.support;

/**
 * Provider for {@link DependencyInjector} instances.
 * <p>This interface defines a contract for components that can supply
 * a {@code DependencyInjector} to be used for injecting dependencies
 * into target objects.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0 2021/9/7 23:08
 */
public interface DependencyInjectorProvider {

  /**
   * Retrieves the {@link DependencyInjector} instance.
   *
   * @return the dependency injector instance
   */
  DependencyInjector getInjector();

}

