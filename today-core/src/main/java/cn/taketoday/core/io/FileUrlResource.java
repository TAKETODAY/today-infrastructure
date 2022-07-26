/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;

/**
 * Subclass of {@link UrlResource} which assumes file resolution, to the degree
 * of implementing the {@link WritableResource} interface for it. This resource
 * variant also caches resolved {@link File} handles from {@link #getFile()}.
 *
 * <p>This is the class resolved by {@link DefaultResourceLoader} for a "file:..."
 * URL location, allowing a downcast to {@link WritableResource} for it.
 *
 * <p>Alternatively, for direct construction from a {@link java.io.File} handle
 * or NIO {@link java.nio.file.Path}, consider using {@link FileSystemResource}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/30 21:39
 */
public class FileUrlResource extends UrlResource implements WritableResource {

  @Nullable
  private volatile File file;

  /**
   * Create a new {@code FileUrlBasedResource} based on the given URL object.
   * <p>Note that this does not enforce "file" as URL protocol. If a protocol
   * is known to be resolvable to a file, it is acceptable for this purpose.
   *
   * @param url a URL
   * @see ResourceUtils#isFileURL(URL)
   * @see #getFile()
   */
  public FileUrlResource(URL url) {
    super(url);
  }

  /**
   * Create a new {@code FileUrlBasedResource} based on the given file location,
   * using the URL protocol "file".
   * <p>The given parts will automatically get encoded if necessary.
   *
   * @param location the location (i.e. the file path within that protocol)
   * @throws MalformedURLException if the given URL specification is not valid
   * @see UrlResource#UrlResource(String, String)
   * @see ResourceUtils#URL_PROTOCOL_FILE
   */
  public FileUrlResource(String location) throws MalformedURLException {
    super(ResourceUtils.URL_PROTOCOL_FILE, location);
  }

  @Override
  public File getFile() throws IOException {
    File file = this.file;
    if (file != null) {
      return file;
    }
    file = super.getFile();
    this.file = file;
    return file;
  }

  @Override
  public boolean isDirectory() throws IOException {
    return getFile().isDirectory();
  }

  @Override
  public boolean isWritable() {
    try {
      File file = getFile();
      return file.canWrite() && !file.isDirectory();
    }
    catch (IOException ex) {
      return false;
    }
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return Files.newOutputStream(getFile().toPath());
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE);
  }

  @Override
  public FileUrlResource createRelative(String relativePath) throws MalformedURLException {
    return new FileUrlResource(createRelativeURL(relativePath));
  }

}
