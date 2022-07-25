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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.core.type.filter.AssignableTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 */
@Order(2)
@Disabled
class AssignableTypeFilterTests {

  @Test
  void directMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes$TestNonInheritingClass";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter matchingFilter = new AssignableTypeFilter(cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes.TestNonInheritingClass.class);
    AssignableTypeFilter notMatchingFilter = new AssignableTypeFilter(cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes.TestInterface.class);
    assertThat(notMatchingFilter.match(metadataReader, metadataReaderFactory)).isFalse();
    assertThat(matchingFilter.match(metadataReader, metadataReaderFactory)).isTrue();
  }

  @Test
  void interfaceMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes$TestInterfaceImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes.TestInterface.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void superClassMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes.SimpleJdbcDaoSupport.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

  @Test
  void interfaceThroughSuperClassMatch() throws Exception {
    MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
    String classUnderTest = "cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes$SomeDaoLikeImpl";
    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classUnderTest);

    AssignableTypeFilter filter = new AssignableTypeFilter(cn.taketoday.core.testfixture.type.AssignableTypeFilterTestsTypes.JdbcDaoSupport.class);
    assertThat(filter.match(metadataReader, metadataReaderFactory)).isTrue();
    ClassloadingAssertions.assertClassNotLoaded(classUnderTest);
  }

}
