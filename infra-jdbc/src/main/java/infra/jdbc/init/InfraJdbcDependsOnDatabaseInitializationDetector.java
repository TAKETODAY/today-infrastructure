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

package infra.jdbc.init;

import java.util.Set;

import infra.jdbc.RepositoryManager;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.namedparam.NamedParameterJdbcOperations;
import infra.jdbc.core.simple.JdbcClient;
import infra.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import infra.sql.init.dependency.DependsOnDatabaseInitializationDetector;

/**
 * {@link DependsOnDatabaseInitializationDetector} for Infra JDBC support.
 *
 * @author Andy Wilkinson
 */
class InfraJdbcDependsOnDatabaseInitializationDetector extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

  @Override
  protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
    return Set.of(JdbcClient.class, JdbcOperations.class, NamedParameterJdbcOperations.class, RepositoryManager.class);
  }

}
