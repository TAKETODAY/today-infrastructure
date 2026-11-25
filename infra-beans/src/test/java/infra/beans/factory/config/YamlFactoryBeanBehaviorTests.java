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

package infra.beans.factory.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import infra.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

class YamlFactoryBeanBehaviorTests {

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void mergesNestedYamlEntriesAndPreservesValueTypes(boolean singleton) {
    var factory = new YamlMapFactoryBean();
    factory.setResources(yaml("service: {port: 8080, enabled: true}"), yaml("service: {port: 8443, secure: true}"));
    factory.setSingleton(singleton);
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(Map.class);
    assertThat(factory.isSingleton()).isEqualTo(singleton);
    var result = factory.getObject();
    assertThat(result).containsOnlyKeys("service").containsEntry("service", Map.of("port", 8443, "enabled", true, "secure", true));
    if (singleton) {
      assertThat(factory.getObject()).isSameAs(result);
    }
    else {
      assertThat(factory.getObject()).isEqualTo(result).isNotSameAs(result);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void flattensYamlListsAndExposesValuesAsStrings(boolean singleton) {
    var factory = new YamlPropertiesFactoryBean();
    factory.setResources(yaml("service: {port: 8080, hosts: [one, two]}"), yaml("service: {port: 8443}"));
    factory.setSingleton(singleton);
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(Properties.class);
    assertThat(factory.isSingleton()).isEqualTo(singleton);
    var result = factory.getObject();
    assertThat(result.getProperty("service.port")).isEqualTo("8443");
    assertThat(result.getProperty("service.hosts[0]")).isEqualTo("one");
    assertThat(result.getProperty("service.hosts[1]")).isEqualTo("two");
    if (singleton) {
      assertThat(factory.getObject()).isSameAs(result);
    }
    else {
      assertThat(factory.getObject()).isEqualTo(result).isNotSameAs(result);
    }
  }

  private static ByteArrayResource yaml(String text) {
    return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
  }

}
