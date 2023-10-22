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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

/**
 * Parser for X.509 certificates in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class PemCertificateParser {

  private static final String HEADER = "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

  private static final String FOOTER = "-+END\\s+.*CERTIFICATE[^-]*-+";

  private static final Pattern PATTERN = Pattern.compile(HEADER + BASE64_TEXT + FOOTER, Pattern.CASE_INSENSITIVE);

  private PemCertificateParser() { }

  /**
   * Parse certificates from the specified string.
   *
   * @param text the text to parse
   * @return the parsed certificates
   */
  @Nullable
  static List<X509Certificate> parse(@Nullable String text) {
    if (text == null) {
      return null;
    }
    CertificateFactory factory = getCertificateFactory();
    List<X509Certificate> certs = new ArrayList<>();
    readCertificates(text, factory, certs::add);
    return List.copyOf(certs);
  }

  private static CertificateFactory getCertificateFactory() {
    try {
      return CertificateFactory.getInstance("X.509");
    }
    catch (CertificateException ex) {
      throw new IllegalStateException("Unable to get X.509 certificate factory", ex);
    }
  }

  private static void readCertificates(String text, CertificateFactory factory, Consumer<X509Certificate> consumer) {
    try {
      Matcher matcher = PATTERN.matcher(text);
      while (matcher.find()) {
        String encodedText = matcher.group(1);
        byte[] decodedBytes = decodeBase64(encodedText);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);
        while (inputStream.available() > 0) {
          consumer.accept((X509Certificate) factory.generateCertificate(inputStream));
        }
      }
    }
    catch (CertificateException ex) {
      throw new IllegalStateException("Error reading certificate: " + ex.getMessage(), ex);
    }
  }

  private static byte[] decodeBase64(String content) {
    byte[] bytes = content.replaceAll("\r", "")
            .replaceAll("\n", "")
            .getBytes();
    return Base64.getDecoder().decode(bytes);
  }

}
