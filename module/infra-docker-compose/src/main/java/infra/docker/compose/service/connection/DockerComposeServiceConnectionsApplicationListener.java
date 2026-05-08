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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContext;
import infra.context.ApplicationListener;
import infra.context.container.ContainerImageMetadata;
import infra.context.service.connection.ConnectionDetails;
import infra.context.service.connection.ConnectionDetailsFactories;
import infra.core.env.Environment;
import infra.docker.compose.core.RunningService;
import infra.docker.compose.lifecycle.DockerComposeServicesReadyEvent;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * {@link ApplicationListener} that listens for an {@link DockerComposeServicesReadyEvent}
 * in order to establish service connections.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeServiceConnectionsApplicationListener implements ApplicationListener<DockerComposeServicesReadyEvent> {

  private final ConnectionDetailsFactories factories;

  public DockerComposeServiceConnectionsApplicationListener() {
    this(new ConnectionDetailsFactories(null));
  }

  DockerComposeServiceConnectionsApplicationListener(ConnectionDetailsFactories factories) {
    this.factories = factories;
  }

  @Override
  public void onApplicationEvent(DockerComposeServicesReadyEvent event) {
    ApplicationContext applicationContext = event.getSource();
    if (applicationContext instanceof BeanDefinitionRegistry registry) {
      Environment environment = applicationContext.getEnvironment();
      registerConnectionDetails(registry, environment, event.getRunningServices());
    }
  }

  private void registerConnectionDetails(BeanDefinitionRegistry registry, Environment environment,
          List<RunningService> runningServices) {
    for (RunningService runningService : runningServices) {
      DockerComposeConnectionSource source = new DockerComposeConnectionSource(runningService, environment);
      this.factories.getConnectionDetails(source, false).forEach((connectionDetailsType, connectionDetails) -> {
        register(registry, runningService, connectionDetailsType, connectionDetails);
        this.factories.getConnectionDetails(connectionDetails, false)
                .forEach((adaptedType, adaptedDetails) -> register(registry, runningService, adaptedType,
                        adaptedDetails));
      });
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void register(BeanDefinitionRegistry registry, RunningService runningService,
          Class<?> connectionDetailsType, ConnectionDetails connectionDetails) {
    ContainerImageMetadata containerMetadata = new ContainerImageMetadata(runningService.image().toString());
    String beanName = getBeanName(runningService, connectionDetailsType);
    Class<T> beanType = (Class<T>) connectionDetails.getClass();
    Supplier<T> beanSupplier = () -> (T) connectionDetails;
    RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType, beanSupplier);
    containerMetadata.addTo(beanDefinition);
    registry.registerBeanDefinition(beanName, beanDefinition);
  }

  private String getBeanName(RunningService runningService, Class<?> connectionDetailsType) {
    List<String> parts = new ArrayList<>();
    parts.add(ClassUtils.getShortNameAsProperty(connectionDetailsType));
    parts.add("for");
    parts.addAll(Arrays.asList(runningService.name().split("-")));
    return StringUtils.uncapitalize(parts.stream().map(StringUtils::capitalize).collect(Collectors.joining()));
  }

}
