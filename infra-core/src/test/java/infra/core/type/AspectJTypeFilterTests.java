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

package infra.core.type;

import org.junit.jupiter.api.Test;

import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.core.type.filter.AspectJTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/15 23:03
 */
class AspectJTypeFilterTests {

  @Test
  void namePatternMatches() throws Exception {
    String type = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass";
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass");
    assertMatch(type, "*");
    assertMatch(type, "*..SomeClass");
    assertMatch(type, "infra.core.testfixture.type..SomeClass");
  }

  @Test
  void namePatternNoMatches() throws Exception {
    assertNoMatch("infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass",
            "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassX");
  }

  @Test
  void subclassPatternMatches() throws Exception {
    String type2 = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass";
    assertMatch(type2, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch(type2, "*+");
    assertMatch(type2, "java.lang.Object+");

    String type1 = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface";
    assertMatch(type1, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch(type1, "*+");
    assertMatch(type1, "java.lang.Object+");

    String type = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface";
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+");
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+");
    assertMatch(type, "*+");
    assertMatch(type, "java.lang.Object+");
  }

  @Test
  void subclassPatternNoMatches() throws Exception {
    assertNoMatch("infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
            "java.lang.String+");
  }

  @Test
  void annotationPatternMatches() throws Exception {
    String type = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent";
    assertMatch(type, "@infra.core.testfixture.stereotype.Component *..*");
    assertMatch(type, "@* *..*");
    assertMatch(type, "@*..* *..*");
    assertMatch(type, "@*..*Component *..*");
    assertMatch(type, "@infra.core.testfixture.stereotype.Component *..*Component");
    assertMatch(type, "@infra.core.testfixture.stereotype.Component *");
  }

  @Test
  void annotationPatternNoMatches() throws Exception {
    assertNoMatch("infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
            "@infra.stereotype.Repository *..*");
  }

  @Test
  void compositionPatternMatches() throws Exception {
    assertMatch("infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass", "!*..SomeOtherClass");
    String type = "infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface";
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
            "&& infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
            "&& infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
    assertMatch(type, "infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
            "|| infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
            "|| infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
  }

  @Test
  void compositionPatternNoMatches() throws Exception {
    assertNoMatch("infra.core.testfixture.type.AspectJTypeFilterTestsTypes$SomeClass",
            "*..Bogus && infra.core.testfixture.type.AspectJTypeFilterTestsTypes.SomeClass");
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
