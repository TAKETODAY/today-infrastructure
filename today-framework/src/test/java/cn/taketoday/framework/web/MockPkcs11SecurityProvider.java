/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web;

import java.io.Serial;
import java.security.Provider;

/**
 * Mock PKCS#11 Security Provider for testing purposes.
 *
 * @author Cyril Dangerville
 */
public class MockPkcs11SecurityProvider extends Provider {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The name of the mock provider.
   */
  public static final String NAME = "Mock-PKCS11";

  static final MockPkcs11SecurityProvider INSTANCE = new MockPkcs11SecurityProvider();

  MockPkcs11SecurityProvider() {
    super(NAME, "0.1", "Mock PKCS11 Provider");
    putService(new Service(this, "KeyStore", "PKCS11", MockKeyStoreSpi.class.getName(), null, null));
  }

}
