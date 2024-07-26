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

package cn.taketoday.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ResourcePatternHint}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class ResourcePatternHintTests {

	@Test
	void patternWithLeadingSlashIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourcePatternHint("/file.properties", null))
				.withMessage("Resource pattern [/file.properties] must not start with a '/' unless it is the root directory");
	}

	@Test
	void rootDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("/", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("/")
				.rejects("/com/example", "/file.txt");
	}

	@Test
	void fileAtRoot() {
		ResourcePatternHint hint = new ResourcePatternHint("file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("file.properties")
				.rejects("com/example/file.properties", "file.prop", "another-file.properties");
	}

	@Test
	void fileInDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties")
				.rejects("file.properties", "com/file.properties", "com/example/another-file.properties");
	}

	@Test
	void extension() {
		ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("file.properties", "com/example/file.properties")
				.rejects("file.prop", "com/example/file.prop");
	}

	@Test
	void extensionInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties")
				.rejects("file.properties", "com/file.properties");
	}

	@Test
	void anyFileInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties", "com/example/another")
				.rejects("file.properties", "com/file.properties");
	}

}
