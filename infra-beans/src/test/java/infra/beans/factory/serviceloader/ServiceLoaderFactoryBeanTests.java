/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.serviceloader;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ServiceLoaderFactoryBeanTests {

  @Test
  void exposesServiceLoaderForConfiguredType() throws Exception {
    var factory = new ServiceLoaderFactoryBean();
    factory.setServiceType(SampleService.class);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(ServiceLoader.class);
    assertThat((ServiceLoader<?>) factory.getObject())
            .hasExactlyElementsOfTypes(FirstProvider.class, SecondProvider.class);
  }

  @Test
  void exposesFirstProviderAsSingleton() throws Exception {
    var factory = new ServiceFactoryBean();
    assertThat(factory.getObjectType()).isNull();

    factory.setServiceType(SampleService.class);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(SampleService.class);
    Object service = factory.getObject();
    assertThat(service).isExactlyInstanceOf(FirstProvider.class);
    assertThat(factory.getObject()).isSameAs(service);
  }

  @Test
  void exposesAllProvidersInDeclarationOrder() throws Exception {
    var factory = new ServiceListFactoryBean();
    factory.setServiceType(SampleService.class);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(List.class);
    assertThat((List<?>) factory.getObject())
            .hasExactlyElementsOfTypes(FirstProvider.class, SecondProvider.class);
  }

  @Test
  void rejectsMissingServiceType() {
    var factory = new ServiceLoaderFactoryBean();

    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet)
            .withMessageContaining("serviceType");
  }

  @Test
  void singleServiceLookupFailsWithoutProviders() {
    var factory = new ServiceFactoryBean();
    factory.setServiceType(UnregisteredService.class);
    factory.setBeanClassLoader(getClass().getClassLoader());

    assertThatIllegalStateException().isThrownBy(factory::afterPropertiesSet)
            .withMessageContaining(UnregisteredService.class.getName());
  }

  @Test
  void listLookupWithoutProvidersReturnsEmptyList() throws Exception {
    var factory = new ServiceListFactoryBean();
    factory.setServiceType(UnregisteredService.class);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();

    assertThat((List<?>) factory.getObject()).isEmpty();
  }

  public interface SampleService {
  }

  public static class FirstProvider implements SampleService {
  }

  public static class SecondProvider implements SampleService {
  }

  interface UnregisteredService {
  }

}
