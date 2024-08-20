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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DelegatingLoggingSystemFactory}.
 *
 * @author Phillip Webb
 */
class DelegatingLoggingSystemFactoryTests {

	private final ClassLoader classLoader = getClass().getClassLoader();

	@Test
	void getLoggingSystemWhenDelegatesFunctionIsNullReturnsNull() {
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory(null);
		assertThat(factory.getLoggingSystem(this.classLoader)).isNull();
	}

	@Test
	void getLoggingSystemWhenDelegatesFunctionReturnsNullReturnsNull() {
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory((cl) -> null);
		assertThat(factory.getLoggingSystem(this.classLoader)).isNull();
	}

	@Test
	void getLoggingSystemReturnsFirstNonNullLoggingSystem() {
		List<LoggingSystemFactory> delegates = new ArrayList<>();
		delegates.add(mock(LoggingSystemFactory.class));
		delegates.add(mock(LoggingSystemFactory.class));
		delegates.add(mock(LoggingSystemFactory.class));
		LoggingSystem result = mock(LoggingSystem.class);
		given(delegates.get(1).getLoggingSystem(this.classLoader)).willReturn(result);
		DelegatingLoggingSystemFactory factory = new DelegatingLoggingSystemFactory((cl) -> delegates);
		assertThat(factory.getLoggingSystem(this.classLoader)).isSameAs(result);
		then(delegates.get(0)).should().getLoggingSystem(this.classLoader);
		then(delegates.get(1)).should().getLoggingSystem(this.classLoader);
		then(delegates.get(2)).shouldHaveNoInteractions();
	}

}
