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

package cn.taketoday.framework.logging.structured;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.logging.structured.CommonStructuredLogFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonStructuredLogFormat}.
 *
 * @author Phillip Webb
 */
class CommonStructuredLogFormatTests {

	@Test
	void forIdReturnsCommonStructuredLogFormat() {
		assertThat(CommonStructuredLogFormat.forId("ecs")).isEqualTo(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA);
		assertThat(CommonStructuredLogFormat.forId("logstash")).isEqualTo(CommonStructuredLogFormat.LOGSTASH);
	}

	@Test
	void forIdWhenIdIsInDifferentCaseReturnsCommonStructuredLogFormat() {
		assertThat(CommonStructuredLogFormat.forId("ECS")).isEqualTo(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA);
		assertThat(CommonStructuredLogFormat.forId("logSTAsh")).isEqualTo(CommonStructuredLogFormat.LOGSTASH);
	}

	@Test
	void forIdWhenNotKnownReturnsNull() {
		assertThat(CommonStructuredLogFormat.forId("madeup")).isNull();
	}

}
