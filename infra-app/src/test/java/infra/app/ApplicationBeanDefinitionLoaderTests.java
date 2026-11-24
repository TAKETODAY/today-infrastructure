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

package infra.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import infra.beans.factory.support.StandardBeanFactory;
import infra.core.env.MapPropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.FileSystemResource;
import infra.core.io.PatternResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ApplicationBeanDefinitionLoaderTests {

  @Test
  void resolvesClassNamePlaceholdersAndSkipsIneligibleTypes() {
    var registry = new StandardBeanFactory();
    var environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("source", Sample.class.getName())));
    var loader = new ApplicationBeanDefinitionLoader(registry, "${source}", Runnable.class, new Object() { }.getClass());
    loader.setEnvironment(environment);

    loader.load();

    assertThat(registry.getBeanNamesForType(Sample.class)).hasSize(1);
    assertThat(registry.getBeanNamesForType(Runnable.class)).isEmpty();
  }

  @Test
  void loadsXmlResourcesAndIgnoresNonXmlResources(@TempDir Path directory) throws IOException {
    Path xml = writeBeans(directory.resolve("beans.xml"));
    Path text = Files.writeString(directory.resolve("ignored.txt"), "not XML");
    var registry = new StandardBeanFactory();

    new ApplicationBeanDefinitionLoader(registry, new FileSystemResource(xml), new FileSystemResource(text)).load();

    assertThat(registry.getBean("xmlBean")).isEqualTo("from XML");
  }

  @Test
  void loadsXmlUsingANonPatternResourceLoader(@TempDir Path directory) throws IOException {
    Path xml = writeBeans(directory.resolve("beans.xml"));
    var registry = new StandardBeanFactory();
    var loader = new ApplicationBeanDefinitionLoader(registry, xml.toUri().toString());
    loader.setResourceLoader(new DefaultResourceLoader());

    loader.load();

    assertThat(registry.getBean("xmlBean")).isEqualTo("from XML");
  }

  @Test
  void rejectsUnsupportedSourceTypesAndUnknownLocations() {
    var registry = new StandardBeanFactory();

    assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationBeanDefinitionLoader(registry, 42).load())
            .withMessageContaining("Invalid source type").withMessageContaining(Integer.class.getName());
    assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationBeanDefinitionLoader(registry, "missing.Source").load())
            .withMessage("Invalid source 'missing.Source'");
  }

  @Test
  void identifiesTheSourceWhenResourceResolutionFails() throws IOException {
    var loader = new ApplicationBeanDefinitionLoader(new StandardBeanFactory(), "unreadable:beans.xml");
    PatternResourceLoader resources = mock(PatternResourceLoader.class);
    given(resources.getResources("unreadable:beans.xml")).willThrow(new IOException("Cannot read"));
    loader.setResourceLoader(resources);

    assertThatIllegalStateException().isThrownBy(loader::load).withMessage("Error reading source 'unreadable:beans.xml'");
  }

  private Path writeBeans(Path file) throws IOException {
    return Files.writeString(file, """
            <beans xmlns="http://www.springframework.org/schema/beans"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd">
              <bean id="xmlBean" class="java.lang.String">
                <constructor-arg value="from XML"/>
              </bean>
            </beans>
            """);
  }

  static class Sample { }

}
