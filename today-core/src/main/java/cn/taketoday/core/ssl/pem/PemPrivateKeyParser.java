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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import cn.taketoday.core.ssl.pem.PemPrivateKeyParser.DerElement.TagType;
import cn.taketoday.core.ssl.pem.PemPrivateKeyParser.DerElement.ValueType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class PemPrivateKeyParser {

  private static final String PKCS1_RSA_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS1_RSA_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String PKCS8_ENCRYPTED_HEADER = "-+BEGIN\\s+ENCRYPTED\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String PKCS8_ENCRYPTED_FOOTER = "-+END\\s+ENCRYPTED\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String SEC1_EC_HEADER = "-+BEGIN\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

  private static final String SEC1_EC_FOOTER = "-+END\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+";

  private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

  public static final int BASE64_TEXT_GROUP = 1;

  private static final List<PemParser> PEM_PARSERS = List.of(
          new PemParser(PKCS1_RSA_HEADER, PKCS1_RSA_FOOTER, PemPrivateKeyParser::createKeySpecForPkcs1Rsa, "RSA"),
          new PemParser(SEC1_EC_HEADER, SEC1_EC_FOOTER, PemPrivateKeyParser::createKeySpecForSec1Ec, "EC"),
          new PemParser(PKCS8_HEADER, PKCS8_FOOTER, PemPrivateKeyParser::createKeySpecForPkcs8,
                  "RSA", "RSASSA-PSS", "EC", "DSA", "EdDSA", "XDH"),
          new PemParser(PKCS8_ENCRYPTED_HEADER, PKCS8_ENCRYPTED_FOOTER, PemPrivateKeyParser::createKeySpecForPkcs8Encrypted,
                  "RSA", "RSASSA-PSS", "EC", "DSA", "EdDSA", "XDH"));

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

  private PemPrivateKeyParser() { }

  private static PKCS8EncodedKeySpec createKeySpecForPkcs1Rsa(byte[] bytes, String password) {
    return createKeySpecForAlgorithm(bytes, RSA_ALGORITHM, null);
  }

  private static PKCS8EncodedKeySpec createKeySpecForSec1Ec(byte[] bytes, String password) {
    DerElement ecPrivateKey = DerElement.of(bytes);
    Assert.state(ecPrivateKey.isType(ValueType.ENCODED, TagType.SEQUENCE), "Key spec should be an ASN.1 encoded sequence");
    DerElement version = DerElement.of(ecPrivateKey.getContents());
    Assert.state(version != null && version.isType(ValueType.PRIMITIVE, TagType.INTEGER), "Key spec should start with version");
    Assert.state(version.getContents().remaining() == 1 && version.getContents().get() == 1, "Key spec version must be 1");
    DerElement privateKey = DerElement.of(ecPrivateKey.getContents());
    Assert.state(privateKey != null && privateKey.isType(ValueType.PRIMITIVE, TagType.OCTET_STRING), "Key spec should contain private key");
    DerElement parameters = DerElement.of(ecPrivateKey.getContents());
    return createKeySpecForAlgorithm(bytes, EC_ALGORITHM, getEcParameters(parameters));
  }

  private static int[] getEcParameters(@Nullable DerElement parameters) {
    if (parameters == null) {
      return EC_PARAMETERS;
    }
    Assert.state(parameters.isType(ValueType.ENCODED), "Key spec should contain encoded parameters");
    DerElement contents = DerElement.of(parameters.getContents());
    Assert.state(contents.isType(ValueType.PRIMITIVE, TagType.OBJECT_IDENTIFIER),
            "Key spec parameters should contain object identifier");
    return getEcParameters(contents.getContents());
  }

  private static int[] getEcParameters(ByteBuffer bytes) {
    int[] result = new int[bytes.remaining()];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytes.get() & 0xFF;
    }
    return result;
  }

  private static PKCS8EncodedKeySpec createKeySpecForAlgorithm(byte[] bytes, int[] algorithm, @Nullable int[] parameters) {
    try {
      DerEncoder encoder = new DerEncoder();
      encoder.integer(0x00); // Version 0
      DerEncoder algorithmIdentifier = new DerEncoder();
      algorithmIdentifier.objectIdentifier(algorithm);
      algorithmIdentifier.objectIdentifier(parameters);
      encoder.sequence(algorithmIdentifier.toByteArray());
      encoder.octetString(bytes);
      return new PKCS8EncodedKeySpec(encoder.toSequence());
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static PKCS8EncodedKeySpec createKeySpecForPkcs8(byte[] bytes, String password) {
    return new PKCS8EncodedKeySpec(bytes);
  }

  private static PKCS8EncodedKeySpec createKeySpecForPkcs8Encrypted(byte[] bytes, String password) {
    return Pkcs8PrivateKeyDecryptor.decrypt(bytes, password);
  }

  /**
   * Parse a private key from the specified string.
   *
   * @param key the private key to parse
   * @return the parsed private key
   */
  static PrivateKey parse(String key) {
    return parse(key, null);
  }

  /**
   * Parse a private key from the specified string, using the provided password for
   * decryption if necessary.
   *
   * @param key the private key to parse
   * @param password the password used to decrypt an encrypted private key
   * @return the parsed private key
   */
  @Nullable
  static PrivateKey parse(@Nullable String key, @Nullable String password) {
    if (key == null) {
      return null;
    }
    try {
      for (PemParser pemParser : PEM_PARSERS) {
        PrivateKey privateKey = pemParser.parse(key, password);
        if (privateKey != null) {
          return privateKey;
        }
      }
      throw new IllegalStateException("Unrecognized private key format");
    }
    catch (Exception ex) {
      throw new IllegalStateException("Error loading private key file: " + ex.getMessage(), ex);
    }
  }

  /**
   * Parser for a specific PEM format.
   */
  private static class PemParser {

    private final Pattern pattern;

    private final BiFunction<byte[], String, PKCS8EncodedKeySpec> keySpecFactory;

    private final String[] algorithms;

    PemParser(String header, String footer,
            BiFunction<byte[], String, PKCS8EncodedKeySpec> keySpecFactory, String... algorithms) {
      this.pattern = Pattern.compile(header + BASE64_TEXT + footer, Pattern.CASE_INSENSITIVE);
      this.keySpecFactory = keySpecFactory;
      this.algorithms = algorithms;
    }

    @Nullable
    PrivateKey parse(String text, String password) {
      Matcher matcher = this.pattern.matcher(text);
      return (!matcher.find()) ? null : parse(decodeBase64(matcher.group(BASE64_TEXT_GROUP)), password);
    }

    private static byte[] decodeBase64(String content) {
      byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
      return Base64.getDecoder().decode(contentBytes);
    }

    @Nullable
    private PrivateKey parse(byte[] bytes, String password) {
      PKCS8EncodedKeySpec keySpec = this.keySpecFactory.apply(bytes, password);
      for (String algorithm : this.algorithms) {
        try {
          KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
          return keyFactory.generatePrivate(keySpec);
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException ignored) {

        }
      }
      return null;
    }

  }

  /**
   * Simple ASN.1 DER encoder.
   */
  static class DerEncoder {

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    void objectIdentifier(@Nullable int... encodedObjectIdentifier) throws IOException {
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

    @Nullable
    private static byte[] bytes(@Nullable int... elements) {
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

  /**
   * An ASN.1 DER encoded element.
   */
  static final class DerElement {

    private final ValueType valueType;

    private final long tagType;

    private final ByteBuffer contents;

    private DerElement(ByteBuffer bytes) {
      byte b = bytes.get();
      this.valueType = ((b & 0x20) == 0) ? ValueType.PRIMITIVE : ValueType.ENCODED;
      this.tagType = decodeTagType(b, bytes);
      int length = decodeLength(bytes);
      bytes.limit(bytes.position() + length);
      this.contents = bytes.slice();
      bytes.limit(bytes.capacity());
      bytes.position(bytes.position() + length);
    }

    private long decodeTagType(byte b, ByteBuffer bytes) {
      long tagType = (b & 0x1F);
      if (tagType != 0x1F) {
        return tagType;
      }
      tagType = 0;
      b = bytes.get();
      while ((b & 0x80) != 0) {
        tagType <<= 7;
        tagType = tagType | (b & 0x7F);
        b = bytes.get();
      }
      return tagType;
    }

    private int decodeLength(ByteBuffer bytes) {
      byte b = bytes.get();
      if ((b & 0x80) == 0) {
        return b & 0x7F;
      }
      int numberOfLengthBytes = (b & 0x7F);
      Assert.state(numberOfLengthBytes != 0, "Infinite length encoding is not supported");
      Assert.state(numberOfLengthBytes != 0x7F, "Reserved length encoding is not supported");
      Assert.state(numberOfLengthBytes <= 4, "Length overflow");
      int length = 0;
      for (int i = 0; i < numberOfLengthBytes; i++) {
        length <<= 8;
        length |= (bytes.get() & 0xFF);
      }
      return length;
    }

    boolean isType(ValueType valueType) {
      return this.valueType == valueType;
    }

    boolean isType(ValueType valueType, TagType tagType) {
      return this.valueType == valueType && this.tagType == tagType.getNumber();
    }

    ByteBuffer getContents() {
      return this.contents;
    }

    @Nullable
    static DerElement of(byte[] bytes) {
      return of(ByteBuffer.wrap(bytes));
    }

    @Nullable
    static DerElement of(ByteBuffer bytes) {
      return (bytes.remaining() > 0) ? new DerElement(bytes) : null;
    }

    enum ValueType {

      PRIMITIVE, ENCODED

    }

    enum TagType {

      INTEGER(0x02), OCTET_STRING(0x04), OBJECT_IDENTIFIER(0x06), SEQUENCE(0x10);

      private final int number;

      TagType(int number) {
        this.number = number;
      }

      int getNumber() {
        return this.number;
      }

    }

  }

  /**
   * Decryptor for PKCS8 encoded private keys.
   */
  static class Pkcs8PrivateKeyDecryptor {

    public static final String PBES2_ALGORITHM = "PBES2";

    static PKCS8EncodedKeySpec decrypt(byte[] bytes, String password) {
      Assert.notNull(password, "Password is required for an encrypted private key");
      try {
        EncryptedPrivateKeyInfo keyInfo = new EncryptedPrivateKeyInfo(bytes);
        AlgorithmParameters algorithmParameters = keyInfo.getAlgParameters();
        String encryptionAlgorithm = getEncryptionAlgorithm(algorithmParameters, keyInfo.getAlgName());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptionAlgorithm);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameters);
        return keyInfo.getKeySpec(cipher);
      }
      catch (IOException | GeneralSecurityException ex) {
        throw new IllegalArgumentException("Error decrypting private key", ex);
      }
    }

    private static String getEncryptionAlgorithm(AlgorithmParameters algParameters, String algName) {
      if (algParameters != null && PBES2_ALGORITHM.equals(algName)) {
        return algParameters.toString();
      }
      return algName;
    }

  }

}
