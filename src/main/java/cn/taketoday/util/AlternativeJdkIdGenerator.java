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

package cn.taketoday.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * An {@link IdGenerator} that uses {@link SecureRandom} for the initial seed and
 * {@link Random} thereafter, instead of calling {@link UUID#randomUUID()} every
 * time as {@link JdkIdGenerator JdkIdGenerator} does.
 * This provides a better balance between securely random ids and performance.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author TODAY 2021/9/11 17:51
 * @since 4.0
 */
public class AlternativeJdkIdGenerator implements IdGenerator {

  private final Random random;

  public AlternativeJdkIdGenerator() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] seed = new byte[8];
    secureRandom.nextBytes(seed);
    this.random = new Random(new BigInteger(seed).longValue());
  }

  @Override
  public UUID generateId() {
    byte[] randomBytes = new byte[16];
    this.random.nextBytes(randomBytes);

    long mostSigBits = 0;
    for (int i = 0; i < 8; i++) {
      mostSigBits = (mostSigBits << 8) | (randomBytes[i] & 0xff);
    }

    long leastSigBits = 0;
    for (int i = 8; i < 16; i++) {
      leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xff);
    }

    return new UUID(mostSigBits, leastSigBits);
  }

}
