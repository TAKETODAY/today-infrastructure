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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import infra.core.testfixture.type.AssignableTypeFilterTestsTypes;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.core.type.filter.AssignableTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 */
@Order(2)
class AssignableTypeFilterTests {

  @Test
  void directMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AssignableTypeFilterTestsTypes$TestNonInheritingClass";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter matchingFilter = new AssignableTypeFilter(AssignableTypeFilterTestsTypes.TestNonInheritingClass.class);
    AssignableTypeFilter notMatchingFilter = new AssignableTypeFilter(AssignableTypeFilterTestsTypes.TestInterface.class);
    assertThat(notMatchingFilter.match(metadataReader, metadataReaderFactory)).isFalse();
    assertThat(matchingFilter.match(metadataReader, metadataReaderFactory)).isTrue();
  }

  @Test
  void interfaceMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AssignableTypeFilterTestsTypes$TestInterfaceImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(AssignableTypeFilterTestsTypes.TestInterface.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void superClassMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(AssignableTypeFilterTestsTypes.SimpleJdbcDaoSupport.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void interfaceThroughSuperClassMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "infra.core.testfixture.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(AssignableTypeFilterTestsTypes.JdbcDaoSupport.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

}
