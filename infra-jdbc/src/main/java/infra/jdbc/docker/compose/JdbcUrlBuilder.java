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

import org.jspecify.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import infra.docker.compose.core.RunningService;
import infra.util.Assert;
import infra.util.StringUtils;

/**
 * Utility used to build a JDBC URL for a {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yu Xia
 */
class JdbcUrlBuilder {

  private static final String PARAMETERS_LABEL = "infra.jdbc.parameters";

  private final String driverProtocol;

  private final int containerPort;

  private final Encoding encoding;

  /**
   * Create a new {@link JdbcUrlBuilder} instance.
   *
   * @param driverProtocol the driver protocol
   * @param containerPort the source container port
   */
  JdbcUrlBuilder(String driverProtocol, int containerPort) {
    this(driverProtocol, containerPort, Encoding.NONE);
  }

  /**
   * Create a new {@link JdbcUrlBuilder} instance.
   *
   * @param driverProtocol the driver protocol
   * @param containerPort the source container port
   * @param encoding the encoding to apply
   */
  JdbcUrlBuilder(String driverProtocol, int containerPort, Encoding encoding) {
    Assert.notNull(driverProtocol, "'driverProtocol' is required");
    this.driverProtocol = driverProtocol;
    this.containerPort = containerPort;
    this.encoding = encoding;
  }

  /**
   * Build a JDBC URL for the given {@link RunningService}.
   *
   * @param service the running service
   * @return a new JDBC URL
   */
  String build(RunningService service) {
    return build(service, null);
  }

  /**
   * Build a JDBC URL for the given {@link RunningService} and database.
   *
   * @param service the running service
   * @param database the database to connect to
   * @return a new JDBC URL
   */
  String build(RunningService service, @Nullable String database) {
    return urlFor(service, database);
  }

  private String urlFor(RunningService service, @Nullable String database) {
    Assert.notNull(service, "'service' is required");
    StringBuilder url = new StringBuilder("jdbc:%s://%s:%d".formatted(this.driverProtocol, service.host(),
            service.ports().get(this.containerPort)));
    if (StringUtils.isNotEmpty(database)) {
      url.append("/");
      url.append(this.encoding.apply(database));
    }
    String parameters = getParameters(service);
    if (StringUtils.isNotEmpty(parameters)) {
      appendParameters(url, parameters);
    }
    return url.toString();
  }

  /**
   * Appends to the given {@code url} the given {@code parameters}.
   * <p>
   * The default implementation appends a {@code ?} followed by the {@code parameters}.
   *
   * @param url the url
   * @param parameters the parameters
   */
  protected void appendParameters(StringBuilder url, String parameters) {
    url.append("?").append(parameters);
  }

  private @Nullable String getParameters(RunningService service) {
    return service.labels().get(PARAMETERS_LABEL);
  }

  /**
   * Encoding strategies for database names.
   */
  enum Encoding {

    /**
     * Do not apply any encoding.
     */
    NONE,

    /**
     * URL encode elements.
     */
    URL,

    /**
     * URL encode elements, representing spaces as {@code %20} rather than {@code +}.
     */
    URL_PLUS;

    String apply(String value) {
      return switch (this) {
        case NONE -> value;
        case URL -> URLEncoder.encode(value, StandardCharsets.UTF_8);
        case URL_PLUS -> URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
      };
    }

  }

}
