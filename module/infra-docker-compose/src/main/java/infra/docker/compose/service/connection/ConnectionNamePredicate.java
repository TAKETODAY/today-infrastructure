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

package infra.docker.compose.service.connection;

import infra.docker.compose.core.ImageReference;
import infra.docker.compose.core.RunningService;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import infra.lang.Assert;

/**
 * {@link Predicate} that matches against connection name.
 *
 * @author Phillip Webb
 * @author Yanming Zhou
 */
class ConnectionNamePredicate implements Predicate<DockerComposeConnectionSource> {

  private final Set<String> required;

  ConnectionNamePredicate(String... required) {
    Assert.notEmpty(required, "'required' must not be empty");
    this.required = Arrays.stream(required).map(this::asCanonicalName).collect(Collectors.toSet());
  }

  @Override
  public boolean test(DockerComposeConnectionSource source) {
    String actual = getActual(source.getRunningService());
    return this.required.contains(actual);
  }

  private String getActual(RunningService service) {
    String label = service.labels().get("infra.service-connection");
    return asCanonicalName((label != null) ? label : service.image().getName());
  }

  private String asCanonicalName(String name) {
    return ImageReference.of(name).getName();
  }

}
