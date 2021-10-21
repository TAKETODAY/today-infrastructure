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

import org.junit.jupiter.api.Test;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.core.type.filter.AspectJTypeFilter;

import example.type.AspectJTypeFilterTestsTypes;

/**
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see AspectJTypeFilterTestsTypes
 */
class AspectJTypeFilterTests {

	@Test
	void namePatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..SomeClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example..SomeClass");
	}

	@Test
	void namePatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClassX");
	}

	@Test
	void subclassPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.Object+");

		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassImplementingSomeInterface",
				"java.lang.Object+");

		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"*+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"java.lang.Object+");
	}

	@Test
	void subclassPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClass",
				"java.lang.String+");
	}

	@Test
	void annotationPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@cn.taketoday.core.testfixture.stereotype.Component *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@* *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..* *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@*..*Component *..*");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@cn.taketoday.core.testfixture.stereotype.Component *..*Component");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@cn.taketoday.core.testfixture.stereotype.Component *");
	}

	@Test
	void annotationPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassAnnotatedWithComponent",
				"@cn.taketoday.stereotype.Repository *..*");
	}

	@Test
	void compositionPatternMatches() throws Exception {
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"!*..SomeOtherClass");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"&& example.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"&& example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
		assertMatch("example.type.AspectJTypeFilterTestsTypes$SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface",
				"example.type.AspectJTypeFilterTestsTypes.SomeInterface+ " +
						"|| example.type.AspectJTypeFilterTestsTypes.SomeClass+ " +
						"|| example.type.AspectJTypeFilterTestsTypes.SomeClassExtendingSomeClass+");
	}

	@Test
	void compositionPatternNoMatches() throws Exception {
		assertNoMatch("example.type.AspectJTypeFilterTestsTypes$SomeClass",
				"*..Bogus && example.type.AspectJTypeFilterTestsTypes.SomeClass");
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
