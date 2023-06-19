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

package cn.taketoday.core.type;

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
class AspectJTypeFilterTests {

  @Test
  void namePatternMatches() throws Exception {
    String type = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass";
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass");
    assertMatch(type, "*");
    assertMatch(type, "*..SomeClass");
    assertMatch(type, "cn.taketoday.core.testfixture.type..SomeClass");
  }

  @Test
  void namePatternNoMatches() throws Exception {
    assertNoMatch("cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass",
            "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassX");
  }

  @Test
  void subclassPatternMatches() throws Exception {
    String type2 = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass";
    assertMatch(type2, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch(type2, "*+");
    assertMatch(type2, "java.lang.Object+");

    String type1 = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface";
    assertMatch(type1, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch(type1, "*+");
    assertMatch(type1, "java.lang.Object+");

    String type = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface";
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch(type, "*+");
    assertMatch(type, "java.lang.Object+");
  }

  @Test
  void subclassPatternNoMatches() throws Exception {
    assertNoMatch("cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "java.lang.String+");
  }

  @Test
  void annotationPatternMatches() throws Exception {
    String type = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent";
    assertMatch(type, "@cn.taketoday.core.testfixture.stereotype.Component *..*");
    assertMatch(type, "@* *..*");
    assertMatch(type, "@*..* *..*");
    assertMatch(type, "@*..*Component *..*");
    assertMatch(type, "@cn.taketoday.core.testfixture.stereotype.Component *..*Component");
    assertMatch(type, "@cn.taketoday.core.testfixture.stereotype.Component *");
  }

  @Test
  void annotationPatternNoMatches() throws Exception {
    assertNoMatch("cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@cn.taketoday.stereotype.Repository *..*");
  }

  @Test
  void compositionPatternMatches() throws Exception {
    assertMatch("cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass", "!*..SomeOtherClass");
    String type = "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface";
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
            "&& cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
            "&& cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch(type, "cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
            "|| cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
            "|| cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
  }

  @Test
  void compositionPatternNoMatches() throws Exception {
    assertNoMatch("cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass",
            "*..Bogus && cn.taketoday.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass");
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
