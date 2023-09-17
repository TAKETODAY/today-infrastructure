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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class FileUtilsTests {

	@TempDir
	File tempDir;

	private File outputDirectory;

	private File originDirectory;

	@BeforeEach
	void init() {
		this.outputDirectory = new File(this.tempDir, "remove");
		this.originDirectory = new File(this.tempDir, "keep");
		this.outputDirectory.mkdirs();
		this.originDirectory.mkdirs();
	}

	@Test
	void simpleDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).doesNotExist();
	}

	@Test
	void nestedDuplicateFile() throws IOException {
		assertThat(new File(this.outputDirectory, "sub").mkdirs()).isTrue();
		assertThat(new File(this.originDirectory, "sub").mkdirs()).isTrue();
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/logback.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).doesNotExist();
	}

	@Test
	void nestedNonDuplicateFile() throws IOException {
		assertThat(new File(this.outputDirectory, "sub").mkdirs()).isTrue();
		assertThat(new File(this.originDirectory, "sub").mkdirs()).isTrue();
		File file = new File(this.outputDirectory, "sub/logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "sub/different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).exists();
	}

	@Test
	void nonDuplicateFile() throws IOException {
		File file = new File(this.outputDirectory, "logback.xml");
		file.createNewFile();
		new File(this.originDirectory, "different.xml").createNewFile();
		FileUtils.removeDuplicatesFromOutputDirectory(this.outputDirectory, this.originDirectory);
		assertThat(file).exists();
	}

	@Test
	void hash() throws Exception {
		File file = new File(this.tempDir, "file");
		try (OutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(new byte[] { 1, 2, 3 });
		}
		assertThat(FileUtils.sha1Hash(file)).isEqualTo("7037807198c22a7d2b0807371d763779a84fdfcf");
	}

}
