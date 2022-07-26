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

package cn.taketoday.core.io;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/26 23:30
 */
class ClassPathResourceTests {

  private static final String PACKAGE_PATH = "cn/taketoday/core/io";
  private static final String NONEXISTENT_RESOURCE_NAME = "nonexistent.xml";
  private static final String FQ_RESOURCE_PATH = PACKAGE_PATH + '/' + NONEXISTENT_RESOURCE_NAME;

  /**
   * Absolute path version of {@link #FQ_RESOURCE_PATH}.
   */
  private static final String FQ_RESOURCE_PATH_WITH_LEADING_SLASH = '/' + FQ_RESOURCE_PATH;

  private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^class path resource \\[(.+?)]$");


  @Test
  void stringConstructorRaisesExceptionWithFullyQualifiedPath() {
    assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH));
  }

  @Test
  void classLiteralConstructorRaisesExceptionWithFullyQualifiedPath() {
    assertExceptionContainsFullyQualifiedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
  }

  @Test
  void classLoaderConstructorRaisesExceptionWithFullyQualifiedPath() {
    assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH, getClass().getClassLoader()));
  }

  @Test
  void getDescriptionWithStringConstructor() {
    assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH), FQ_RESOURCE_PATH);
  }

  @Test
  void getDescriptionWithStringConstructorAndLeadingSlash() {
    assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH),
            FQ_RESOURCE_PATH);
  }

  @Test
  void getDescriptionWithClassLiteralConstructor() {
    assertDescriptionContainsExpectedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()),
            FQ_RESOURCE_PATH);
  }

  @Test
  void getDescriptionWithClassLiteralConstructorAndLeadingSlash() {
    assertDescriptionContainsExpectedPath(
            new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH, getClass()), FQ_RESOURCE_PATH);
  }

  @Test
  void getDescriptionWithClassLoaderConstructor() {
    assertDescriptionContainsExpectedPath(
            new ClassPathResource(FQ_RESOURCE_PATH, getClass().getClassLoader()), FQ_RESOURCE_PATH);
  }

  @Test
  void getDescriptionWithClassLoaderConstructorAndLeadingSlash() {
    assertDescriptionContainsExpectedPath(
            new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH, getClass().getClassLoader()), FQ_RESOURCE_PATH);
  }

  @Test
  void dropLeadingSlashForClassLoaderAccess() {
    assertThat(new ClassPathResource("/test.html").getPath()).isEqualTo("test.html");
    assertThat(((ClassPathResource) new ClassPathResource("").createRelative("/test.html")).getPath()).isEqualTo("test.html");
  }

  @Test
  void preserveLeadingSlashForClassRelativeAccess() {
    assertThat(new ClassPathResource("/test.html", getClass()).getPath()).isEqualTo("/test.html");
    assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("/test.html")).getPath()).isEqualTo("/test.html");
  }

  @Test
  void directoryNotReadable() {
    Resource fileDir = new ClassPathResource("cn/taketoday/core");
    assertThat(fileDir.exists()).isTrue();
    assertThat(fileDir.isReadable()).isFalse();

    Resource jarDir = new ClassPathResource("reactor/core");
    assertThat(jarDir.exists()).isTrue();
    assertThat(jarDir.isReadable()).isFalse();
  }


  private void assertDescriptionContainsExpectedPath(ClassPathResource resource, String expectedPath) {
    Matcher matcher = DESCRIPTION_PATTERN.matcher(resource.toString());
    assertThat(matcher.matches()).isTrue();
    assertThat(matcher.groupCount()).isEqualTo(1);
    String match = matcher.group(1);

    assertThat(match).isEqualTo(expectedPath);
  }

  private void assertExceptionContainsFullyQualifiedPath(ClassPathResource resource) {
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
                    resource::getInputStream)
            .withMessageContaining(FQ_RESOURCE_PATH);
  }

}
