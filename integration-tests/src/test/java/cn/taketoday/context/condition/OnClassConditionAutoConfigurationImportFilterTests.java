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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.support.DefaultListableBeanFactory;
import cn.taketoday.framework.autoconfigure.AutoConfigurationImportFilter;
import cn.taketoday.framework.autoconfigure.AutoConfigurationMetadata;
import cn.taketoday.core.io.support.SpringFactoriesLoader;

import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.context.condition.OnClassCondition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link AutoConfigurationImportFilter} part of {@link OnClassCondition}.
 *
 * @author Phillip Webb
 */
class OnClassConditionAutoConfigurationImportFilterTests {

	private OnClassCondition filter = new OnClassCondition();

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@BeforeEach
	void setup() {
		this.filter.setBeanClassLoader(getClass().getClassLoader());
		this.filter.setBeanFactory(this.beanFactory);
	}

	@Test
	void shouldBeRegistered() {
		assertThat(SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, null))
				.hasAtLeastOneElementOfType(OnClassCondition.class);
	}

	@Test
	void matchShouldMatchClasses() {
		String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
		boolean[] result = this.filter.match(autoConfigurationClasses, getAutoConfigurationMetadata());
		assertThat(result).containsExactly(true, false);
	}

	@Test
	void matchShouldRecordOutcome() {
		String[] autoConfigurationClasses = new String[] { "test.match", "test.nomatch" };
		this.filter.match(autoConfigurationClasses, getAutoConfigurationMetadata());
		ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
		assertThat(report.getConditionAndOutcomesBySource()).hasSize(1).containsKey("test.nomatch");
	}

	private AutoConfigurationMetadata getAutoConfigurationMetadata() {
		AutoConfigurationMetadata metadata = mock(AutoConfigurationMetadata.class);
		given(metadata.wasProcessed("test.match")).willReturn(true);
		given(metadata.get("test.match", "ConditionalOnClass")).willReturn("java.io.InputStream");
		given(metadata.wasProcessed("test.nomatch")).willReturn(true);
		given(metadata.get("test.nomatch", "ConditionalOnClass")).willReturn("java.io.DoesNotExist");
		return metadata;
	}

}
