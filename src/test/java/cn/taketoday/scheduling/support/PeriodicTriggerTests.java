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

package cn.taketoday.scheduling.support;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.util.NumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class PeriodicTriggerTests {

	@Test
	public void fixedDelayFirstExecution() {
		Date now = new Date();
		PeriodicTrigger trigger = new PeriodicTrigger(5000);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	public void fixedDelayWithInitialDelayFirstExecution() {
		Date now = new Date();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay);
	}

	@Test
	public void fixedDelayWithTimeUnitFirstExecution() {
		Date now = new Date();
		PeriodicTrigger trigger = new PeriodicTrigger(5, TimeUnit.SECONDS);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	public void fixedDelayWithTimeUnitAndInitialDelayFirstExecution() {
		Date now = new Date();
		long period = 5;
		long initialDelay = 30;
		PeriodicTrigger trigger = new PeriodicTrigger(period, TimeUnit.SECONDS);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay * 1000);
	}

	@Test
	public void fixedDelaySubsequentExecution() {
		Date now = new Date();
		long period = 5000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, period + 3000);
	}

	@Test
	public void fixedDelayWithInitialDelaySubsequentExecution() {
		Date now = new Date();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, period + 3000);
	}

	@Test
	public void fixedDelayWithTimeUnitSubsequentExecution() {
		Date now = new Date();
		long period = 5;
		PeriodicTrigger trigger = new PeriodicTrigger(period, TimeUnit.SECONDS);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, (period * 1000) + 3000);
	}

	@Test
	public void fixedRateFirstExecution() {
		Date now = new Date();
		PeriodicTrigger trigger = new PeriodicTrigger(5000);
		trigger.setFixedRate(true);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	public void fixedRateWithTimeUnitFirstExecution() {
		Date now = new Date();
		PeriodicTrigger trigger = new PeriodicTrigger(5, TimeUnit.SECONDS);
		trigger.setFixedRate(true);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertNegligibleDifference(now, next);
	}

	@Test
	public void fixedRateWithInitialDelayFirstExecution() {
		Date now = new Date();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setFixedRate(true);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertApproximateDifference(now, next, initialDelay);
	}

	@Test
	public void fixedRateWithTimeUnitAndInitialDelayFirstExecution() {
		Date now = new Date();
		long period = 5;
		long initialDelay = 30;
		PeriodicTrigger trigger = new PeriodicTrigger(period, TimeUnit.MINUTES);
		trigger.setFixedRate(true);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(null, null, null));
		assertApproximateDifference(now, next, (initialDelay * 60 * 1000));
	}

	@Test
	public void fixedRateSubsequentExecution() {
		Date now = new Date();
		long period = 5000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setFixedRate(true);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, period);
	}

	@Test
	public void fixedRateWithInitialDelaySubsequentExecution() {
		Date now = new Date();
		long period = 5000;
		long initialDelay = 30000;
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setFixedRate(true);
		trigger.setInitialDelay(initialDelay);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, period);
	}

	@Test
	public void fixedRateWithTimeUnitSubsequentExecution() {
		Date now = new Date();
		long period = 5;
		PeriodicTrigger trigger = new PeriodicTrigger(period, TimeUnit.HOURS);
		trigger.setFixedRate(true);
		Date next = trigger.nextExecutionTime(context(now, 500, 3000));
		assertApproximateDifference(now, next, (period * 60 * 60 * 1000));
	}

	@Test
	public void equalsVerification() {
		PeriodicTrigger trigger1 = new PeriodicTrigger(3000);
		PeriodicTrigger trigger2 = new PeriodicTrigger(3000);
		assertThat(trigger1.equals(new String("not a trigger"))).isFalse();
		assertThat(trigger1.equals(null)).isFalse();
		assertThat(trigger1).isEqualTo(trigger1);
		assertThat(trigger2).isEqualTo(trigger2);
		assertThat(trigger2).isEqualTo(trigger1);
		trigger2.setInitialDelay(1234);
		assertThat(trigger1.equals(trigger2)).isFalse();
		assertThat(trigger2.equals(trigger1)).isFalse();
		trigger1.setInitialDelay(1234);
		assertThat(trigger2).isEqualTo(trigger1);
		trigger2.setFixedRate(true);
		assertThat(trigger1.equals(trigger2)).isFalse();
		assertThat(trigger2.equals(trigger1)).isFalse();
		trigger1.setFixedRate(true);
		assertThat(trigger2).isEqualTo(trigger1);
		PeriodicTrigger trigger3 = new PeriodicTrigger(3, TimeUnit.SECONDS);
		trigger3.setInitialDelay(7);
		trigger3.setFixedRate(true);
		assertThat(trigger1.equals(trigger3)).isFalse();
		assertThat(trigger3.equals(trigger1)).isFalse();
		trigger1.setInitialDelay(7000);
		assertThat(trigger3).isEqualTo(trigger1);
	}


	// utility methods

	private static void assertNegligibleDifference(Date d1, Date d2) {
		long diff = Math.abs(d1.getTime() - d2.getTime());
		assertThat(diff < 100).as("difference exceeds threshold: " + diff).isTrue();
	}

	private static void assertApproximateDifference(Date lesser, Date greater, long expected) {
		long diff = greater.getTime() - lesser.getTime();
		long variance = Math.abs(expected - diff);
		assertThat(variance < 100).as("expected approximate difference of " + expected +
				", but actual difference was " + diff).isTrue();
	}

	private static TriggerContext context(Object scheduled, Object actual, Object completion) {
		return new TestTriggerContext(asDate(scheduled), asDate(actual), asDate(completion));
	}

	private static Date asDate(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Date) {
			return (Date) o;
		}
		if (o instanceof Number) {
			return new Date(System.currentTimeMillis() +
					NumberUtils.convertNumberToTargetClass((Number) o, Long.class));
		}
		throw new IllegalArgumentException(
				"expected Date or Number, but actual type was: " + o.getClass());
	}


	// helper class

	private static class TestTriggerContext implements TriggerContext {

		private final Date scheduled;

		private final Date actual;

		private final Date completion;

		TestTriggerContext(Date scheduled, Date actual, Date completion) {
			this.scheduled = scheduled;
			this.actual = actual;
			this.completion = completion;
		}

		@Override
		public Date lastActualExecutionTime() {
			return this.actual;
		}

		@Override
		public Date lastCompletionTime() {
			return this.completion;
		}

		@Override
		public Date lastScheduledExecutionTime() {
			return this.scheduled;
		}
	}

}
