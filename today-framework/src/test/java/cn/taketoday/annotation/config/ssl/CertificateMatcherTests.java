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

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/6 23:05
 */
class CertificateMatcherTests {

  @CertificateMatchingTest
  void matchesWhenMatchReturnsTrue(CertificateMatchingTestSource source) {
    CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
    assertThat(matcher.matches(source.matchingCertificate())).isTrue();
  }

  @CertificateMatchingTest
  void matchesWhenNoMatchReturnsFalse(CertificateMatchingTestSource source) {
    CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
    for (Certificate nonMatchingCertificate : source.nonMatchingCertificates()) {
      assertThat(matcher.matches(nonMatchingCertificate)).isFalse();
    }
  }

  @CertificateMatchingTest
  void matchesAnyWhenNoneMatchReturnsFalse(CertificateMatchingTestSource source) {
    CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
    assertThat(matcher.matchesAny(source.nonMatchingCertificates())).isFalse();
  }

  @CertificateMatchingTest
  void matchesAnyWhenOneMatchesReturnsTrue(CertificateMatchingTestSource source) {
    CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
    List<Certificate> certificates = new ArrayList<>(source.nonMatchingCertificates());
    certificates.add(source.matchingCertificate());
    assertThat(matcher.matchesAny(certificates)).isTrue();
  }

}