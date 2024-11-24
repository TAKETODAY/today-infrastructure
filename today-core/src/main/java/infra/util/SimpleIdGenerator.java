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

package infra.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple {@link IdGenerator} that starts at 1, increments up to
 * {@link Long#MAX_VALUE}, and then rolls over.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/9/11 17:52
 * @since 4.0
 */
public class SimpleIdGenerator implements IdGenerator {
  private final long mostSigBits;
  private final AtomicLong leastSigBits = new AtomicLong();

  public SimpleIdGenerator() {
    this(0);
  }

  public SimpleIdGenerator(long mostSigBits) {
    this.mostSigBits = mostSigBits;
  }

  @Override
  public UUID generateId() {
    return new UUID(mostSigBits, this.leastSigBits.incrementAndGet());
  }

  public long getMostSigBits() {
    return mostSigBits;
  }

}
