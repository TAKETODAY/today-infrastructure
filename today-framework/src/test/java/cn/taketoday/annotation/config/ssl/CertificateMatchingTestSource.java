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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Source used with {@link CertificateMatchingTest @CertificateMatchingTest} annotated
 * tests that provides access to useful test material.
 *
 * @param algorithm the algorithm
 * @param privateKey the private key to use for matching
 * @param matchingCertificate a certificate that matches the private key
 * @param nonMatchingCertificates a list of certificate that do not match the private key
 * @param nonMatchingPrivateKeys a list of private keys that do not match the certificate
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
record CertificateMatchingTestSource(CertificateMatchingTestSource.Algorithm algorithm, PrivateKey privateKey,
                                     X509Certificate matchingCertificate, List<X509Certificate> nonMatchingCertificates,
                                     List<PrivateKey> nonMatchingPrivateKeys) {

  private static final List<Algorithm> ALGORITHMS;

  static {
    List<Algorithm> algorithms = new ArrayList<>();
    Stream.of("RSA", "DSA", "ed25519", "ed448").map(Algorithm::of).forEach(algorithms::add);
    Stream.of("secp256r1", "secp521r1").map(Algorithm::ec).forEach(algorithms::add);
    ALGORITHMS = List.copyOf(algorithms);
  }

  CertificateMatchingTestSource(Algorithm algorithm, KeyPair matchingKeyPair, List<KeyPair> nonMatchingKeyPairs) {
    this(algorithm, matchingKeyPair.getPrivate(), asCertificate(matchingKeyPair),
            nonMatchingKeyPairs.stream().map(CertificateMatchingTestSource::asCertificate).toList(),
            nonMatchingKeyPairs.stream().map(KeyPair::getPrivate).toList());
  }

  private static X509Certificate asCertificate(KeyPair keyPair) {
    X509Certificate certificate = mock(X509Certificate.class);
    given(certificate.getPublicKey()).willReturn(keyPair.getPublic());
    return certificate;
  }

  @Override
  public String toString() {
    return this.algorithm.toString();
  }

  static List<CertificateMatchingTestSource> create()
          throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    Map<Algorithm, KeyPair> keyPairs = new LinkedHashMap<>();
    for (Algorithm algorithm : ALGORITHMS) {
      keyPairs.put(algorithm, algorithm.generateKeyPair());
    }
    List<CertificateMatchingTestSource> parameters = new ArrayList<>();
    keyPairs.forEach((algorith, matchingKeyPair) -> {
      List<KeyPair> nonMatchingKeyPairs = new ArrayList<>(keyPairs.values());
      nonMatchingKeyPairs.remove(matchingKeyPair);
      parameters.add(new CertificateMatchingTestSource(algorith, matchingKeyPair, nonMatchingKeyPairs));
    });
    return List.copyOf(parameters);
  }

  /**
   * An individual algorithm.
   *
   * @param name the algorithm name
   * @param spec the algorithm spec or {@code null}
   */
  record Algorithm(String name, AlgorithmParameterSpec spec) {

    KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(this.name);
      if (this.spec != null) {
        generator.initialize(this.spec);
      }
      return generator.generateKeyPair();
    }

    @Override
    public String toString() {
      String spec = (this.spec instanceof NamedParameterSpec namedSpec) ? namedSpec.getName() : "";
      return this.name + ((!spec.isEmpty()) ? ":" + spec : "");
    }

    static Algorithm of(String name) {
      return new Algorithm(name, null);
    }

    static Algorithm ec(String curve) {
      return new Algorithm("EC", new ECGenParameterSpec(curve));
    }

  }

}
