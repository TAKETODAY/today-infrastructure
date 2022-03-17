/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.type;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.core.type.filter.AspectJTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/15 23:03
 */
@Disabled
class AspectJTypeFilterTests {

  @Test
  void namePatternMatches() throws Exception {
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "type.AspectJTypeFilterTestsTypes.SomeClass");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "*");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "*..SomeClass");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "type..SomeClass");
  }

  @Test
  void namePatternNoMatches() throws Exception {
    assertNoMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "type.AspectJTypeFilterTestsTypes.SomeClassX");
  }

  @Test
  void subclassPatternMatches() throws Exception {
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "*+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "java.lang.Object+");

    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
            "*+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
            "java.lang.Object+");

    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "*+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "java.lang.Object+");
  }

  @Test
  void subclassPatternNoMatches() throws Exception {
    assertNoMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "java.lang.String+");
  }

  @Test
  void annotationPatternMatches() throws Exception {
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@cn.taketoday.lang.Component *..*");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@* *..*");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@*..* *..*");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@*..*Component *..*");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@cn.taketoday.lang.Component *..*Component");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@cn.taketoday.lang.Component *");
  }

  @Test
  void annotationPatternNoMatches() throws Exception {
    assertNoMatch("type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@cn.taketoday.lang.Repository *..*");
  }

  @Test
  void compositionPatternMatches() throws Exception {
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "!*..SomeOtherClass");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
                    "&& type.AspectJTypeFilterTestsTypes.SomeClass+ " +
                    "&& type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch("type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
            "type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
                    "|| type.AspectJTypeFilterTestsTypes.SomeClass+ " +
                    "|| type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
  }

  @Test
  void compositionPatternNoMatches() throws Exception {
    assertNoMatch("type.AspectJTypeFilterTestsTypes$SomeClass",
            "*..Bogus && type.AspectJTypeFilterTestsTypes.SomeClass");
  }

  private void assertMatch(String type, String typePattern) throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

    AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(type);
  }

  private void assertNoMatch(String type, String typePattern) throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);

    AspectJTypeFilter filter = new AspectJTypeFilter(typePattern, getClass().getClassLoader());
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
    ClassloadingAssertions.assertClassNotLoaded(type);
  }

}
