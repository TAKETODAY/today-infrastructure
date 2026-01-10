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

package infra.scripting.groovy;

import groovy.lang.GroovyObject;

/**
 * Strategy used by {@link GroovyScriptFactory} to allow the customization of
 * a created {@link GroovyObject}.
 *
 * <p>This is useful to allow the authoring of DSLs, the replacement of missing
 * methods, and so forth. For example, a custom {@link groovy.lang.MetaClass}
 * could be specified.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GroovyScriptFactory
 * @since 4.0
 */
@FunctionalInterface
public interface GroovyObjectCustomizer {

  /**
   * Customize the supplied {@link GroovyObject}.
   * <p>For example, this can be used to set a custom metaclass to
   * handle missing methods.
   *
   * @param goo the {@code GroovyObject} to customize
   */
  void customize(GroovyObject goo);

}
