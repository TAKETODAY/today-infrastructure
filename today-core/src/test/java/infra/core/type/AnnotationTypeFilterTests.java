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

package infra.core.type;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import infra.core.testfixture.stereotype.Component;
import infra.core.testfixture.type.InheritedAnnotation;
import infra.core.testfixture.type.NonInheritedAnnotation;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 */
@Order(1)
class AnnotationTypeFilterTests {

  @Test
  void directAnnotationMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeComponent";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void inheritedAnnotationFromInterfaceDoesNotMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeClassWithSomeComponentInterface";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
    // Must fail as annotation on interfaces should not be considered a match
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void inheritedAnnotationFromBaseClassDoesMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeSubclassOfSomeComponent";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void nonInheritedAnnotationDoesNotMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeSubclassOfSomeClassMarkedWithNonInheritedAnnotation";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(NonInheritedAnnotation.class);
    // Must fail as annotation isn't inherited
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void nonAnnotatedClassDoesntMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeNonCandidateClass";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(Component.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isFalse();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void matchesInterfacesIfConfigured() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AnnotationTypeFilterTestsTypes$SomeClassWithSomeComponentInterface";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AnnotationTypeFilter filter = new AnnotationTypeFilter(InheritedAnnotation.class, false, true);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

}
