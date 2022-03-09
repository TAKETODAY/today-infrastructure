/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.util.backoff;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 11:30
 */
class FixedBackOffTests {

	@Test
	void defaultInstance() {
		FixedBackOff backOff = new FixedBackOff();
		BackOffExecution execution = backOff.start();
		for (int i = 0; i < 100; i++) {
			assertThat(execution.nextBackOff()).isEqualTo(FixedBackOff.DEFAULT_INTERVAL);
		}
	}

	@Test
	void noAttemptAtAll() {
		FixedBackOff backOff = new FixedBackOff(100L, 0L);
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void maxAttemptsReached() {
		FixedBackOff backOff = new FixedBackOff(200L, 2);
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(200L);
		assertThat(execution.nextBackOff()).isEqualTo(200L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void startReturnDifferentInstances() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		BackOffExecution execution = backOff.start();
		BackOffExecution execution2 = backOff.start();

		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution2.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
		assertThat(execution2.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void liveUpdate() {
		FixedBackOff backOff = new FixedBackOff(100L, 1);
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(100L);

		backOff.setInterval(200L);
		backOff.setMaxAttempts(2);

		assertThat(execution.nextBackOff()).isEqualTo(200L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void toStringContent() {
		FixedBackOff backOff = new FixedBackOff(200L, 10);
		BackOffExecution execution = backOff.start();
		assertThat(execution.toString()).isEqualTo("FixedBackOff{interval=200, currentAttempts=0, maxAttempts=10}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("FixedBackOff{interval=200, currentAttempts=1, maxAttempts=10}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("FixedBackOff{interval=200, currentAttempts=2, maxAttempts=10}");
	}

}
