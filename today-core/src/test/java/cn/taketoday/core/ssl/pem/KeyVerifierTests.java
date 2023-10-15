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

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.ssl.pem.KeyVerifier.Result;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 22:26
 */
class KeyVerifierTests {

  private static final List<Algorithm> ALGORITHMS = List.of(Algorithm.of("RSA"), Algorithm.of("DSA"),
          Algorithm.of("ed25519"), Algorithm.of("ed448"), Algorithm.ec("secp256r1"), Algorithm.ec("secp521r1"));

  private final KeyVerifier keyVerifier = new KeyVerifier();

  @ParameterizedTest(name = "{0}")
  @MethodSource("arguments")
  void test(PrivateKey privateKey, PublicKey publicKey, List<PublicKey> invalidPublicKeys) {
    assertThat(this.keyVerifier.matches(privateKey, publicKey)).isEqualTo(Result.YES);
    for (PublicKey invalidPublicKey : invalidPublicKeys) {
      assertThat(this.keyVerifier.matches(privateKey, invalidPublicKey)).isEqualTo(Result.NO);
    }
  }

  static Stream<Arguments> arguments() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    List<KeyPair> keyPairs = new LinkedList<>();
    for (Algorithm algorithm : ALGORITHMS) {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm.name());
      if (algorithm.spec() != null) {
        generator.initialize(algorithm.spec());
      }
      keyPairs.add(generator.generateKeyPair());
      keyPairs.add(generator.generateKeyPair());
    }
    return keyPairs.stream()
            .map((kp) -> Arguments.arguments(Named.named(kp.getPrivate().getAlgorithm(), kp.getPrivate()),
                    kp.getPublic(), without(keyPairs, kp).map(KeyPair::getPublic).toList()));
  }

  private static Stream<KeyPair> without(List<KeyPair> keyPairs, KeyPair without) {
    return keyPairs.stream().filter((kp) -> !kp.equals(without));
  }

  private record Algorithm(String name, @Nullable AlgorithmParameterSpec spec) {

    static Algorithm of(String name) {
      return new Algorithm(name, null);
    }

    static Algorithm ec(String curve) {
      return new Algorithm("EC", new ECGenParameterSpec(curve));
    }
  }

}