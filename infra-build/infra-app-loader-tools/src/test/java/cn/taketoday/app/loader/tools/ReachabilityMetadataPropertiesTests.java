/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReachabilityMetadataProperties}.
 *
 * @author Moritz Halbritter
 */
class ReachabilityMetadataPropertiesTests {

	@Test
	void shouldReadFromInputStream() throws IOException {
		String propertiesContent = "override=true\n";
		ReachabilityMetadataProperties properties = ReachabilityMetadataProperties
			.fromInputStream(new ByteArrayInputStream(propertiesContent.getBytes(StandardCharsets.UTF_8)));
		assertThat(properties.isOverridden()).isTrue();
	}

	@Test
	void shouldFormatLocation() {
		String location = ReachabilityMetadataProperties
			.getLocation(LibraryCoordinates.of("group-id", "artifact-id", "1.0.0"));
		assertThat(location)
			.isEqualTo("META-INF/native-image/group-id/artifact-id/1.0.0/reachability-metadata.properties");
	}

}
