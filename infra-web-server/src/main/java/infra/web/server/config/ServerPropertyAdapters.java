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

package infra.web.server.config;

import org.jspecify.annotations.Nullable;

import infra.app.cloud.CloudPlatform;
import infra.core.env.Environment;
import infra.util.PropertyMapper;
import infra.util.PropertyMapper.Source.Adapter;
import infra.web.config.ForwardedHeaderStrategy;
import infra.web.config.WebProperties;
import infra.web.config.WebProperties.ForwardedHeaders;
import infra.web.server.ForwardedRequestHeaders;

/**
 * {@link PropertyMapper} adapters for shared {@link WebProperties} used by web servers.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public final class ServerPropertyAdapters {

  private ServerPropertyAdapters() {
  }

  /**
   * Return an {@link Adapter} that converts {@link WebProperties} to
   * {@link ForwardedRequestHeaders}.
   *
   * @param environment the environment or {@code null}
   * @return the properties adapter
   */
  public static Adapter<WebProperties, ForwardedRequestHeaders> webServerForwardedRequestHeaders(
          @Nullable Environment environment) {
    return new ForwardedRequestHeadersAdapter(environment);
  }

  private record ForwardedRequestHeadersAdapter(
          @Nullable Environment environment) implements Adapter<WebProperties, ForwardedRequestHeaders> {

    @Override
    public @Nullable ForwardedRequestHeaders adapt(WebProperties webProperties) {
      ForwardedHeaderStrategy strategy = webProperties.forwardedHeaders.strategy;
      if (strategy == null) {
        CloudPlatform cloudPlatform = CloudPlatform.getActive(environment);
        strategy = cloudPlatform != null && cloudPlatform.isUsingForwardHeaders()
                ? ForwardedHeaderStrategy.WEB_SERVER : ForwardedHeaderStrategy.NONE;
      }
      return strategy == ForwardedHeaderStrategy.WEB_SERVER ? forwardedRequestHeaders(webProperties) : null;
    }

    private ForwardedRequestHeaders forwardedRequestHeaders(WebProperties webProperties) {
      ForwardedHeaders forwardedProperties = webProperties.forwardedHeaders;
      return new ForwardedRequestHeaders(forwardedProperties.naming, forwardedProperties.usePrefix);
    }

  }

}
