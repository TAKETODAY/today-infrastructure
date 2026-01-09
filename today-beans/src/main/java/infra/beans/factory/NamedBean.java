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

package infra.beans.factory;

/**
 * Counterpart of {@link BeanNameAware}. Returns the bean name of an object.
 *
 * <p>This interface can be introduced to avoid a brittle dependence on
 * bean name in objects used with IoC and AOP.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanNameAware
 * @since 4.0 2022/3/9 22:22
 */
public interface NamedBean {

  /**
   * Return the name of this bean in a bean factory, if known.
   */
  String getBeanName();

}
