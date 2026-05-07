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

package infra.testcontainers.service.connection;

import org.jspecify.annotations.Nullable;
import infra.testcontainers.beans.TestcontainerBeanDefinition;
import org.testcontainers.containers.Container;

import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.BootstrapContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.service.connection.ConnectionDetailsFactories;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.lang.Assert;
import infra.origin.Origin;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServiceConnectionAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Daeho Kwon
 */
class ServiceConnectionAutoConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

  private final BeanFactory beanFactory;

  ServiceConnectionAutoConfigurationRegistrar(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    if (this.beanFactory instanceof ConfigurableBeanFactory listableBeanFactory) {
      registerBeanDefinitions(listableBeanFactory, context.getRegistry());
    }
  }

  private void registerBeanDefinitions(ConfigurableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
    ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory,
            new ConnectionDetailsFactories(null));
    for (String beanName : beanFactory.getBeanNamesForType(Container.class)) {
      BeanDefinition beanDefinition = getBeanDefinition(beanFactory, beanName);
      MergedAnnotations annotations = getAnnotations(beanDefinition);
      for (ServiceConnection serviceConnection : getServiceConnections(beanFactory, beanName, annotations)) {
        ContainerConnectionSource<?> source = createSource(beanFactory, beanName, beanDefinition, annotations,
                serviceConnection);
        registrar.registerBeanDefinitions(registry, source);
      }
    }
  }

  private Set<ServiceConnection> getServiceConnections(ConfigurableBeanFactory beanFactory, String beanName,
          @Nullable MergedAnnotations annotations) {
    Set<ServiceConnection> serviceConnections = beanFactory.findAllAnnotationsOnBean(beanName,
            ServiceConnection.class, false);
    if (annotations != null) {
      serviceConnections = new LinkedHashSet<>(serviceConnections);
      annotations.stream(ServiceConnection.class)
              .map(MergedAnnotation::synthesize)
              .forEach(serviceConnections::add);
    }
    return serviceConnections;
  }

  private @Nullable BeanDefinition getBeanDefinition(ConfigurableBeanFactory beanFactory, String beanName) {
    try {
      return beanFactory.getBeanDefinition(beanName);
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

  private @Nullable MergedAnnotations getAnnotations(@Nullable BeanDefinition beanDefinition) {
    if (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition) {
      return testcontainerBeanDefinition.getAnnotations();
    }
    if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
      MethodMetadata metadata = annotatedBeanDefinition.getFactoryMethodMetadata();
      return (metadata != null) ? metadata.getAnnotations() : null;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private <C extends Container<?>> ContainerConnectionSource<C> createSource(
          ConfigurableBeanFactory beanFactory, String beanName, @Nullable BeanDefinition beanDefinition,
          @Nullable MergedAnnotations annotations, ServiceConnection serviceConnection) {
    Origin origin = new BeanOrigin(beanName, beanDefinition);
    Class<C> containerType = (Class<C>) beanFactory.getType(beanName, false);
    String containerImageName = (beanDefinition instanceof TestcontainerBeanDefinition testcontainerBeanDefinition)
            ? testcontainerBeanDefinition.getContainerImageName() : null;
    Assert.state(containerType != null, "'containerType' is required");
    return new ContainerConnectionSource<>(beanName, origin, containerType, containerImageName, serviceConnection,
            () -> beanFactory.getBean(beanName, containerType),
            SslBundleSource.get(beanFactory, beanName, annotations), annotations);
  }

}
