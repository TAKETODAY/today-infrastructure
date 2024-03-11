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

package cn.taketoday.core.ssl.pem;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StreamUtils;

/**
 * Utility to load PEM content.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class PemContent {

  private static final Pattern PEM_HEADER = Pattern.compile("-+BEGIN\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

  private static final Pattern PEM_FOOTER = Pattern.compile("-+END\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

  private final String text;

  private PemContent(String text) {
    this.text = text;
  }

  /**
   * Parse and return all {@link X509Certificate certificates} from the PEM content.
   * Most PEM files either contain a single certificate or a certificate chain.
   *
   * @return the certificates
   * @throws IllegalStateException if no certificates could be loaded
   */
  @Nullable
  public List<X509Certificate> getCertificates() {
    return PemCertificateParser.parse(this.text);
  }

  /**
   * Parse and return the {@link PrivateKey private keys} from the PEM content.
   *
   * @return the private keys
   * @throws IllegalStateException if no private key could be loaded
   */
  @Nullable
  public PrivateKey getPrivateKey() {
    return getPrivateKey(null);
  }

  /**
   * Parse and return the {@link PrivateKey private keys} from the PEM content or
   * {@code null} if there is no private key.
   *
   * @param password the password to decrypt the private keys or {@code null}
   * @return the private keys
   */
  @Nullable
  public PrivateKey getPrivateKey(@Nullable String password) {
    return PemPrivateKeyParser.parse(this.text, password);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.text, ((PemContent) obj).text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.text);
  }

  @Override
  public String toString() {
    return this.text;
  }

  /**
   * Load {@link PemContent} from the given content (either the PEM content itself or a
   * reference to the resource to load).
   *
   * @param content the content to load
   * @return a new {@link PemContent} instance
   * @throws IOException on IO error
   */
  @Nullable
  static PemContent load(@Nullable String content) throws IOException {
    if (content == null) {
      return null;
    }
    if (isPresentInText(content)) {
      return new PemContent(content);
    }
    try {
      return load(ResourceUtils.getURL(content));
    }
    catch (IOException | UncheckedIOException ex) {
      throw new IOException("Error reading certificate or key from file '%s'".formatted(content), ex);
    }
  }

  /**
   * Load {@link PemContent} from the given {@link Path}.
   *
   * @param path a path to load the content from
   * @return the loaded PEM content
   * @throws IOException on IO error
   */
  public static PemContent load(Path path) throws IOException {
    Assert.notNull(path, "Path is required");
    try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
      return load(in);
    }
  }

  private static PemContent load(URL url) throws IOException {
    Assert.notNull(url, "Url is required");
    try (InputStream in = url.openStream()) {
      return load(in);
    }
  }

  private static PemContent load(InputStream in) throws IOException {
    return of(StreamUtils.copyToString(in, StandardCharsets.UTF_8));
  }

  /**
   * Return a new {@link PemContent} instance containing the given text.
   *
   * @param text the text containing PEM encoded content
   * @return a new {@link PemContent} instance
   */
  @Nullable
  public static PemContent of(@Nullable String text) {
    return (text != null) ? new PemContent(text) : null;
  }

  /**
   * Return if PEM content is present in the given text.
   *
   * @param text the text to check
   * @return if the text includes PEM encoded content.
   */
  public static boolean isPresentInText(@Nullable String text) {
    return text != null && PEM_HEADER.matcher(text).find() && PEM_FOOTER.matcher(text).find();
  }

}
