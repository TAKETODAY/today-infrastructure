/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author pwebb
 */
class JarUriTests {

  @Test
  void describeBootInfClassesUri() {
    JarUri uri = JarUri.from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar"
            + "!/BOOT-INF/classes!/application.properties");
    assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar");
  }

  @Test
  void describeBootInfLibUri() {
    JarUri uri = JarUri.from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar"
            + "!/BOOT-INF/lib/nested.jar!/application.properties");
    assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/nested.jar");
  }

  @Test
  void describeRegularJar() {
    JarUri uri = JarUri
            .from("jar:file:/home/user/project/target/project-0.0.1-SNAPSHOT.jar!/application.properties");
    assertThat(uri.getDescription()).isEqualTo("project-0.0.1-SNAPSHOT.jar");
  }

  @Test
  void getDescriptionMergedWithExisting() {
    JarUri uri = JarUri.from("jar:file:/project-0.0.1-SNAPSHOT.jar!/application.properties");
    assertThat(uri.getDescription("classpath: [application.properties]"))
            .isEqualTo("classpath: [application.properties] from project-0.0.1-SNAPSHOT.jar");
  }

}
