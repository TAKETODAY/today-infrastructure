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
 * @since 4.0 2022/4/15 12:35
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.util.Base64Utils;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ResourceUtils;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class PrivateKeyParser {

  private static final String PKCS1_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS1_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

  private static final Pattern PKCS1_PATTERN = Pattern.compile(PKCS1_HEADER + BASE64_TEXT + PKCS1_FOOTER,
          Pattern.CASE_INSENSITIVE);

  private static final Pattern PKCS8_KEY_PATTERN = Pattern.compile(PKCS8_HEADER + BASE64_TEXT + PKCS8_FOOTER,
          Pattern.CASE_INSENSITIVE);

  private PrivateKeyParser() {
  }

  /**
   * Load a private key from the specified resource.
   *
   * @param resource the private key to parse
   * @return the parsed private key
   */
  static PrivateKey parse(String resource) {
    try {
      String text = readText(resource);
      Matcher matcher = PKCS1_PATTERN.matcher(text);
      if (matcher.find()) {
        return parsePkcs1(decodeBase64(matcher.group(1)));
      }
      matcher = PKCS8_KEY_PATTERN.matcher(text);
      if (matcher.find()) {
        return parsePkcs8(decodeBase64(matcher.group(1)));
      }
      throw new IllegalStateException("Unrecognized private key format in " + resource);
    }
    catch (GeneralSecurityException | IOException ex) {
      throw new IllegalStateException("Error loading private key file " + resource, ex);
    }
  }

  private static PrivateKey parsePkcs1(byte[] privateKeyBytes) throws GeneralSecurityException {
    byte[] pkcs8Bytes = convertPkcs1ToPkcs8(privateKeyBytes);
    return parsePkcs8(pkcs8Bytes);
  }

  private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1) {
    try {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      int pkcs1Length = pkcs1.length;
      int totalLength = pkcs1Length + 22;
      // Sequence + total length
      result.write(bytes(0x30, 0x82));
      result.write((totalLength >> 8) & 0xff);
      result.write(totalLength & 0xff);
      // Integer (0)
      result.write(bytes(0x02, 0x01, 0x00));
      // Sequence: 1.2.840.113549.1.1.1, NULL
      result.write(
              bytes(0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00));
      // Octet string + length
      result.write(bytes(0x04, 0x82));
      result.write((pkcs1Length >> 8) & 0xff);
      result.write(pkcs1Length & 0xff);
      // PKCS1
      result.write(pkcs1);
      return result.toByteArray();
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static byte[] bytes(int... elements) {
    byte[] result = new byte[elements.length];
    for (int i = 0; i < elements.length; i++) {
      result[i] = (byte) elements[i];
    }
    return result;
  }

  private static PrivateKey parsePkcs8(byte[] privateKeyBytes) throws GeneralSecurityException {
    try {
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    }
    catch (InvalidKeySpecException ex) {
      throw new IllegalArgumentException("Unexpected key format", ex);
    }
  }

  private static String readText(String resource) throws IOException {
    URL url = ResourceUtils.getURL(resource);
    try (Reader reader = new InputStreamReader(url.openStream())) {
      return FileCopyUtils.copyToString(reader);
    }
  }

  private static byte[] decodeBase64(String content) {
    byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
    return Base64Utils.decode(contentBytes);
  }

}
