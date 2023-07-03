/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io;

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