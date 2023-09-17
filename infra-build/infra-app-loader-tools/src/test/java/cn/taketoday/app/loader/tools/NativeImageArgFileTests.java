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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for @{link NativeImageArgFile}.
 *
 * @author Moritz Halbritter
 */
class NativeImageArgFileTests {

	@Test
	void writeIfNecessaryWhenHasExcludesWritesLines() {
		NativeImageArgFile argFile = new NativeImageArgFile(List.of("path/to/dependency-1.jar", "dependency-2.jar"));
		List<String> lines = new ArrayList<>();
		argFile.writeIfNecessary(lines::addAll);
		assertThat(lines).containsExactly("--exclude-config", "\\Qdependency-1.jar\\E", "^/META-INF/native-image/.*",
				"--exclude-config", "\\Qdependency-2.jar\\E", "^/META-INF/native-image/.*");
	}

	@Test
	void writeIfNecessaryWhenHasNothingDoesNotCallConsumer() {
		NativeImageArgFile argFile = new NativeImageArgFile(Collections.emptyList());
		argFile.writeIfNecessary((lines) -> fail("Should not be called"));
	}

}
