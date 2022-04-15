/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.server;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/15 12:33
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.util.Base64Utils;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ResourceUtils;

/**
 * Parser for X.509 certificates in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class CertificateParser {

  private static final String HEADER = "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

  private static final String FOOTER = "-+END\\s+.*CERTIFICATE[^-]*-+";

  private static final Pattern PATTERN = Pattern.compile(HEADER + BASE64_TEXT + FOOTER, Pattern.CASE_INSENSITIVE);

  private CertificateParser() {
  }

  /**
   * Load certificates from the specified resource.
   *
   * @param path the certificate to parse
   * @return the parsed certificates
   */
  static X509Certificate[] parse(String path) {
    CertificateFactory factory = getCertificateFactory();
    List<X509Certificate> certificates = new ArrayList<>();
    readCertificates(path, factory, certificates::add);
    return certificates.toArray(new X509Certificate[0]);
  }

  private static CertificateFactory getCertificateFactory() {
    try {
      return CertificateFactory.getInstance("X.509");
    }
    catch (CertificateException ex) {
      throw new IllegalStateException("Unable to get X.509 certificate factory", ex);
    }
  }

  private static void readCertificates(String resource, CertificateFactory factory,
          Consumer<X509Certificate> consumer) {
    try {
      String text = readText(resource);
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
    catch (CertificateException | IOException ex) {
      throw new IllegalStateException("Error reading certificate from '" + resource + "' : " + ex.getMessage(),
              ex);
    }
  }

  private static String readText(String resource) throws IOException {
    URL url = ResourceUtils.getURL(resource);
    try (Reader reader = new InputStreamReader(url.openStream())) {
      return FileCopyUtils.copyToString(reader);
    }
  }

  private static byte[] decodeBase64(String content) {
    byte[] bytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
    return Base64Utils.decode(bytes);
  }

}
