/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.loader.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

/**
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class TestJarFile {

	private final byte[] buffer = new byte[4096];

	private final File temporaryDirectory;

	private final File jarSource;

	private final List<ZipEntrySource> entries = new ArrayList<>();

	public TestJarFile(File temporaryDirectory) {
		this.temporaryDirectory = temporaryDirectory;
		this.jarSource = new File(temporaryDirectory, "jar-source");
	}

	public void addClass(String filename, Class<?> classToCopy) throws IOException {
		addClass(filename, classToCopy, null);
	}

	public void addClass(String filename, Class<?> classToCopy, Long time) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		InputStream inputStream = getClass()
			.getResourceAsStream("/" + classToCopy.getName().replace('.', '/') + ".class");
		copyToFile(inputStream, file);
		if (time != null) {
			file.setLastModified(time);
		}
		this.entries.add(new FileSource(filename, file));
	}

	public void addFile(String filename, File fileToCopy) throws IOException {
		try (InputStream inputStream = new FileInputStream(fileToCopy)) {
			addFile(filename, inputStream);
		}
	}

	public void addFile(String filename, InputStream inputStream) throws IOException {
		File file = getFilePath(filename);
		file.getParentFile().mkdirs();
		copyToFile(inputStream, file);
		this.entries.add(new FileSource(filename, file));
	}

	public void addManifest(Manifest manifest) throws IOException {
		File manifestFile = new File(this.jarSource, "META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		try (OutputStream outputStream = new FileOutputStream(manifestFile)) {
			manifest.write(outputStream);
		}
		this.entries.add(new FileSource("META-INF/MANIFEST.MF", manifestFile));
	}

	private File getFilePath(String filename) {
		String[] paths = filename.split("\\/");
		File file = this.jarSource;
		for (String path : paths) {
			file = new File(file, path);
		}
		return file;
	}

	private void copyToFile(InputStream inputStream, File file) throws IOException {
		try (OutputStream outputStream = new FileOutputStream(file)) {
			copy(inputStream, outputStream);
		}
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		int bytesRead;
		while ((bytesRead = in.read(this.buffer)) != -1) {
			out.write(this.buffer, 0, bytesRead);
		}
	}

	public JarFile getJarFile() throws IOException {
		return new JarFile(getFile());
	}

	public File getJarSource() {
		return this.jarSource;
	}

	public File getFile() {
		return getFile("jar");
	}

	public File getFile(String extension) {
		File file = new File(this.temporaryDirectory, UUID.randomUUID() + "." + extension);
		ZipUtil.pack(this.entries.toArray(new ZipEntrySource[0]), file);
		return file;
	}

}
