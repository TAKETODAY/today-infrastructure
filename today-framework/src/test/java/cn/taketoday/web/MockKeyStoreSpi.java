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

package cn.taketoday.web;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock Security Provider for testing purposes.
 *
 * @author Cyril Dangerville
 */
public class MockKeyStoreSpi extends KeyStoreSpi {

  private static final KeyPairGenerator KEYGEN;

  static {
    try {
      KEYGEN = KeyPairGenerator.getInstance("RSA");
      KEYGEN.initialize(2048);
    }
    catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private final Map<String, KeyPair> aliases = new HashMap<>();

  @Override
  public Key engineGetKey(String alias, char[] password) {
    final KeyPair keyPair = this.aliases.get(alias);
    return (keyPair != null) ? keyPair.getPrivate() : null;
  }

  @Override
  public Certificate[] engineGetCertificateChain(String alias) {
    return new Certificate[0];
  }

  @Override
  public Certificate engineGetCertificate(String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date engineGetCreationDate(String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineSetCertificateEntry(String alias, Certificate cert) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineDeleteEntry(String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<String> engineAliases() {
    return Collections.enumeration(this.aliases.keySet());
  }

  @Override
  public boolean engineContainsAlias(String alias) {
    this.aliases.put(alias, KEYGEN.generateKeyPair());
    return true;
  }

  @Override
  public int engineSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean engineIsKeyEntry(String alias) {
    return this.aliases.containsKey(alias);
  }

  @Override
  public boolean engineIsCertificateEntry(String alias) {
    return false;
  }

  @Override
  public String engineGetCertificateAlias(Certificate cert) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineStore(OutputStream stream, char[] password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void engineLoad(InputStream stream, char[] password) {
  }

}
