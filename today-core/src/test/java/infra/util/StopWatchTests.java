/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.concurrent.TimeUnit;

import infra.util.StopWatch.TaskInfo;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author TODAY 2021/3/6 9:34
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class StopWatchTests {

  private static final String ID = "myId";

  private static final String name1 = "Task 1";
  private static final String name2 = "Task 2";

  private static final long duration1 = 200;
  private static final long duration2 = 100;
  private static final long fudgeFactor = 50;

  private final StopWatch stopWatch = new StopWatch(ID);

  @Test
  void failureToStartBeforeGettingTimings() {
    assertThatIllegalStateException().isThrownBy(stopWatch::lastTaskInfo);
  }

  @Test
  void failureToStartBeforeStop() {
    assertThatIllegalStateException().isThrownBy(stopWatch::stop);
  }

  @Test
  void rejectsStartTwice() {
    stopWatch.start();
    assertThat(stopWatch.isRunning()).isTrue();
    stopWatch.stop();
    assertThat(stopWatch.isRunning()).isFalse();

    stopWatch.start();
    assertThat(stopWatch.isRunning()).isTrue();
    assertThatIllegalStateException().isThrownBy(stopWatch::start);
  }

  @Test
  void validUsage() throws Exception {
    assertThat(stopWatch.isRunning()).isFalse();

    stopWatch.start(name1);
    Thread.sleep(duration1);
    assertThat(stopWatch.isRunning()).isTrue();
    assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
    stopWatch.stop();
    assertThat(stopWatch.isRunning()).isFalse();
    assertThat(stopWatch.getLastTaskTimeNanos())
            .as("last task time in nanoseconds for task #1")
            .isGreaterThanOrEqualTo(millisToNanos(duration1 - fudgeFactor))
            .isLessThanOrEqualTo(millisToNanos(duration1 + fudgeFactor));
    assertThat(stopWatch.getTotalTimeMillis())
            .as("total time in milliseconds for task #1")
            .isGreaterThanOrEqualTo(duration1 - fudgeFactor)
            .isLessThanOrEqualTo(duration1 + fudgeFactor);
    assertThat(stopWatch.getTotalTimeSeconds())
            .as("total time in seconds for task #1")
            .isGreaterThanOrEqualTo((duration1 - fudgeFactor) / 1000.0)
            .isLessThanOrEqualTo((duration1 + fudgeFactor) / 1000.0);

    stopWatch.start(name2);
    Thread.sleep(duration2);
    assertThat(stopWatch.isRunning()).isTrue();
    assertThat(stopWatch.currentTaskName()).isEqualTo(name2);
    stopWatch.stop();
    assertThat(stopWatch.isRunning()).isFalse();
    assertThat(stopWatch.getLastTaskTimeNanos())
            .as("last task time in nanoseconds for task #2")
            .isGreaterThanOrEqualTo(millisToNanos(duration2))
            .isLessThanOrEqualTo(millisToNanos(duration2 + fudgeFactor));
    assertThat(stopWatch.getTotalTimeMillis())
            .as("total time in milliseconds for tasks #1 and #2")
            .isGreaterThanOrEqualTo(duration1 + duration2 - fudgeFactor)
            .isLessThanOrEqualTo(duration1 + duration2 + fudgeFactor);
    assertThat(stopWatch.getTotalTimeSeconds())
            .as("total time in seconds for task #2")
            .isGreaterThanOrEqualTo((duration1 + duration2 - fudgeFactor) / 1000.0)
            .isLessThanOrEqualTo((duration1 + duration2 + fudgeFactor) / 1000.0);

    assertThat(stopWatch.getTaskCount()).isEqualTo(2);
    assertThat(stopWatch.prettyPrint()).contains(name1, name2);
    assertThat(stopWatch.getTaskInfo()).extracting(TaskInfo::getTaskName).containsExactly(name1, name2);
    assertThat(stopWatch.toString()).contains(ID, name1, name2);
    assertThat(stopWatch.getId()).isEqualTo(ID);
  }

  @Test
  void validUsageDoesNotKeepTaskList() throws Exception {
    stopWatch.setKeepTaskList(false);

    stopWatch.start(name1);
    Thread.sleep(duration1);
    assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
    stopWatch.stop();

    stopWatch.start(name2);
    Thread.sleep(duration2);
    assertThat(stopWatch.currentTaskName()).isEqualTo(name2);
    stopWatch.stop();

    assertThat(stopWatch.getTaskCount()).isEqualTo(2);
    assertThat(stopWatch.prettyPrint()).contains("No task info kept");
    assertThat(stopWatch.toString()).doesNotContain(name1, name2);
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(stopWatch::getTaskInfo)
            .withMessage("Task info is not being kept!");
  }

  @Test
  void taskCountStartsAtZero() {
    StopWatch stopWatch = new StopWatch();
    assertThat(stopWatch.getTaskCount()).isZero();
  }

  @Test
  void stopWatchTracksTotalTimeAcrossMultipleTasks() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task1");
    Thread.sleep(100);
    stopWatch.stop();
    stopWatch.start("task2");
    Thread.sleep(150);
    stopWatch.stop();

    assertThat(stopWatch.getTotalTimeMillis())
            .isGreaterThanOrEqualTo(250)
            .isLessThanOrEqualTo(300);
  }

  @Test
  void prettyPrintWithDifferentTimeUnits() throws Exception {
    StopWatch stopWatch = new StopWatch("test");
    stopWatch.start("task1");
    Thread.sleep(100);
    stopWatch.stop();

    String millisOutput = stopWatch.prettyPrint(TimeUnit.MILLISECONDS);
    assertThat(millisOutput)
            .contains("Milliseconds")
            .contains("task1");

    String nanosOutput = stopWatch.prettyPrint(TimeUnit.NANOSECONDS);
    assertThat(nanosOutput)
            .contains("Nanoseconds")
            .contains("task1");
  }

  @Test
  void getTimeInDifferentUnits() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Thread.sleep(1000);
    stopWatch.stop();

    assertThat(stopWatch.getTotalTimeMillis()).isGreaterThanOrEqualTo(1000);
    assertThat(stopWatch.getTotalTimeSeconds()).isGreaterThanOrEqualTo(1.0);
    assertThat(stopWatch.getTotalTime(TimeUnit.MINUTES)).isGreaterThanOrEqualTo(0.016);
  }

  @Test
  void getLastTaskInfoAfterMultipleTasks() throws Exception {
    StopWatch stopWatch = new StopWatch();

    stopWatch.start("task1");
    Thread.sleep(100);
    stopWatch.stop();

    stopWatch.start("task2");
    Thread.sleep(200);
    stopWatch.stop();

    TaskInfo lastTask = stopWatch.lastTaskInfo();
    assertThat(lastTask.getTaskName()).isEqualTo("task2");
    assertThat(lastTask.getTimeMillis()).isGreaterThanOrEqualTo(200);
  }

  @Test
  void taskTimingsAreAccurate() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task");
    Thread.sleep(500);
    stopWatch.stop();

    TaskInfo task = stopWatch.lastTaskInfo();
    assertThat(task.getTimeNanos()).isGreaterThan(TimeUnit.MILLISECONDS.toNanos(490));
    assertThat(task.getTimeMillis()).isGreaterThan(490);
    assertThat(task.getTimeSeconds()).isGreaterThan(0.490);
    assertThat(task.getTime(TimeUnit.MICROSECONDS)).isGreaterThan(490_000);
  }

  @Test
  void shortSummaryFormatsCorrectly() throws Exception {
    StopWatch stopWatch = new StopWatch("test");
    stopWatch.start();
    Thread.sleep(100);
    stopWatch.stop();

    assertThat(stopWatch.shortSummary())
            .startsWith("StopWatch 'test'")
            .endsWith("seconds");
  }

  @Test
  void concurrentTasksAreNotAllowed() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task1");
    assertThatIllegalStateException().isThrownBy(() -> stopWatch.start("task2"));
  }

  @Test
  void emptyTaskNameIsValid() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("");
    assertThat(stopWatch.currentTaskName()).isEmpty();
  }

  @Test
  void stopWatchWithNegativeTimeValuesHandlesOverflow() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task1");
    Thread.sleep(1);
    System.nanoTime(); // Force potential overflow
    stopWatch.stop();
    assertThat(stopWatch.getTotalTimeNanos()).isPositive();
  }

  @Test
  void getTaskInfoReturnsDefensiveCopy() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task1");
    stopWatch.stop();

    TaskInfo[] tasks = stopWatch.getTaskInfo();
    tasks[0] = null;

    assertThat(stopWatch.getTaskInfo()).hasSize(1)
            .extracting(TaskInfo::getTaskName)
            .containsExactly("task1");
  }

  @Test
  void prettyPrintHandlesLongTaskNames() throws Exception {
    StopWatch stopWatch = new StopWatch();
    String longName = "a".repeat(100);
    stopWatch.start(longName);
    Thread.sleep(10);
    stopWatch.stop();

    String output = stopWatch.prettyPrint();
    assertThat(output).contains(longName);
  }

  @Test
  void toStringAfterClearingTaskList() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task");
    stopWatch.stop();
    stopWatch.setKeepTaskList(false);

    assertThat(stopWatch.toString())
            .contains("no task info kept");
  }

  @Test
  void taskTimingWithMinimalDuration() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("quick");
    stopWatch.stop();

    assertThat(stopWatch.getTotalTimeNanos()).isNotNegative();
  }

  @Test
  void multipleTasksWithSameName() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task");
    Thread.sleep(10);
    stopWatch.stop();
    stopWatch.start("task");
    Thread.sleep(10);
    stopWatch.stop();

    assertThat(stopWatch.getTaskInfo())
            .hasSize(2)
            .extracting(TaskInfo::getTaskName)
            .containsExactly("task", "task");
  }

  @Test
  void prettyPrintWithCustomTimeUnitFormatting() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task");
    Thread.sleep(1500);
    stopWatch.stop();

    String minutesOutput = stopWatch.prettyPrint(TimeUnit.MINUTES);
    assertThat(minutesOutput)
            .contains("Minutes");
  }

  @Test
  void invalidTimeUnitForPrettyPrint() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    stopWatch.stop();

    assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> stopWatch.prettyPrint(null));
  }

  @Test
  void longRunningTasksDoNotOverflow() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("long task");
    Thread.sleep(2000);
    stopWatch.stop();

    assertThat(stopWatch.getTotalTimeSeconds()).isPositive()
            .isGreaterThanOrEqualTo(2.0);
  }

  @Test
  void stopWithoutStartThrowsException() {
    StopWatch stopWatch = new StopWatch();
    assertThatIllegalStateException().isThrownBy(stopWatch::stop)
            .withMessage("Can't stop StopWatch: it's not running");
  }

  @Test
  void startWithEmptyTaskName() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("");
    assertThat(stopWatch.currentTaskName()).isEmpty();
    stopWatch.stop();
  }

  @Test
  void stopWatchWithNoTasks() {
    StopWatch stopWatch = new StopWatch();
    assertThat(stopWatch.getTaskCount()).isZero();
    assertThat(stopWatch.getTotalTimeNanos()).isZero();
    assertThat(stopWatch.getTotalTimeMillis()).isZero();
    assertThat(stopWatch.getTotalTimeSeconds()).isZero();
    assertThat(stopWatch.prettyPrint()).contains("No task info kept");
  }

  @Test
  void startStopMultipleTimesWithoutTasks() {
    StopWatch stopWatch = new StopWatch();
    for (int i = 0; i < 10; i++) {
      stopWatch.start("task" + i);
      stopWatch.stop();
    }
    assertThat(stopWatch.getTaskCount()).isEqualTo(10);
  }

  @Test
  void stopWatchWithHighPrecisionTiming() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("highPrecisionTask");
    Thread.sleep(1);
    stopWatch.stop();
    assertThat(stopWatch.getLastTaskTimeNanos()).isGreaterThan(0);
  }

  @Test
  void stopWatchWithNegativeTimeValues() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("negativeTimeTask");
    stopWatch.stop();
    assertThat(stopWatch.getTotalTimeNanos()).isNotNegative();
  }

  @Test
  void stopWatchWithLongRunningTask() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("longRunningTask");
    Thread.sleep(5000);
    stopWatch.stop();
    assertThat(stopWatch.getTotalTimeSeconds()).isGreaterThanOrEqualTo(5.0);
  }

  @Test
  void stopWatchWithMultipleTasksAndNoTaskList() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.setKeepTaskList(false);
    stopWatch.start("task1");
    Thread.sleep(100);
    stopWatch.stop();
    stopWatch.start("task2");
    Thread.sleep(200);
    stopWatch.stop();
    assertThat(stopWatch.getTaskCount()).isEqualTo(2);
    assertThat(stopWatch.prettyPrint()).contains("No task info kept");
  }

  @Test
  void stopWatchWithConcurrentModification() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("task1");
    Thread.sleep(100);
    stopWatch.stop();
    stopWatch.start("task2");
    Thread.sleep(200);
    stopWatch.stop();
    TaskInfo[] tasks = stopWatch.getTaskInfo();
    tasks[0] = null;
    assertThat(stopWatch.getTaskInfo()).hasSize(2);
  }

  private static long millisToNanos(long duration) {
    return MILLISECONDS.toNanos(duration);
  }

}
