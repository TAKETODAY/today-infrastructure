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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 * Tests for {@link ImagePackager}
 *
 * @author Phillip Webb
 */
class ImagePackagerTests extends AbstractPackagerTests<ImagePackager> {

	private Map<ZipArchiveEntry, byte[]> entries;

	@Override
	protected ImagePackager createPackager(File source) {
		return new ImagePackager(source, null);
	}

	@Override
	protected void execute(ImagePackager packager, Libraries libraries) throws IOException {
		this.entries = new LinkedHashMap<>();
		packager.packageImage(libraries, this::save);
	}

	private void save(ZipEntry entry, EntryWriter writer) {
		try {
			this.entries.put((ZipArchiveEntry) entry, getContent(writer));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private byte[] getContent(EntryWriter writer) throws IOException {
		if (writer == null) {
			return null;
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writer.write(outputStream);
		return outputStream.toByteArray();
	}

	@Override
	protected Collection<ZipArchiveEntry> getAllPackagedEntries() throws IOException {
		return this.entries.keySet();
	}

	@Override
	protected Manifest getPackagedManifest() throws IOException {
		byte[] bytes = getEntryBytes("META-INF/MANIFEST.MF");
		return (bytes != null) ? new Manifest(new ByteArrayInputStream(bytes)) : null;
	}

	@Override
	protected String getPackagedEntryContent(String name) throws IOException {
		byte[] bytes = getEntryBytes(name);
		return (bytes != null) ? new String(bytes, StandardCharsets.UTF_8) : null;
	}

	private byte[] getEntryBytes(String name) throws IOException {
		ZipEntry entry = getPackagedEntry(name);
		return (entry != null) ? this.entries.get(entry) : null;
	}

}
