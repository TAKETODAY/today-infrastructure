/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditor;

import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 11:21
 */
class ResourceArrayPropertyEditorTests {

  @Test
  void vanillaResource() {
    PropertyEditor editor = new ResourceArrayPropertyEditor();
    editor.setAsText("classpath:cn/taketoday/core/io/ResourceArrayPropertyEditor.class");
    Resource[] resources = (Resource[]) editor.getValue();
    assertThat(resources).isNotNull();
    assertThat(resources[0].exists()).isTrue();
  }

  @Test
  void patternResource() {
    // N.B. this will sometimes fail if you use classpath: instead of classpath*:.
    // The result depends on the classpath - if test-classes are segregated from classes
    // and they come first on the classpath (like in Maven) then it breaks, if classes
    // comes first (like in Infra Build) then it is OK.
    PropertyEditor editor = new ResourceArrayPropertyEditor();
    editor.setAsText("classpath*:cn/taketoday/core/io/Resource*Editor.class");
    Resource[] resources = (Resource[]) editor.getValue();
    assertThat(resources).isNotNull();
    assertThat(resources[0].exists()).isTrue();
  }

  @Test
  void systemPropertyReplacement() {
    PropertyEditor editor = new ResourceArrayPropertyEditor();
    System.setProperty("test.prop", "foo");
    try {
      editor.setAsText("${test.prop}");
      Resource[] resources = (Resource[]) editor.getValue();
      assertThat(resources[0].getName()).isEqualTo("foo");
    }
    finally {
      System.clearProperty("test.prop");
    }
  }

  @Test
  void strictSystemPropertyReplacementWithUnresolvablePlaceholder() {
    PropertyEditor editor = new ResourceArrayPropertyEditor(
            new PathMatchingPatternResourceLoader(), new StandardEnvironment(),
            false);
    System.setProperty("test.prop", "foo");
    try {
      assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() ->
              editor.setAsText("${test.prop}-${bar}"));
    }
    finally {
      System.clearProperty("test.prop");
    }
  }

  @Test
  void commaDelimitedResourcesWithSingleResource() {
    PropertyEditor editor = new ResourceArrayPropertyEditor();
    editor.setAsText("classpath:cn/taketoday/core/io/ResourceArrayPropertyEditor.class,     file:/test.txt");
    Resource[] resources = (Resource[]) editor.getValue();
    assertThat(resources).isNotNull();
    assertThat(resources[0]).isInstanceOfSatisfying(ClassPathResource.class,
            resource -> assertThat(resource.exists()).isTrue());
    assertThat(resources[1]).isInstanceOfSatisfying(FileUrlResource.class,
            resource -> assertThat(resource.getName()).isEqualTo("test.txt"));
  }

  @Test
  void commaDelimitedResourcesWithMultipleResources() {
    PropertyEditor editor = new ResourceArrayPropertyEditor();
    editor.setAsText("file:/test.txt, classpath:cn/taketoday/core/io/test-resources/*.txt");
    Resource[] resources = (Resource[]) editor.getValue();
    assertThat(resources).isNotNull();
    assertThat(resources[0]).isInstanceOfSatisfying(FileUrlResource.class,
            resource -> assertThat(resource.getName()).isEqualTo("test.txt"));

    assertThat(resources).anySatisfy(candidate ->
            assertThat(candidate.getName()).isEqualTo("resource1.txt"));

    assertThat(resources).anySatisfy(candidate ->
            assertThat(candidate.getName()).isEqualTo("resource2.txt"));
    assertThat(resources).hasSize(3);
  }

}
