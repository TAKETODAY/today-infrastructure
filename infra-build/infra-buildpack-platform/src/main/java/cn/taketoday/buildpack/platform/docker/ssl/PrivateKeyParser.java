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

package cn.taketoday.buildpack.platform.docker.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
final class PrivateKeyParser {

  private static final String PKCS1_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS1_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String EC_HEADER = "-+BEGIN\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String EC_FOOTER = "-+END\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

  private static final List<PemParser> PEM_PARSERS;

  static {
    List<PemParser> parsers = new ArrayList<>();
    parsers.add(new PemParser(PKCS1_HEADER, PKCS1_FOOTER, PrivateKeyParser::createKeySpecForPkcs1, "RSA"));
    parsers.add(new PemParser(EC_HEADER, EC_FOOTER, PrivateKeyParser::createKeySpecForEc, "EC"));
    parsers.add(new PemParser(PKCS8_HEADER, PKCS8_FOOTER, PKCS8EncodedKeySpec::new, "RSA", "EC", "DSA", "Ed25519"));
    PEM_PARSERS = Collections.unmodifiableList(parsers);
  }

  /**
   * ASN.1 encoded object identifier {@literal 1.2.840.113549.1.1.1}.
   */
  private static final int[] RSA_ALGORITHM = { 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01 };

  /**
   * ASN.1 encoded object identifier {@literal 1.2.840.10045.2.1}.
   */
  private static final int[] EC_ALGORITHM = { 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01 };

  /**
   * ASN.1 encoded object identifier {@literal 1.3.132.0.34}.
   */
  private static final int[] EC_PARAMETERS = { 0x2b, 0x81, 0x04, 0x00, 0x22 };

  private PrivateKeyParser() {
  }

  private static PKCS8EncodedKeySpec createKeySpecForPkcs1(byte[] bytes) {
    return createKeySpecForAlgorithm(bytes, RSA_ALGORITHM, null);
  }

  private static PKCS8EncodedKeySpec createKeySpecForEc(byte[] bytes) {
    return createKeySpecForAlgorithm(bytes, EC_ALGORITHM, EC_PARAMETERS);
  }

  private static PKCS8EncodedKeySpec createKeySpecForAlgorithm(byte[] bytes, int[] algorithm, int[] parameters) {
    try {
      DerEncoder encoder = new DerEncoder();
      encoder.integer(0x00); // Version 0
      DerEncoder algorithmIdentifier = new DerEncoder();
      algorithmIdentifier.objectIdentifier(algorithm);
      algorithmIdentifier.objectIdentifier(parameters);
      byte[] byteArray = algorithmIdentifier.toByteArray();
      encoder.sequence(byteArray);
      encoder.octetString(bytes);
      return new PKCS8EncodedKeySpec(encoder.toSequence());
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Load a private key from the specified file paths.
   *
   * @param path the path to the private key file
   * @return private key from specified file path
   */
  static PrivateKey parse(Path path) {
    try {
      String text = Files.readString(path);
      for (PemParser pemParser : PEM_PARSERS) {
        PrivateKey privateKey = pemParser.parse(text);
        if (privateKey != null) {
          return privateKey;
        }
      }
      throw new IllegalStateException("Unrecognized private key format");
    }
    catch (Exception ex) {
      throw new IllegalStateException("Error loading private key file " + path, ex);
    }
  }

  /**
   * Parser for a specific PEM format.
   */
  private static class PemParser {

    private final Pattern pattern;

    private final Function<byte[], PKCS8EncodedKeySpec> keySpecFactory;

    private final String[] algorithms;

    PemParser(String header, String footer, Function<byte[], PKCS8EncodedKeySpec> keySpecFactory,
            String... algorithms) {
      this.pattern = Pattern.compile(header + BASE64_TEXT + footer, Pattern.CASE_INSENSITIVE);
      this.algorithms = algorithms;
      this.keySpecFactory = keySpecFactory;
    }

    PrivateKey parse(String text) {
      Matcher matcher = this.pattern.matcher(text);
      return (!matcher.find()) ? null : parse(decodeBase64(matcher.group(1)));
    }

    private static byte[] decodeBase64(String content) {
      byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
      return Base64.getDecoder().decode(contentBytes);
    }

    private PrivateKey parse(byte[] bytes) {
      try {
        PKCS8EncodedKeySpec keySpec = this.keySpecFactory.apply(bytes);
        for (String algorithm : this.algorithms) {
          KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
          try {
            return keyFactory.generatePrivate(keySpec);
          }
          catch (InvalidKeySpecException ex) {
          }
        }
        return null;
      }
      catch (GeneralSecurityException ex) {
        throw new IllegalArgumentException("Unexpected key format", ex);
      }
    }

  }

  /**
   * Simple ASN.1 DER encoder.
   */
  static class DerEncoder {

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    void objectIdentifier(int... encodedObjectIdentifier) throws IOException {
      int code = (encodedObjectIdentifier != null) ? 0x06 : 0x05;
      codeLengthBytes(code, bytes(encodedObjectIdentifier));
    }

    void integer(int... encodedInteger) throws IOException {
      codeLengthBytes(0x02, bytes(encodedInteger));
    }

    void octetString(byte[] bytes) throws IOException {
      codeLengthBytes(0x04, bytes);
    }

    void sequence(int... elements) throws IOException {
      sequence(bytes(elements));
    }

    void sequence(byte[] bytes) throws IOException {
      codeLengthBytes(0x30, bytes);
    }

    void codeLengthBytes(int code, byte[] bytes) throws IOException {
      this.stream.write(code);
      int length = (bytes != null) ? bytes.length : 0;
      if (length <= 127) {
        this.stream.write(length & 0xFF);
      }
      else {
        ByteArrayOutputStream lengthStream = new ByteArrayOutputStream();
        while (length != 0) {
          lengthStream.write(length & 0xFF);
          length = length >> 8;
        }
        byte[] lengthBytes = lengthStream.toByteArray();
        this.stream.write(0x80 | lengthBytes.length);
        for (int i = lengthBytes.length - 1; i >= 0; i--) {
          this.stream.write(lengthBytes[i]);
        }
      }
      if (bytes != null) {
        this.stream.write(bytes);
      }
    }

    private static byte[] bytes(int... elements) {
      if (elements == null) {
        return null;
      }
      byte[] result = new byte[elements.length];
      for (int i = 0; i < elements.length; i++) {
        result[i] = (byte) elements[i];
      }
      return result;
    }

    byte[] toSequence() throws IOException {
      DerEncoder sequenceEncoder = new DerEncoder();
      sequenceEncoder.sequence(toByteArray());
      return sequenceEncoder.toByteArray();
    }

    byte[] toByteArray() {
      return this.stream.toByteArray();
    }

  }

}
