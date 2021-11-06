/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.server.reactive;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSession;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link SslInfo}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
final class DefaultSslInfo implements SslInfo {

	@Nullable
	private final String sessionId;

	@Nullable
	private final X509Certificate[] peerCertificates;


	DefaultSslInfo(@Nullable String sessionId, X509Certificate[] peerCertificates) {
		Assert.notNull(peerCertificates, "No SSL certificates");
		this.sessionId = sessionId;
		this.peerCertificates = peerCertificates;
	}

	DefaultSslInfo(SSLSession session) {
		Assert.notNull(session, "SSLSession is required");
		this.sessionId = initSessionId(session);
		this.peerCertificates = initCertificates(session);
	}


	@Override
	@Nullable
	public String getSessionId() {
		return this.sessionId;
	}

	@Override
	@Nullable
	public X509Certificate[] getPeerCertificates() {
		return this.peerCertificates;
	}


	@Nullable
	private static String initSessionId(SSLSession session) {
		byte [] bytes = session.getId();
		if (bytes == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			String digit = Integer.toHexString(b);
			if (digit.length() < 2) {
				sb.append('0');
			}
			if (digit.length() > 2) {
				digit = digit.substring(digit.length() - 2);
			}
			sb.append(digit);
		}
		return sb.toString();
	}

	@Nullable
	private static X509Certificate[] initCertificates(SSLSession session) {
		Certificate[] certificates;
		try {
			certificates = session.getPeerCertificates();
		}
		catch (Throwable ex) {
			return null;
		}

		List<X509Certificate> result = new ArrayList<>(certificates.length);
		for (Certificate certificate : certificates) {
			if (certificate instanceof X509Certificate) {
				result.add((X509Certificate) certificate);
			}
		}
		return (!result.isEmpty() ? result.toArray(new X509Certificate[0]) : null);
	}

}
