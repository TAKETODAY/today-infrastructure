/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 00:37
 */
class DefaultSslInfoTests {

  @Test
  void constructor_withSessionIdAndCertificates_shouldSetFields() {
    String sessionId = "test-session-id";
    X509Certificate[] certificates = new X509Certificate[] { mock(X509Certificate.class) };

    DefaultSslInfo sslInfo = new DefaultSslInfo(sessionId, certificates);

    assertThat(sslInfo.getSessionId()).isEqualTo(sessionId);
    assertThat(sslInfo.getPeerCertificates()).isEqualTo(certificates);
  }

  @Test
  void constructor_withNullCertificates_shouldThrowException() {
    assertThatThrownBy(() -> new DefaultSslInfo("session-id", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No SSL certificates");
  }

  @Test
  void constructor_withSSLSession_shouldInitializeFromSession() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    byte[] sessionIdBytes = new byte[] { 0x01, 0x02, 0x03 };
    when(session.getId()).thenReturn(sessionIdBytes);

    X509Certificate cert1 = mock(X509Certificate.class);
    X509Certificate cert2 = mock(X509Certificate.class);
    Certificate[] certificates = new Certificate[] { cert1, cert2 };
    when(session.getPeerCertificates()).thenReturn(certificates);

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getSessionId()).isEqualTo("010203");
    assertThat(sslInfo.getPeerCertificates()).containsExactly(cert1, cert2);
  }

  @Test
  void constructor_withNullSSLSession_shouldThrowException() {
    assertThatThrownBy(() -> new DefaultSslInfo((SSLSession) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SSLSession is required");
  }

  @Test
  void getSessionId_whenSessionHasId_shouldReturnHexString() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    byte[] sessionIdBytes = new byte[] { (byte) 0xAB, (byte) 0xCD, 0x12 };
    when(session.getId()).thenReturn(sessionIdBytes);

    X509Certificate cert = mock(X509Certificate.class);
    when(session.getPeerCertificates()).thenReturn(new Certificate[] { cert });

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getSessionId()).isEqualTo("abcd12");
  }

  @Test
  void getSessionId_whenSessionIdIsNull_shouldReturnNull() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(null);

    X509Certificate cert = mock(X509Certificate.class);
    when(session.getPeerCertificates()).thenReturn(new Certificate[] { cert });

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getSessionId()).isNull();
  }

  @Test
  void getSessionId_whenSessionIdIsEmpty_shouldReturnEmptyString() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(new byte[0]);

    X509Certificate cert = mock(X509Certificate.class);
    when(session.getPeerCertificates()).thenReturn(new Certificate[] { cert });

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getSessionId()).isEqualTo("");
  }

  @Test
  void getPeerCertificates_whenAllCertificatesAreX509_shouldReturnAll() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(new byte[0]);

    X509Certificate cert1 = mock(X509Certificate.class);
    X509Certificate cert2 = mock(X509Certificate.class);
    Certificate[] certificates = new Certificate[] { cert1, cert2 };
    when(session.getPeerCertificates()).thenReturn(certificates);

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getPeerCertificates()).containsExactly(cert1, cert2);
  }

  @Test
  void getPeerCertificates_whenMixedCertificateTypes_shouldReturnOnlyX509() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(new byte[0]);

    X509Certificate x509Cert = mock(X509Certificate.class);
    Certificate nonX509Cert = mock(Certificate.class);
    Certificate[] certificates = new Certificate[] { x509Cert, nonX509Cert };
    when(session.getPeerCertificates()).thenReturn(certificates);

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getPeerCertificates()).containsExactly(x509Cert);
  }

  @Test
  void getPeerCertificates_whenNoX509Certificates_shouldReturnNull() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(new byte[0]);

    Certificate nonX509Cert = mock(Certificate.class);
    Certificate[] certificates = new Certificate[] { nonX509Cert };
    when(session.getPeerCertificates()).thenReturn(certificates);

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getPeerCertificates()).isNull();
  }

  @Test
  void getPeerCertificates_whenGetPeerCertificatesThrows_shouldReturnNull() throws Exception {
    SSLSession session = mock(SSLSession.class);
    when(session.getId()).thenReturn(new byte[0]);
    when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("Not verified"));

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getPeerCertificates()).isNull();
  }

  @Test
  void initSessionId_whenByteRequiresPadding_shouldPadWithZero() throws SSLPeerUnverifiedException {
    SSLSession session = mock(SSLSession.class);
    byte[] sessionIdBytes = new byte[] { 0x0F, (byte) 0xFF };
    when(session.getId()).thenReturn(sessionIdBytes);

    X509Certificate cert = mock(X509Certificate.class);
    when(session.getPeerCertificates()).thenReturn(new Certificate[] { cert });

    DefaultSslInfo sslInfo = new DefaultSslInfo(session);

    assertThat(sslInfo.getSessionId()).isEqualTo("0fff");
  }
}
