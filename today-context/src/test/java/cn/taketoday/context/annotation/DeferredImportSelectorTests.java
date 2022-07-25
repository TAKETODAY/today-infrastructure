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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.DeferredImportSelector.Group;
import cn.taketoday.core.type.AnnotationMetadata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeferredImportSelector}.
 *
 * @author Stephane Nicoll
 */
public class DeferredImportSelectorTests {

  @Test
  public void entryEqualsSameInstance() {
    AnnotationMetadata metadata = mock(AnnotationMetadata.class);
    Group.Entry entry = new Group.Entry(metadata, "com.example.Test");
    assertThat(entry).isEqualTo(entry);
  }

  @Test
  public void entryEqualsSameMetadataAndClassName() {
    AnnotationMetadata metadata = mock(AnnotationMetadata.class);
    assertThat(new Group.Entry(metadata, "com.example.Test")).isEqualTo(new Group.Entry(metadata, "com.example.Test"));
  }

  @Test
  public void entryEqualDifferentMetadataAndSameClassName() {
    assertThat(new Group.Entry(mock(AnnotationMetadata.class), "com.example.Test")).isNotEqualTo(new Group.Entry(mock(AnnotationMetadata.class), "com.example.Test"));
  }

  @Test
  public void entryEqualSameMetadataAnDifferentClassName() {
    AnnotationMetadata metadata = mock(AnnotationMetadata.class);
    assertThat(new Group.Entry(metadata, "com.example.AnotherTest")).isNotEqualTo(new Group.Entry(metadata, "com.example.Test"));
  }
}
