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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoaderTests {

	private TestConfigDataLoader loader = new TestConfigDataLoader();

	private ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void isLoadableAlwaysReturnsTrue() {
		assertThat(this.loader.isLoadable(this.context, new TestConfigDataResource())).isTrue();
	}

	static class TestConfigDataLoader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource resource) throws IOException {
			return null;
		}

	}

	static class TestConfigDataResource extends ConfigDataResource {

	}

}
