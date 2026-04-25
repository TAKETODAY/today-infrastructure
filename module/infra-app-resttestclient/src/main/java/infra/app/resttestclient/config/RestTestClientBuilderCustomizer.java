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

package infra.app.resttestclient.config;

import infra.test.web.mock.client.RestTestClient;

/**
 * A customizer that can be implemented by beans wishing to customize the {@link RestTestClient.Builder
 * RestTestClient.Builder} to fine-tune its auto-configuration before a
 * {@link RestTestClient} is created.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
@FunctionalInterface
public interface RestTestClientBuilderCustomizer {

  /**
   * Customize the given {@link RestTestClient.Builder RestTestClient.Builder}.
   *
   * @param builder the builder
   */
  void customize(RestTestClient.Builder<?> builder);

}
