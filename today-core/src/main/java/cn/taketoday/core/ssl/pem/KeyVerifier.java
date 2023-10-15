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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import cn.taketoday.lang.Nullable;

/**
 * Performs checks on keys, e.g., if a public key and a private key belong together.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class KeyVerifier {

  private static final byte[] DATA = "Just some piece of data which gets signed".getBytes(StandardCharsets.UTF_8);

  /**
   * Checks if the given private key belongs to the given public key.
   *
   * @param privateKey the private key
   * @param publicKey the public key
   * @return whether the keys belong together
   */
  Result matches(PrivateKey privateKey, PublicKey publicKey) {
    try {
      if (!privateKey.getAlgorithm().equals(publicKey.getAlgorithm())) {
        // Keys are of different type
        return Result.NO;
      }
      String algorithm = getSignatureAlgorithm(privateKey.getAlgorithm());
      if (algorithm == null) {
        return Result.UNKNOWN;
      }
      byte[] signature = createSignature(privateKey, algorithm);
      return verifySignature(publicKey, algorithm, signature);
    }
    catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
      return Result.UNKNOWN;
    }
  }

  private static byte[] createSignature(PrivateKey privateKey, String algorithm)
          throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    Signature signer = Signature.getInstance(algorithm);
    signer.initSign(privateKey);
    signer.update(DATA);
    return signer.sign();
  }

  private static Result verifySignature(PublicKey publicKey, String algorithm, byte[] signature)
          throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    Signature verifier = Signature.getInstance(algorithm);
    verifier.initVerify(publicKey);
    verifier.update(DATA);
    try {
      if (verifier.verify(signature)) {
        return Result.YES;
      }
      else {
        return Result.NO;
      }
    }
    catch (SignatureException ex) {
      return Result.NO;
    }
  }

  @Nullable
  private static String getSignatureAlgorithm(String keyAlgorithm) {
    // https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms
    // https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keypairgenerator-algorithms
    return switch (keyAlgorithm) {
      case "RSA" -> "SHA256withRSA";
      case "DSA" -> "SHA256withDSA";
      case "EC" -> "SHA256withECDSA";
      case "EdDSA" -> "EdDSA";
      default -> null;
    };
  }

  enum Result {

    YES, NO, UNKNOWN

  }

}
