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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggerGroups}
 *
 * @author HaiTao Zhang
 * @author Madhura Bhave
 */
class LoggerGroupsTests {

	@Test
	void putAllShouldAddLoggerGroups() {
		Map<String, List<String>> groups = Collections.singletonMap("test",
				Arrays.asList("test.member", "test.member2"));
		LoggerGroups loggerGroups = new LoggerGroups();
		loggerGroups.putAll(groups);
		LoggerGroup group = loggerGroups.get("test");
		assertThat(group.getMembers()).containsExactly("test.member", "test.member2");
	}

	@Test
	void iteratorShouldReturnLoggerGroups() {
		LoggerGroups groups = createLoggerGroups();
		assertThat(groups).hasSize(3);
		assertThat(groups).extracting("name").containsExactlyInAnyOrder("test0", "test1", "test2");
	}

	private LoggerGroups createLoggerGroups() {
		Map<String, List<String>> groups = new LinkedHashMap<>();
		groups.put("test0", Arrays.asList("test0.member", "test0.member2"));
		groups.put("test1", Arrays.asList("test1.member", "test1.member2"));
		groups.put("test2", Arrays.asList("test2.member", "test2.member2"));
		return new LoggerGroups(groups);
	}

}
