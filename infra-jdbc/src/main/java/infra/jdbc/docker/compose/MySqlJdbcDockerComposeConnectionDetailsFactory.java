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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.docker.compose;

import infra.docker.compose.core.RunningService;
import infra.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import infra.docker.compose.service.connection.DockerComposeConnectionSource;
import infra.jdbc.config.JdbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code mysql} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MySqlJdbcDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

  protected MySqlJdbcDockerComposeConnectionDetailsFactory() {
    super("mysql");
  }

  @Override
  protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
    return new MySqlJdbcDockerComposeConnectionDetails(source.getRunningService());
  }

  /**
   * {@link JdbcConnectionDetails} backed by a {@code mysql} {@link RunningService}.
   */
  static class MySqlJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
          implements JdbcConnectionDetails {

    private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("mysql", 3306);

    private final MySqlEnvironment environment;

    private final String jdbcUrl;

    MySqlJdbcDockerComposeConnectionDetails(RunningService service) {
      super(service);
      this.environment = new MySqlEnvironment(service.env());
      this.jdbcUrl = jdbcUrlBuilder.build(service, this.environment.getDatabase());
    }

    @Override
    public String getUsername() {
      return this.environment.getUsername();
    }

    @Override
    public String getPassword() {
      return this.environment.getPassword();
    }

    @Override
    public String getJdbcUrl() {
      return this.jdbcUrl;
    }

  }

}
