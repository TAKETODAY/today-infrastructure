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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.util.NumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class PeriodicTriggerTests {

  @Test
  void fixedDelayFirstExecution() {
    Instant now = Instant.now();
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(5000));
    Instant next = trigger.nextExecution(context(null, null, null));
    assertNegligibleDifference(now, next);
  }

  @Test
  void fixedDelayWithInitialDelayFirstExecution() {
    Instant now = Instant.now();
    long period = 5000;
    long initialDelay = 30000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    trigger.setInitialDelay(initialDelay);
    Instant next = trigger.nextExecution(context(null, null, null));
    assertApproximateDifference(now, next, initialDelay);
  }

  @Test
  void fixedDelayWithTimeUnitFirstExecution() {
    Instant now = Instant.now();
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(5));
    Instant next = trigger.nextExecution(context(null, null, null));
    assertNegligibleDifference(now, next);
  }

  @Test
  void fixedDelayWithTimeUnitAndInitialDelayFirstExecution() {
    Instant now = Instant.now();
    long period = 5;
    long initialDelay = 30;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(period));
    trigger.setInitialDelay(Duration.ofSeconds(initialDelay));
    Instant next = trigger.nextExecution(context(null, null, null));
    assertApproximateDifference(now, next, initialDelay * 1000);
  }

  @Test
  void fixedDelaySubsequentExecution() {
    Instant now = Instant.now();
    long period = 5000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, period + 3000);
  }

  @Test
  void fixedDelayWithInitialDelaySubsequentExecution() {
    Instant now = Instant.now();
    long period = 5000;
    long initialDelay = 30000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    trigger.setInitialDelay(initialDelay);
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, period + 3000);
  }

  @Test
  void fixedDelayWithTimeUnitSubsequentExecution() {
    Instant now = Instant.now();
    long period = 5;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(period));
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, (period * 1000) + 3000);
  }

  @Test
  void fixedRateFirstExecution() {
    Instant now = Instant.now();
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(5000));
    trigger.setFixedRate(true);
    Instant next = trigger.nextExecution(context(null, null, null));
    assertNegligibleDifference(now, next);
  }

  @Test
  void fixedRateWithTimeUnitFirstExecution() {
    Instant now = Instant.now();
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(5));
    trigger.setFixedRate(true);
    Instant next = trigger.nextExecution(context(null, null, null));
    assertNegligibleDifference(now, next);
  }

  @Test
  void fixedRateWithInitialDelayFirstExecution() {
    Instant now = Instant.now();
    long period = 5000;
    long initialDelay = 30000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    trigger.setFixedRate(true);
    trigger.setInitialDelay(initialDelay);
    Instant next = trigger.nextExecution(context(null, null, null));
    assertApproximateDifference(now, next, initialDelay);
  }

  @Test
  void fixedRateWithTimeUnitAndInitialDelayFirstExecution() {
    Instant now = Instant.now();
    long period = 5;
    long initialDelay = 30;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMinutes(period));
    trigger.setFixedRate(true);
    trigger.setInitialDelay(Duration.ofMinutes(initialDelay));
    Instant next = trigger.nextExecution(context(null, null, null));
    assertApproximateDifference(now, next, (initialDelay * 60 * 1000));
  }

  @Test
  void fixedRateSubsequentExecution() {
    Instant now = Instant.now();
    long period = 5000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    trigger.setFixedRate(true);
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, period);
  }

  @Test
  void fixedRateWithInitialDelaySubsequentExecution() {
    Instant now = Instant.now();
    long period = 5000;
    long initialDelay = 30000;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
    trigger.setFixedRate(true);
    trigger.setInitialDelay(initialDelay);
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, period);
  }

  @Test
  void fixedRateWithTimeUnitSubsequentExecution() {
    Instant now = Instant.now();
    long period = 5;
    PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofHours(period));
    trigger.setFixedRate(true);
    Instant next = trigger.nextExecution(context(now, 500, 3000));
    assertApproximateDifference(now, next, (period * 60 * 60 * 1000));
  }

  @Test
  void equalsVerification() {
    PeriodicTrigger trigger1 = new PeriodicTrigger(Duration.ofMillis(3000));
    PeriodicTrigger trigger2 = new PeriodicTrigger(Duration.ofMillis(3000));
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
    PeriodicTrigger trigger3 = new PeriodicTrigger(Duration.ofSeconds(3));
    trigger3.setInitialDelay(Duration.ofSeconds(7));
    trigger3.setFixedRate(true);
    assertThat(trigger1.equals(trigger3)).isFalse();
    assertThat(trigger3.equals(trigger1)).isFalse();
    trigger1.setInitialDelay(Duration.ofMillis(7000));
    assertThat(trigger3).isEqualTo(trigger1);
  }

  // utility methods

  private static void assertNegligibleDifference(Instant d1, @Nullable Instant d2) {
    assertThat(Duration.between(d1, d2)).isLessThan(Duration.ofMillis(100));
  }

  private static void assertApproximateDifference(Instant lesser, Instant greater, long expected) {
    long diff = greater.toEpochMilli() - lesser.toEpochMilli();
    long variance = Math.abs(expected - diff);
    assertThat(variance < 100).as("expected approximate difference of " + expected +
            ", but actual difference was " + diff).isTrue();
  }

  private static TriggerContext context(@Nullable Object scheduled, @Nullable Object actual,
          @Nullable Object completion) {
    return new TestTriggerContext(toInstant(scheduled), toInstant(actual), toInstant(completion));
  }

  @Nullable
  private static Instant toInstant(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Instant) {
      return (Instant) o;
    }
    if (o instanceof Number) {
      return Instant.now()
              .plus(NumberUtils.convertNumberToTargetClass((Number) o, Long.class),
                      ChronoUnit.MILLIS);
    }
    throw new IllegalArgumentException(
            "expected Date or Number, but actual type was: " + o.getClass());
  }

  // helper class

  private static class TestTriggerContext implements TriggerContext {

    @Nullable
    private final Instant scheduled;

    @Nullable
    private final Instant actual;

    @Nullable
    private final Instant completion;

    TestTriggerContext(@Nullable Instant scheduled,
            @Nullable Instant actual, @Nullable Instant completion) {

      this.scheduled = scheduled;
      this.actual = actual;
      this.completion = completion;
    }

    @Override
    public Instant lastActualExecution() {
      return this.actual;
    }

    @Override
    public Instant lastCompletion() {
      return this.completion;
    }

    @Override
    public Instant lastScheduledExecution() {
      return this.scheduled;
    }
  }

}
