/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.loader.jar;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import infra.app.loader.zip.ZipContent;

/**
 * Security information ({@link Certificate} and {@link CodeSigner} details) for entries
 * in the jar.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class SecurityInfo {

  static final SecurityInfo NONE = new SecurityInfo(null, null);

  private final Certificate @Nullable [][] certificateLookups;

  private final CodeSigner @Nullable [][] codeSignerLookups;

  private SecurityInfo(Certificate @Nullable [][] entryCertificates, CodeSigner @Nullable [][] entryCodeSigners) {
    this.certificateLookups = entryCertificates;
    this.codeSignerLookups = entryCodeSigners;
  }

  Certificate @Nullable [] getCertificates(ZipContent.Entry contentEntry) {
    return (this.certificateLookups != null) ? clone(this.certificateLookups[contentEntry.getLookupIndex()]) : null;
  }

  CodeSigner @Nullable [] getCodeSigners(ZipContent.Entry contentEntry) {
    return (this.codeSignerLookups != null) ? clone(this.codeSignerLookups[contentEntry.getLookupIndex()]) : null;
  }

  private <T> T @Nullable [] clone(T @Nullable [] array) {
    return (array != null) ? array.clone() : null;
  }

  /**
   * Get the {@link SecurityInfo} for the given {@link ZipContent}.
   *
   * @param content the zip content
   * @return the security info
   */
  static SecurityInfo get(ZipContent content) {
    if (!content.hasJarSignatureFile()) {
      return NONE;
    }
    try {
      return load(content);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Load security info from the jar file. We need to use {@link JarInputStream} to
   * obtain the security info since we don't have an actual real file to read. This
   * isn't that fast, but hopefully doesn't happen too often and the result is cached.
   *
   * @param content the zip content
   * @return the security info
   * @throws IOException on I/O error
   */
  @SuppressWarnings("resource")
  private static SecurityInfo load(ZipContent content) throws IOException {
    int size = content.size();
    boolean hasSecurityInfo = false;
    Certificate[][] entryCertificates = new Certificate[size][];
    CodeSigner[][] entryCodeSigners = new CodeSigner[size][];
    try (JarEntriesStream entries = new JarEntriesStream(content.openRawZipData().asInputStream())) {
      JarEntry entry = entries.getNextEntry();
      while (entry != null) {
        ZipContent.Entry relatedEntry = content.getEntry(entry.getName());
        if (relatedEntry != null && entries.matches(relatedEntry.isDirectory(),
                relatedEntry.getUncompressedSize(), relatedEntry.getCompressionMethod(),
                () -> relatedEntry.openContent().asInputStream())) {
          Certificate[] certificates = entry.getCertificates();
          CodeSigner[] codeSigners = entry.getCodeSigners();
          if (certificates != null || codeSigners != null) {
            hasSecurityInfo = true;
            entryCertificates[relatedEntry.getLookupIndex()] = certificates;
            entryCodeSigners[relatedEntry.getLookupIndex()] = codeSigners;
          }
        }
        entry = entries.getNextEntry();
      }
    }
    return (!hasSecurityInfo) ? NONE : new SecurityInfo(entryCertificates, entryCodeSigners);
  }

}
