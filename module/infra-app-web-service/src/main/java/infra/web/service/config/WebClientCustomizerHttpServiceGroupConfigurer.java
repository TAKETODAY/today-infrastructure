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

package infra.web.service.config;

import infra.beans.factory.ObjectProvider;
import infra.web.client.WebClientCustomizer;
import infra.web.reactive.client.WebClient;
import infra.web.service.registry.HttpServiceGroup;
import infra.web.service.support.WebClientHttpServiceGroupConfigurer;

/**
 * A {@link WebClientHttpServiceGroupConfigurer} to apply auto-configured
 * {@link WebClientCustomizer} beans to the group's {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class WebClientCustomizerHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

  /**
   * Allow user defined configurers to apply before / after ours.
   */
  private static final int ORDER = 0;

  private final ObjectProvider<WebClientCustomizer> customizers;

  WebClientCustomizerHttpServiceGroupConfigurer(ObjectProvider<WebClientCustomizer> customizers) {
    this.customizers = customizers;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void configureGroups(Groups<WebClient.Builder> groups) {
    groups.forEachClient(this::configureClient);
  }

  private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
    this.customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  }

}
