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

package cn.taketoday.annotation.config.ssl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Helper used to match certificates against a {@link PrivateKey}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class CertificateMatcher {

  private static final byte[] DATA = new byte[256];

  static {
    for (int i = 0; i < DATA.length; i++) {
      DATA[i] = (byte) i;
    }
  }

  private final PrivateKey privateKey;

  private final Signature signature;

  @Nullable
  private final byte[] generatedSignature;

  CertificateMatcher(PrivateKey privateKey) {
    Assert.notNull(privateKey, "Private key is required");
    this.privateKey = privateKey;
    this.signature = createSignature(privateKey);
    Assert.notNull(signature, "Failed to create signature");
    this.generatedSignature = sign(this.signature, privateKey);
  }

  @Nullable
  private Signature createSignature(PrivateKey privateKey) {
    try {
      String algorithm = getSignatureAlgorithm(privateKey);
      return (algorithm != null) ? Signature.getInstance(algorithm) : null;
    }
    catch (NoSuchAlgorithmException ex) {
      return null;
    }
  }

  @Nullable
  private static String getSignatureAlgorithm(PrivateKey privateKey) {
    // https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms
    // https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keypairgenerator-algorithms
    return switch (privateKey.getAlgorithm()) {
      case "RSA" -> "SHA256withRSA";
      case "DSA" -> "SHA256withDSA";
      case "EC" -> "SHA256withECDSA";
      case "EdDSA" -> "EdDSA";
      default -> null;
    };
  }

  boolean matchesAny(List<? extends Certificate> certificates) {
    return (this.generatedSignature != null) && certificates.stream().anyMatch(this::matches);
  }

  boolean matches(Certificate certificate) {
    return matches(certificate.getPublicKey());
  }

  private boolean matches(PublicKey publicKey) {
    return (this.generatedSignature != null)
            && Objects.equals(this.privateKey.getAlgorithm(), publicKey.getAlgorithm()) && verify(publicKey);
  }

  private boolean verify(PublicKey publicKey) {
    try {
      this.signature.initVerify(publicKey);
      this.signature.update(DATA);
      return this.signature.verify(this.generatedSignature);
    }
    catch (InvalidKeyException | SignatureException ex) {
      return false;
    }
  }

  @Nullable
  private static byte[] sign(Signature signature, PrivateKey privateKey) {
    try {
      signature.initSign(privateKey);
      signature.update(DATA);
      return signature.sign();
    }
    catch (InvalidKeyException | SignatureException ex) {
      return null;
    }
  }

}
