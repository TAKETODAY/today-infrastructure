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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class Spr7816Tests {

	@Test
	public void spr7816() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("spr7816.xml", getClass());
		FilterAdapter adapter = ctx.getBean(FilterAdapter.class);
		assertThat(adapter.getSupportedTypes().get("Building")).isEqualTo(Building.class);
		assertThat(adapter.getSupportedTypes().get("Entrance")).isEqualTo(Entrance.class);
		assertThat(adapter.getSupportedTypes().get("Dwelling")).isEqualTo(Dwelling.class);
	}

	public static class FilterAdapter {

		private String extensionPrefix;

		private Map<String, Class<? extends DomainEntity>> supportedTypes;

		public FilterAdapter(final String extensionPrefix, final Map<String, Class<? extends DomainEntity>> supportedTypes) {
			this.extensionPrefix = extensionPrefix;
			this.supportedTypes = supportedTypes;
		}

		public String getExtensionPrefix() {
			return extensionPrefix;
		}

		public Map<String, Class<? extends DomainEntity>> getSupportedTypes() {
			return supportedTypes;
		}

	}

	public static class Building extends DomainEntity {
	}

	public static class Entrance extends DomainEntity {
	}

	public static class Dwelling extends DomainEntity {
	}

	public abstract static class DomainEntity {

	}
}
