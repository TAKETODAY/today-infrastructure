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

package cn.taketoday.test.context;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MutablePropertySources;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Integration tests for {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
@JUnitConfig
@TestPropertySource(properties = "test.container.ip: test")
@TestInstance(PER_CLASS)
@DisplayName("@DynamicPropertySource integration tests")
class DynamicPropertySourceIntegrationTests {

	private static final String TEST_CONTAINER_IP = "test.container.ip";

	static {
		System.setProperty(TEST_CONTAINER_IP, "system");
	}

	static DemoContainer container = new DemoContainer();

	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add(TEST_CONTAINER_IP, container::getIpAddress);
		registry.add("test.container.port", container::getPort);
	}


	@AfterAll
	void clearSystemProperty() {
		System.clearProperty(TEST_CONTAINER_IP);
	}

	@Test
	@DisplayName("@DynamicPropertySource overrides @TestPropertySource and JVM system property")
	void dynamicPropertySourceOverridesTestPropertySourceAndSystemProperty(@Autowired ConfigurableEnvironment env) {
		MutablePropertySources propertySources = env.getPropertySources();
		assertThat(propertySources.size()).isGreaterThanOrEqualTo(4);
		assertThat(propertySources.contains("Dynamic Test Properties")).isTrue();
		assertThat(propertySources.contains("Inlined Test Properties")).isTrue();
		assertThat(propertySources.contains("systemProperties")).isTrue();
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(TEST_CONTAINER_IP)).isEqualTo("127.0.0.1");
		assertThat(propertySources.get("Inlined Test Properties").getProperty(TEST_CONTAINER_IP)).isEqualTo("test");
		assertThat(propertySources.get("systemProperties").getProperty(TEST_CONTAINER_IP)).isEqualTo("system");
		assertThat(env.getProperty(TEST_CONTAINER_IP)).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("@Service has values injected from @DynamicPropertySource")
	void serviceHasInjectedValues(@Autowired Service service) {
		assertThat(service.getIp()).isEqualTo("127.0.0.1");
		assertThat(service.getPort()).isEqualTo(4242);
	}


	@Configuration
	@Import(Service.class)
	static class Config {
	}

	static class Service {

		private final String ip;

		private final int port;


		Service(@Value("${test.container.ip}") String ip, @Value("${test.container.port}") int port) {
			this.ip = ip;
			this.port = port;
		}

		String getIp() {
			return this.ip;
		}

		int getPort() {
			return this.port;
		}

	}

	static class DemoContainer {

		String getIpAddress() {
			return "127.0.0.1";
		}

		int getPort() {
			return 4242;
		}

	}

}
