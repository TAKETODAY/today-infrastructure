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

package cn.taketoday.core.ssl.pem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ResourceUtils;

/**
 * Utility to load PEM content.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class PemContent {

  private static final Pattern PEM_HEADER = Pattern.compile("-+BEGIN\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

  private static final Pattern PEM_FOOTER = Pattern.compile("-+END\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

  private final String text;

  private PemContent(String text) {
    this.text = text;
  }

  @Nullable
  List<X509Certificate> getCertificates() {
    return PemCertificateParser.parse(this.text);
  }

  @Nullable
  List<PrivateKey> getPrivateKeys() {
    return PemPrivateKeyParser.parse(this.text);
  }

  @Nullable
  List<PrivateKey> getPrivateKeys(@Nullable String password) {
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

  @Nullable
  static PemContent load(@Nullable String content) {
    if (content == null) {
      return null;
    }
    if (isPemContent(content)) {
      return new PemContent(content);
    }
    try {
      URL url = ResourceUtils.getURL(content);
      try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
        return new PemContent(FileCopyUtils.copyToString(reader));
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException(
              "Error reading certificate or key from file '" + content + "':" + ex.getMessage(), ex);
    }
  }

  private static boolean isPemContent(@Nullable String content) {
    return content != null && PEM_HEADER.matcher(content).find() && PEM_FOOTER.matcher(content).find();
  }

}
