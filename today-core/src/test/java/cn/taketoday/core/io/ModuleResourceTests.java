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

  @Test
  void existingResource() throws IOException {
    ModuleResource mr = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
    assertThat(mr.exists()).isTrue();
    assertThat(mr.isReadable()).isTrue();
    assertThat(mr.isOpen()).isFalse();
    assertThat(mr.isFile()).isFalse();
    assertThat(mr.getName()).isEqualTo("Introspector.class");
    assertThat(mr.toString()).contains(mr.getModule().getName());
    assertThat(mr.toString()).contains(mr.getPath());

    Resource cpr = new ClassPathResource("java/beans/Introspector.class");
    assertThat(mr.getContentAsByteArray()).isEqualTo(cpr.getContentAsByteArray());
    assertThat(mr.contentLength()).isEqualTo(cpr.contentLength());
  }

  @Test
  void nonExistingResource() {
    ModuleResource mr = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspecter.class");
    assertThat(mr.exists()).isFalse();
    assertThat(mr.isReadable()).isFalse();
    assertThat(mr.isOpen()).isFalse();
    assertThat(mr.isFile()).isFalse();
    assertThat(mr.getName()).isEqualTo("Introspecter.class");
    assertThat(mr.toString()).contains(mr.getModule().getName());
    assertThat(mr.toString()).contains(mr.getPath());

    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::getContentAsByteArray);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::contentLength);
  }

  @Test
  void equalsAndHashCode() {
    Resource mr1 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
    Resource mr2 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
    Resource mr3 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspecter.class");
    assertThat(mr1).isEqualTo(mr2);
    assertThat(mr1).isNotEqualTo(mr3);
    assertThat(mr1).hasSameHashCodeAs(mr2);
    assertThat(mr1).doesNotHaveSameHashCodeAs(mr3);
  }

}