/*
 * Copyright 2012-present the original author or authors.
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

package infra.sql.init.dependency;

import java.util.Set;

import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Detects beans that depend on database initialization. Implementations should be
 * registered in {@code META-INF/today.strategies} under the key
 * {@code infra.sql.init.dependency.DependsOnDatabaseInitializationDetector}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface DependsOnDatabaseInitializationDetector {

  /**
   * Detect beans defined in the given {@code beanFactory} that depend on database
   * initialization. If no beans are detected, an empty set is returned.
   *
   * @param beanFactory bean factory to examine
   * @return names of any beans that depend upon database initialization
   */
  Set<String> detect(ConfigurableBeanFactory beanFactory);

}
