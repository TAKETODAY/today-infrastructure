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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 11:33
 */
class ExponentialBackOffTests {

	@Test
	void defaultInstance() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(3000L);
		assertThat(execution.nextBackOff()).isEqualTo(4500L);
	}

	@Test
	void simpleIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 2.0);
		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(200L);
		assertThat(execution.nextBackOff()).isEqualTo(400L);
		assertThat(execution.nextBackOff()).isEqualTo(800L);
	}

	@Test
	void fixedIncrease() {
		ExponentialBackOff backOff = new ExponentialBackOff(100L, 1.0);
		backOff.setMaxElapsedTime(300L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(100L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void maxIntervalReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxInterval(4000L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		// max reached
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
	}

	@Test
	void maxAttemptsReached() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		// > 4 sec wait in total
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void startReturnDifferentInstances() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(2000L);
		backOff.setMultiplier(2.0);
		backOff.setMaxElapsedTime(4000L);

		BackOffExecution execution = backOff.start();
		BackOffExecution execution2 = backOff.start();

		assertThat(execution.nextBackOff()).isEqualTo(2000L);
		assertThat(execution2.nextBackOff()).isEqualTo(2000L);
		assertThat(execution.nextBackOff()).isEqualTo(4000L);
		assertThat(execution2.nextBackOff()).isEqualTo(4000L);
		assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
		assertThat(execution2.nextBackOff()).isEqualTo(BackOffExecution.STOP);
	}

	@Test
	void invalidInterval() {
		ExponentialBackOff backOff = new ExponentialBackOff();
		assertThatIllegalArgumentException().isThrownBy(() ->
				backOff.setMultiplier(0.9));
	}

	@Test
	void maxIntervalReachedImmediately() {
		ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
		backOff.setMaxInterval(50L);

		BackOffExecution execution = backOff.start();
		assertThat(execution.nextBackOff()).isEqualTo(50L);
		assertThat(execution.nextBackOff()).isEqualTo(50L);
	}

	@Test
	void toStringContent() {
		ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
		BackOffExecution execution = backOff.start();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOff{currentInterval=n/a, multiplier=2.0}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOff{currentInterval=2000ms, multiplier=2.0}");
		execution.nextBackOff();
		assertThat(execution.toString()).isEqualTo("ExponentialBackOff{currentInterval=4000ms, multiplier=2.0}");
	}

}
