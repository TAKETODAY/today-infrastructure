/*
 * Copyright 2012 - 2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SizeCalculatingEntryWriter}.
 *
 * @author Phillip Webb
 */
class SizeCalculatingEntryWriterTests {

	@Test
	void getWhenWithinThreshold() throws Exception {
		TestEntryWriter original = new TestEntryWriter(SizeCalculatingEntryWriter.THRESHOLD - 1);
		EntryWriter writer = SizeCalculatingEntryWriter.get(original);
		assertThat(writer.size()).isEqualTo(original.getBytes().length);
		assertThat(writeBytes(writer)).isEqualTo(original.getBytes());
		assertThat(writer).extracting("content").isNotInstanceOf(File.class);
	}

	@Test
	void getWhenExceedingThreshold() throws Exception {
		TestEntryWriter original = new TestEntryWriter(SizeCalculatingEntryWriter.THRESHOLD + 1);
		EntryWriter writer = SizeCalculatingEntryWriter.get(original);
		assertThat(writer.size()).isEqualTo(original.getBytes().length);
		assertThat(writeBytes(writer)).isEqualTo(original.getBytes());
		assertThat(writer).extracting("content").isInstanceOf(File.class);
	}

	private byte[] writeBytes(EntryWriter writer) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writer.write(outputStream);
		outputStream.close();
		return outputStream.toByteArray();
	}

	private static class TestEntryWriter implements EntryWriter {

		private final byte[] bytes;

		TestEntryWriter(int size) {
			this.bytes = new byte[size];
			new Random().nextBytes(this.bytes);
		}

		byte[] getBytes() {
			return this.bytes;
		}

		@Override
		public void write(OutputStream outputStream) throws IOException {
			outputStream.write(this.bytes);
		}

	}

}
