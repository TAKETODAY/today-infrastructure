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

package infra.context;

import infra.beans.factory.Aware;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ResourceLoader} (typically the ApplicationContext) that it runs in.
 * This is an alternative to a full {@link ApplicationContext} dependency via
 * the {@link ApplicationContextAware} interface.
 *
 * <p>Note that {@link Resource} dependencies can also
 * be exposed as bean properties of type {@code Resource} or {@code Resource[]},
 * populated via Strings with automatic type conversion by the bean factory. This
 * removes the need for implementing any callback interface just for the purpose
 * of accessing specific file resources.
 *
 * <p>You typically need a {@link ResourceLoader} when your application object has to
 * access a variety of file resources whose names are calculated. A good strategy is
 * to make the object use a {@link DefaultResourceLoader}
 * but still implement {@code ResourceLoaderAware} to allow for overriding when
 * running in an {@code ApplicationContext}.
 *
 * <p>A passed-in {@code ResourceLoader} can also be checked for the
 * {@link PatternResourceLoader} interface
 * and cast accordingly, in order to resolve resource patterns into arrays of
 * {@code Resource} objects. This will always work when running in an ApplicationContext
 * (since the context interface extends the PatternResourceLoader interface). Use a
 * {@link PathMatchingPatternResourceLoader} as
 * default; see also the {@code ResourcePatternUtils.getPatternResourceLoader} method.
 *
 * <p>As an alternative to a {@code PatternResourceLoader} dependency, consider
 * exposing bean properties of type {@code Resource[]} array, populated via pattern
 * Strings with automatic type conversion by the bean factory at binding time.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY 2021/10/7 17:04
 * @see ApplicationContextAware
 * @see Resource
 * @see ResourceLoader
 * @since 4.0
 */
public interface ResourceLoaderAware extends Aware {

  /**
   * Set the ResourceLoader that this object runs in.
   * <p>This might be a PatternResourceLoader, which can be checked
   * through {@code instanceof PatternResourceLoader}. See also the
   * {@code ResourcePatternUtils.getPatternResourceLoader} method.
   * <p>Invoked after population of normal bean properties but before an init callback
   * like InitializingBean's {@code afterPropertiesSet} or a custom init-method.
   * Invoked before ApplicationContextAware's {@code setApplicationContext}.
   *
   * @param resourceLoader the ResourceLoader object to be used by this object
   * @see PatternResourceLoader
   * @see PathMatchingPatternResourceLoader#fromResourceLoader
   */
  void setResourceLoader(ResourceLoader resourceLoader);

}

