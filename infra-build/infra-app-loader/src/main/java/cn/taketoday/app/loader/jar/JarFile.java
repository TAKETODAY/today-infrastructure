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

package cn.taketoday.app.loader.jar;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.data.RandomAccessData;
import cn.taketoday.app.loader.data.RandomAccessDataFile;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} based
 * on any directory entry.</li>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} for
 * embedded JAR files (as long as their entry is not compressed).</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JarFile extends AbstractJarFile implements Iterable<java.util.jar.JarEntry> {

  private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

  private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

  private static final String HANDLERS_PACKAGE = "cn.taketoday.app.loader";

  private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

  private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

  private static final String READ_ACTION = "read";

  private final RandomAccessDataFile rootFile;

  private final String pathFromRoot;

  private final RandomAccessData data;

  private final JarFileType type;

  private URL url;

  private String urlString;

  private final JarFileEntries entries;

  private final Supplier<Manifest> manifestSupplier;

  private SoftReference<Manifest> manifest;

  private boolean signed;

  private String comment;

  private volatile boolean closed;

  private volatile JarFileWrapper wrapper;

  /**
   * Create a new {@link JarFile} backed by the specified file.
   *
   * @param file the root jar file
   * @throws IOException if the file cannot be read
   */
  public JarFile(File file) throws IOException {
    this(new RandomAccessDataFile(file));
  }

  /**
   * Create a new {@link JarFile} backed by the specified file.
   *
   * @param file the root jar file
   * @throws IOException if the file cannot be read
   */
  JarFile(RandomAccessDataFile file) throws IOException {
    this(file, "", file, JarFileType.DIRECT);
  }

  /**
   * Private constructor used to create a new {@link JarFile} either directly or from a
   * nested entry.
   *
   * @param rootFile the root jar file
   * @param pathFromRoot the name of this file
   * @param data the underlying data
   * @param type the type of the jar file
   * @throws IOException if the file cannot be read
   */
  private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarFileType type)
          throws IOException {
    this(rootFile, pathFromRoot, data, null, type, null);
  }

  private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter,
          JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
    super(rootFile.getFile());
    super.close();
    this.rootFile = rootFile;
    this.pathFromRoot = pathFromRoot;
    CentralDirectoryParser parser = new CentralDirectoryParser();
    this.entries = parser.addVisitor(new JarFileEntries(this, filter));
    this.type = type;
    parser.addVisitor(centralDirectoryVisitor());
    try {
      this.data = parser.parse(data, filter == null);
    }
    catch (RuntimeException ex) {
      try {
        this.rootFile.close();
        super.close();
      }
      catch (IOException ignored) { }
      throw ex;
    }
    this.manifestSupplier = (manifestSupplier != null) ? manifestSupplier : () -> {
      try (InputStream inputStream = getInputStream(MANIFEST_NAME)) {
        if (inputStream == null) {
          return null;
        }
        return new Manifest(inputStream);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private CentralDirectoryVisitor centralDirectoryVisitor() {
    return new CentralDirectoryVisitor() {

      @Override
      public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
        JarFile.this.comment = endRecord.getComment();
      }

      @Override
      public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
        AsciiBytes name = fileHeader.getName();
        if (name.startsWith(META_INF) && name.endsWith(SIGNATURE_FILE_EXTENSION)) {
          JarFile.this.signed = true;
        }
      }

      @Override
      public void visitEnd() {
      }

    };
  }

  JarFileWrapper getWrapper() throws IOException {
    JarFileWrapper wrapper = this.wrapper;
    if (wrapper == null) {
      wrapper = new JarFileWrapper(this);
      this.wrapper = wrapper;
    }
    return wrapper;
  }

  @Override
  Permission getPermission() {
    return new FilePermission(this.rootFile.getFile().getPath(), READ_ACTION);
  }

  protected final RandomAccessDataFile getRootJarFile() {
    return this.rootFile;
  }

  RandomAccessData getData() {
    return this.data;
  }

  @Override
  public Manifest getManifest() throws IOException {
    Manifest manifest = (this.manifest != null) ? this.manifest.get() : null;
    if (manifest == null) {
      try {
        manifest = this.manifestSupplier.get();
      }
      catch (RuntimeException ex) {
        throw new IOException(ex);
      }
      this.manifest = new SoftReference<>(manifest);
    }
    return manifest;
  }

  @Override
  public Enumeration<java.util.jar.JarEntry> entries() {
    return new JarEntryEnumeration(this.entries.iterator());
  }

  @Override
  public Stream<java.util.jar.JarEntry> stream() {
    Spliterator<java.util.jar.JarEntry> spliterator = Spliterators.spliterator(iterator(), size(),
            Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
    return StreamSupport.stream(spliterator, false);
  }

  /**
   * Return an iterator for the contained entries.
   *
   * @see java.lang.Iterable#iterator()
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Iterator<java.util.jar.JarEntry> iterator() {
    return (Iterator) this.entries.iterator(this::ensureOpen);
  }

  public JarEntry getJarEntry(CharSequence name) {
    return this.entries.getEntry(name);
  }

  @Override
  public JarEntry getJarEntry(String name) {
    return (JarEntry) getEntry(name);
  }

  public boolean containsEntry(String name) {
    return this.entries.containsEntry(name);
  }

  @Override
  public ZipEntry getEntry(String name) {
    ensureOpen();
    return this.entries.getEntry(name);
  }

  @Override
  InputStream getInputStream() throws IOException {
    return this.data.getInputStream();
  }

  @Override
  public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
    ensureOpen();
    if (entry instanceof JarEntry jarEntry) {
      return this.entries.getInputStream(jarEntry);
    }
    return getInputStream((entry != null) ? entry.getName() : null);
  }

  InputStream getInputStream(String name) throws IOException {
    return this.entries.getInputStream(name);
  }

  /**
   * Return a nested {@link JarFile} loaded from the specified entry.
   *
   * @param entry the zip entry
   * @return a {@link JarFile} for the entry
   * @throws IOException if the nested jar file cannot be read
   */
  public synchronized JarFile getNestedJarFile(ZipEntry entry) throws IOException {
    return getNestedJarFile((JarEntry) entry);
  }

  /**
   * Return a nested {@link JarFile} loaded from the specified entry.
   *
   * @param entry the zip entry
   * @return a {@link JarFile} for the entry
   * @throws IOException if the nested jar file cannot be read
   */
  public synchronized JarFile getNestedJarFile(JarEntry entry) throws IOException {
    try {
      return createJarFileFromEntry(entry);
    }
    catch (Exception ex) {
      throw new IOException("Unable to open nested jar file '" + entry.getName() + "'", ex);
    }
  }

  private JarFile createJarFileFromEntry(JarEntry entry) throws IOException {
    if (entry.isDirectory()) {
      return createJarFileFromDirectoryEntry(entry);
    }
    return createJarFileFromFileEntry(entry);
  }

  private JarFile createJarFileFromDirectoryEntry(JarEntry entry) throws IOException {
    AsciiBytes name = entry.getAsciiBytesName();
    JarEntryFilter filter = (candidate) -> {
      if (candidate.startsWith(name) && !candidate.equals(name)) {
        return candidate.substring(name.length());
      }
      return null;
    };
    return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName().substring(0, name.length() - 1),
            this.data, filter, JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
  }

  private JarFile createJarFileFromFileEntry(JarEntry entry) throws IOException {
    if (entry.getMethod() != ZipEntry.STORED) {
      throw new IllegalStateException(
              "Unable to open nested entry '" + entry.getName() + "'. It has been compressed and nested "
                      + "jar files must be stored without compression. Please check the "
                      + "mechanism used to create your executable jar file");
    }
    RandomAccessData entryData = this.entries.getEntryData(entry.getName());
    return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData,
            JarFileType.NESTED_JAR);
  }

  @Override
  public String getComment() {
    ensureOpen();
    return this.comment;
  }

  @Override
  public int size() {
    ensureOpen();
    return this.entries.getSize();
  }

  @Override
  public void close() throws IOException {
    if (this.closed) {
      return;
    }
    super.close();
    if (this.type == JarFileType.DIRECT) {
      this.rootFile.close();
    }
    this.closed = true;
  }

  private void ensureOpen() {
    if (this.closed) {
      throw new IllegalStateException("zip file closed");
    }
  }

  boolean isClosed() {
    return this.closed;
  }

  String getUrlString() throws MalformedURLException {
    if (this.urlString == null) {
      this.urlString = getUrl().toString();
    }
    return this.urlString;
  }

  @Override
  public URL getUrl() throws MalformedURLException {
    if (this.url == null) {
      String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
      file = file.replace("file:////", "file://"); // Fix UNC paths
      this.url = new URL("jar", "", -1, file, new Handler(this));
    }
    return this.url;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public String getName() {
    return this.rootFile.getFile() + this.pathFromRoot;
  }

  boolean isSigned() {
    return this.signed;
  }

  JarEntryCertification getCertification(JarEntry entry) {
    try {
      return this.entries.getCertification(entry);
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void clearCache() {
    this.entries.clearCache();
  }

  protected String getPathFromRoot() {
    return this.pathFromRoot;
  }

  @Override
  JarFileType getType() {
    return this.type;
  }

  /**
   * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
   * {@link URLStreamHandler} will be located to deal with jar URLs.
   */
  public static void registerUrlProtocolHandler() {
    Handler.captureJarContextUrl();
    String handlers = System.getProperty(PROTOCOL_HANDLER, "");
    System.setProperty(PROTOCOL_HANDLER,
            ((handlers == null || handlers.isEmpty()) ? HANDLERS_PACKAGE : handlers + "|" + HANDLERS_PACKAGE));
    resetCachedUrlHandlers();
  }

  /**
   * Reset any cached handlers just in case a jar protocol has already been used. We
   * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
   * should have no effect other than clearing the handlers cache.
   */
  private static void resetCachedUrlHandlers() {
    try {
      URL.setURLStreamHandlerFactory(null);
    }
    catch (Error ex) {
      // Ignore
    }
  }

  /**
   * An {@link Enumeration} on {@linkplain java.util.jar.JarEntry jar entries}.
   */
  private static class JarEntryEnumeration implements Enumeration<java.util.jar.JarEntry> {

    private final Iterator<JarEntry> iterator;

    JarEntryEnumeration(Iterator<JarEntry> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasMoreElements() {
      return this.iterator.hasNext();
    }

    @Override
    public java.util.jar.JarEntry nextElement() {
      return this.iterator.next();
    }

  }

}
