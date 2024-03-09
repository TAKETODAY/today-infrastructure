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
 * Tests for {@link ResourceEditor}.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Dave Syer
 */
class ResourceEditorTests {

  @Test
  void sunnyDay() {
    PropertyEditor editor = new ResourceEditor();
    editor.setAsText("classpath:cn/taketoday/core/io/ResourceEditorTests.class");
    Resource resource = (Resource) editor.getValue();
    assertThat(resource).isNotNull();
    assertThat(resource.exists()).isTrue();
  }

  @Test
  void ctorWithNullCtorArgs() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ResourceEditor(null, null));
  }

  @Test
  void setAndGetAsTextWithNull() {
    PropertyEditor editor = new ResourceEditor();
    editor.setAsText(null);
    assertThat(editor.getAsText()).isEmpty();
  }

  @Test
  void setAndGetAsTextWithWhitespaceResource() {
    PropertyEditor editor = new ResourceEditor();
    editor.setAsText("  ");
    assertThat(editor.getAsText()).isEmpty();
  }

  @Test
  void systemPropertyReplacement() {
    PropertyEditor editor = new ResourceEditor();
    System.setProperty("test.prop", "foo");
    try {
      editor.setAsText("${test.prop}");
      Resource resolved = (Resource) editor.getValue();
      assertThat(resolved.getName()).isEqualTo("foo");
    }
    finally {
      System.clearProperty("test.prop");
    }
  }

  @Test
  void systemPropertyReplacementWithUnresolvablePlaceholder() {
    PropertyEditor editor = new ResourceEditor();
    System.setProperty("test.prop", "foo");
    try {
      editor.setAsText("${test.prop}-${bar}");
      Resource resolved = (Resource) editor.getValue();
      assertThat(resolved.getName()).isEqualTo("foo-${bar}");
    }
    finally {
      System.clearProperty("test.prop");
    }
  }

  @Test
  void strictSystemPropertyReplacementWithUnresolvablePlaceholder() {
    PropertyEditor editor = new ResourceEditor(new DefaultResourceLoader(), new StandardEnvironment(), false);
    System.setProperty("test.prop", "foo");
    try {
      assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() -> {
        editor.setAsText("${test.prop}-${bar}");
        editor.getValue();
      });
    }
    finally {
      System.clearProperty("test.prop");
    }
  }

}
