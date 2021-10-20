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

package cn.taketoday.context.aware;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.ResourceLoader;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ResourceLoader} (typically the ApplicationContext) that it runs in.
 * This is an alternative to a full {@link ApplicationContext} dependency via
 * the {@link cn.taketoday.context.aware.ApplicationContextAware} interface.
 *
 * <p>Note that {@link cn.taketoday.core.io.Resource} dependencies can also
 * be exposed as bean properties of type {@code Resource} or {@code Resource[]},
 * populated via Strings with automatic type conversion by the bean factory. This
 * removes the need for implementing any callback interface just for the purpose
 * of accessing specific file resources.
 *
 * <p>You typically need a {@link ResourceLoader} when your application object has to
 * access a variety of file resources whose names are calculated. A good strategy is
 * to make the object use a {@link cn.taketoday.core.io.DefaultResourceLoader}
 * but still implement {@code ResourceLoaderAware} to allow for overriding when
 * running in an {@code ApplicationContext}.
 *
 * <p>A passed-in {@code ResourceLoader} can also be checked for the
 * {@link PatternResourceLoader} interface
 * and cast accordingly, in order to resolve resource patterns into arrays of
 * {@code Resource} objects. This will always work when running in an ApplicationContext
 * (since the context interface extends the ResourcePatternResolver interface). Use a
 * {@link PathMatchingPatternResourceLoader} as
 * default; see also the {@code ResourcePatternUtils.getResourcePatternResolver} method.
 *
 * <p>As an alternative to a {@code ResourcePatternResolver} dependency, consider
 * exposing bean properties of type {@code Resource[]} array, populated via pattern
 * Strings with automatic type conversion by the bean factory at binding time.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY 2021/10/7 17:04
 * @see ApplicationContextAware
 * @see cn.taketoday.core.io.Resource
 * @see cn.taketoday.core.io.ResourceLoader
 * @since 4.0
 */
public interface ResourceLoaderAware extends Aware {

  /**
   * Set the ResourceLoader that this object runs in.
   * <p>This might be a ResourcePatternResolver, which can be checked
   * through {@code instanceof ResourcePatternResolver}. See also the
   * {@code ResourcePatternUtils.getResourcePatternResolver} method.
   * <p>Invoked after population of normal bean properties but before an init callback
   * like InitializingBean's {@code afterPropertiesSet} or a custom init-method.
   * Invoked before ApplicationContextAware's {@code setApplicationContext}.
   *
   * @param resourceLoader the ResourceLoader object to be used by this object
   * @see PatternResourceLoader
   * @see cn.taketoday.core.io.PathMatchingPatternResourceLoader#fromResourceLoader
   */
  void setResourceLoader(ResourceLoader resourceLoader);

}

