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
import infra.util.StringUtils;

/**
 * Base class for a {@link DockerComposeConnectionDetailsFactory} to create
 * {@link JdbcConnectionDetails} for an {@code oracle-free} or {@code oracle-xe} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class OracleJdbcDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

  private final String defaultDatabase;

  protected OracleJdbcDockerComposeConnectionDetailsFactory(OracleContainer container) {
    super(container.getImageName());
    this.defaultDatabase = container.getDefaultDatabase();
  }

  @Override
  protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
    return new OracleJdbcDockerComposeConnectionDetails(source.getRunningService(), this.defaultDatabase);
  }

  /**
   * {@link JdbcConnectionDetails} backed by an {@code oracle-xe} or {@code oracle-free}
   * {@link RunningService}.
   */
  static class OracleJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
          implements JdbcConnectionDetails {

    private static final String PARAMETERS_LABEL = "infra.jdbc.parameters";

    private final OracleEnvironment environment;

    private final String jdbcUrl;

    OracleJdbcDockerComposeConnectionDetails(RunningService service, String defaultDatabase) {
      super(service);
      this.environment = new OracleEnvironment(service.env(), defaultDatabase);
      this.jdbcUrl = "jdbc:oracle:thin:@" + service.host() + ":" + service.ports().get(1521) + "/"
              + this.environment.getDatabase() + getParameters(service);
    }

    private String getParameters(RunningService service) {
      String parameters = service.labels().get(PARAMETERS_LABEL);
      return (StringUtils.isNotEmpty(parameters)) ? "?" + parameters : "";
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
