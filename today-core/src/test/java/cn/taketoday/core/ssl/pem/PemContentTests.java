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

package cn.taketoday.core.ssl.pem;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PemContent}.
 *
 * @author Phillip Webb
 */
class PemContentTests {

  @Test
  void getCertificateWhenNoCertificatesThrowsException() {
    PemContent content = PemContent.of("");
    assertThatIllegalStateException().isThrownBy(content::getCertificates)
            .withMessage("Missing certificates or unrecognized format");
  }

  @Test
  void getCertificateReturnsCertificates() throws Exception {
    PemContent content = PemContent.load(contentFromClasspath("ssl/test-cert-chain.pem"));
    List<X509Certificate> certificates = content.getCertificates();
    assertThat(certificates).isNotNull();
    assertThat(certificates).hasSize(2);
    assertThat(certificates.get(0).getType()).isEqualTo("X.509");
    assertThat(certificates.get(1).getType()).isEqualTo("X.509");
  }

  @Test
  void getPrivateKeyWhenNoKeyThrowsException() {
    PemContent content = PemContent.of("");
    assertThatIllegalStateException().isThrownBy(content::getPrivateKey)
            .withMessage("Missing private key or unrecognized format");
  }

  @Test
  void getPrivateKeyReturnsPrivateKey() throws Exception {
    PemContent content = PemContent
            .load(contentFromClasspath("/cn/taketoday/core/ssl/pkcs8/dsa.key"));
    PrivateKey privateKey = content.getPrivateKey();
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("DSA");
  }

  @Test
  void equalsAndHashCode() {
    PemContent c1 = PemContent.of("aaa");
    PemContent c2 = PemContent.of("aaa");
    PemContent c3 = PemContent.of("bbb");
    assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    assertThat(c1).isEqualTo(c1).isEqualTo(c2).isNotEqualTo(c3);
  }

  @Test
  void toStringReturnsString() {
    PemContent content = PemContent.of("test");
    assertThat(content).hasToString("test");
  }

  @Test
  void loadWithStringWhenContentIsNullReturnsNull() throws Exception {
    assertThat(PemContent.load((String) null)).isNull();
  }

  @Test
  void loadWithStringWhenContentIsPemContentReturnsContent() throws Exception {
    String content = """
            -----BEGIN CERTIFICATE-----
            MIICpDCCAYwCCQCDOqHKPjAhCTANBgkqhkiG9w0BAQUFADAUMRIwEAYDVQQDDAls
            b2NhbGhvc3QwHhcNMTQwOTEwMjE0MzA1WhcNMTQxMDEwMjE0MzA1WjAUMRIwEAYD
            VQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDR
            0KfxUw7MF/8RB5/YXOM7yLnoHYb/M/6dyoulMbtEdKKhQhU28o5FiDkHcEG9PJQL
            gqrRgAjl3VmCC9omtfZJQ2EpfkTttkJjnKOOroXhYE51/CYSckapBYCVh8GkjUEJ
            uEfnp07cTfYZFqViIgIWPZyjkzl3w4girS7kCuzNdDntVJVx5F/EsFwMA8n3C0Qa
            zHQoM5s00Fer6aTwd6AW0JD5QkADavpfzZ554e4HrVGwHlM28WKQQkFzzGu44FFX
            yVuEF3HeyVPug8GRHAc8UU7ijVgJB5TmbvRGYowIErD5i4VvGLuOv9mgR3aVyN0S
            dJ1N7aJnXpeSQjAgf03jAgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAE4yvwhbPldg
            Bpl7sBw/m2B3bfiNeSqa4tII1PQ7ysgWVb9HbFNKkriScwDWlqo6ljZfJ+SDFCoj
            bQz4fOFdMAOzRnpTrG2NAKMoJLY0/g/p7XO00PiC8T3h3BOJ5SHuW3gUyfGXmAYs
            DnJxJOrwPzj57xvNXjNSbDOJ3DRfCbB0CWBexOeGDiUokoEq3Gnz04Q4ZfHyAcpZ
            3deMw8Od5p9WAoCh3oClpFyOSzXYKZd+3ppMMtfc4wnbfocnfSFxj0UCpOEJw4Ez
            +lGuHKdhNOVW9CmqPD1y76o6c8PQKuF7KZEoY2jvy3GeIfddBvqXgZ4PbWvFz1jO
            32C9XWHwRA4=
            -----END CERTIFICATE-----""";
    assertThat(PemContent.load(content)).hasToString(content);
  }

  @Test
  void loadWithStringWhenClasspathLocationReturnsContent() throws IOException {
    String actual = PemContent.load("classpath:ssl/test-cert.pem").toString();
    String expected = contentFromClasspath("ssl/test-cert.pem");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void loadWithStringWhenFileLocationReturnsContent() throws IOException {
    String actual = PemContent.load("src/test/resources/ssl/test-cert.pem").toString();
    String expected = contentFromClasspath("test-cert.pem");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void loadWithPathReturnsContent() throws IOException {
    Path path = Path.of("src/test/resources/test-cert.pem");
    String actual = PemContent.load(path).toString();
    String expected = contentFromClasspath("test-cert.pem");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ofWhenNullReturnsNull() {
    assertThat(PemContent.of(null)).isNull();
  }

  @Test
  void ofReturnsContent() {
    assertThat(PemContent.of("test")).hasToString("test");
  }

  private static String contentFromClasspath(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

}
