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

package cn.taketoday.mock.api;

/**
 * Provides information about the connection made to the mock container. This interface is intended primarily for
 * debugging purposes and as such provides the raw information as seen by the container. Unless explicitly stated
 * otherwise in the Javadoc for a method, no adjustment is made for the presence of reverse proxies or similar
 * configurations.
 */
public interface MockConnection {

  /**
   * Obtain a unique (within the lifetime of the JVM) identifier string for the network connection to the JVM that is
   * being used for the {@code ServletRequest} from which this {@code ServletConnection} was obtained.
   * <p>
   * There is no defined format for this string. The format is implementation dependent.
   *
   * @return A unique identifier for the network connection
   */
  String getConnectionId();

  /**
   * Obtain the name of the protocol as presented to the server after the removal, if present, of any TLS or similar
   * encryption. This may not be the same as the protocol seen by the application. For example, a reverse proxy may
   * present AJP whereas the application will see HTTP 1.1.
   * <p>
   * If the protocol has an entry in the <a href=
   * "https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids">IANA
   * registry for ALPN names</a> then the identification sequence, in string form, must be returned. Registered
   * identification sequences MUST only be used for the associated protocol. Return values for other protocols are
   * implementation dependent. Unknown protocols should return the string "unknown".
   *
   * @return The name of the protocol presented to the server after decryption of TLS, or similar encryption, if any.
   */
  String getProtocol();

  /**
   * Obtain the connection identifier for the network connection to the server that is being used for the
   * {@code ServletRequest} from which this {@code ServletConnection} was obtained as defined by the protocol in use. Note
   * that some protocols do not define such an identifier.
   * <p>
   * Examples of protocol provided connection identifiers include:
   * <dl>
   * <dt>HTTP 1.x</dt>
   * <dd>None, so the empty string should be returned</dd>
   * <dt>HTTP 2</dt>
   * <dd>None, so the empty string should be returned</dd>
   * <dt>HTTP 3</dt>
   * <dd>The QUIC connection ID</dd>
   * <dt>AJP</dt>
   * <dd>None, so the empty string should be returned</dd>
   * </dl>
   *
   * @return The connection identifier if one is defined, otherwise an empty string
   */
  String getProtocolConnectionId();

  /**
   * Determine whether or not the incoming network connection to the server used encryption or not. Note that where a
   * reverse proxy is used, the application may have a different view as to whether encryption is being used due to the
   * use of headers like {@code X-Forwarded-Proto}.
   *
   * @return {@code true} if the incoming network connection used encryption, otherwise {@code false}
   */
  boolean isSecure();
}
