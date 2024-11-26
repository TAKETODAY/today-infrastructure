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

package infra.retry.backoff;

/**
 * Simple {@link Sleeper} implementation that just blocks the current Thread with sleep
 * period.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ThreadWaitSleeper implements Sleeper {

  @Override
  public void sleep(long backOffPeriod) throws InterruptedException {
    Thread.sleep(backOffPeriod);
  }

}
