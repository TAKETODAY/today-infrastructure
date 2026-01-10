/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 20:52
 */
class ModuleResourceTests {

  private static final String existingPath = "java/beans/Introspector.class";
  private static final String nonExistingPath = "org/example/NonExistingClass.class";

  @Test
  void existingClassFileResource() throws IOException {
    // Check expected behavior of ClassPathResource first.
    ClassPathResource cpr = new ClassPathResource(existingPath);
    assertExistingResource(cpr);
    assertThat(cpr.toString()).startsWith("class path resource").contains(cpr.getPath());

    ModuleResource mr = new ModuleResource(Introspector.class.getModule(), existingPath);
    assertExistingResource(mr);
    assertThat(mr.toString()).startsWith("module resource").contains(mr.getModule().getName(), mr.getPath());
    System.err.println(mr);

    assertThat(mr.getContentAsByteArray()).isEqualTo(cpr.getContentAsByteArray());
    assertThat(mr.contentLength()).isEqualTo(cpr.contentLength());
  }

  private static void assertExistingResource(Resource resource) {
    assertThat(resource.exists()).isTrue();
    assertThat(resource.isReadable()).isTrue();
    assertThat(resource.isOpen()).isFalse();
    assertThat(resource.isFile()).isFalse();
    assertThat(resource.getName()).isEqualTo("Introspector.class");
  }

  @Test
  void nonExistingResource() {
    ModuleResource mr = new ModuleResource(Introspector.class.getModule(), nonExistingPath);
    assertThat(mr.exists()).isFalse();
    assertThat(mr.isReadable()).isFalse();
    assertThat(mr.isOpen()).isFalse();
    assertThat(mr.isFile()).isFalse();
    assertThat(mr.getName()).isEqualTo("NonExistingClass.class");
    assertThat(mr.toString()).contains(mr.getModule().getName());
    assertThat(mr.toString()).contains(mr.getPath());

    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::getContentAsByteArray);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::contentLength);
  }

  @Test
  void equalsAndHashCode() {
    Resource resource1 = new ModuleResource(Introspector.class.getModule(), existingPath);
    Resource resource2 = new ModuleResource(Introspector.class.getModule(), existingPath);
    Resource resource3 = new ModuleResource(Introspector.class.getModule(), nonExistingPath);
    assertThat(resource1).isEqualTo(resource1);
    assertThat(resource1).isEqualTo(resource2);
    assertThat(resource2).isEqualTo(resource1);
    assertThat(resource1).isNotEqualTo(resource3);
    assertThat(resource1).hasSameHashCodeAs(resource2);
    assertThat(resource1).doesNotHaveSameHashCodeAs(resource3);
  }

}