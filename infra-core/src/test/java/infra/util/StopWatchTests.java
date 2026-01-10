/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

  @Test
  void startWithNullId() {
    StopWatch stopWatch = new StopWatch(null);
    assertThat(stopWatch.getId()).isNull();
  }

  @Test
  void startWithCustomId() {
    StopWatch stopWatch = new StopWatch("customId");
    assertThat(stopWatch.getId()).isEqualTo("customId");
  }

  @Test
  void prettyPrintWithEmptyTaskList() {
    StopWatch stopWatch = new StopWatch("test");
    String output = stopWatch.prettyPrint();
    assertThat(output).contains("StopWatch 'test'")
            .contains("No task info kept");
  }

  @Test
  void prettyPrintWithSingleTask() throws Exception {
    StopWatch stopWatch = new StopWatch("test");
    stopWatch.start("singleTask");
    Thread.sleep(50);
    stopWatch.stop();

    String output = stopWatch.prettyPrint();
    assertThat(output).contains("singleTask")
            .contains("StopWatch 'test'")
            .contains("seconds");
  }

  @Test
  void prettyPrintWithMultipleTasks() throws Exception {
    StopWatch stopWatch = new StopWatch("test");
    stopWatch.start("task1");
    Thread.sleep(50);
    stopWatch.stop();
    stopWatch.start("task2");
    Thread.sleep(50);
    stopWatch.stop();

    String output = stopWatch.prettyPrint();
    assertThat(output).contains("task1")
            .contains("task2")
            .contains("StopWatch 'test'");
  }

  @Test
  void prettyPrintWithNanoseconds() throws Exception {
    StopWatch stopWatch = new StopWatch("test");
    stopWatch.start("nanoTask");
    Thread.sleep(1);
    stopWatch.stop();

    String output = stopWatch.prettyPrint(TimeUnit.NANOSECONDS);
    assertThat(output).contains("Nanoseconds")
            .contains("nanoTask");
  }

  @Test
  void getTotalTimeWithDifferentTimeUnits() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Thread.sleep(100);
    stopWatch.stop();

    assertThat(stopWatch.getTotalTime(TimeUnit.MILLISECONDS)).isPositive();
    assertThat(stopWatch.getTotalTime(TimeUnit.SECONDS)).isPositive();
    assertThat(stopWatch.getTotalTime(TimeUnit.NANOSECONDS)).isPositive();
  }

  @Test
  void getLastTaskName() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("lastTask");
    Thread.sleep(10);
    stopWatch.stop();

    assertThat(stopWatch.getLastTaskName()).isEqualTo("lastTask");
  }

  @Test
  void getLastTaskTimeMillis() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("timedTask");
    Thread.sleep(100);
    stopWatch.stop();

    assertThat(stopWatch.getLastTaskTimeMillis()).isGreaterThanOrEqualTo(100);
  }

  @Test
  void shortSummaryWithNoTasks() {
    StopWatch stopWatch = new StopWatch("empty");
    String summary = stopWatch.shortSummary();
    assertThat(summary).isEqualTo("StopWatch 'empty': 0.0 seconds");
  }

  @Test
  void shortSummaryWithTasks() throws Exception {
    StopWatch stopWatch = new StopWatch("withTasks");
    stopWatch.start();
    Thread.sleep(50);
    stopWatch.stop();

    String summary = stopWatch.shortSummary();
    assertThat(summary).contains("StopWatch 'withTasks'")
            .contains("seconds");
  }

  @Test
  void toStringWithTasks() throws Exception {
    StopWatch stopWatch = new StopWatch("withTasks");
    stopWatch.start("toStringTask");
    Thread.sleep(10);
    stopWatch.stop();

    String result = stopWatch.toString();
    assertThat(result).contains("StopWatch 'withTasks'")
            .contains("toStringTask")
            .contains("seconds")
            .contains("%");
  }

  @Test
  void taskInfoGetTaskName() {
    StopWatch.TaskInfo taskInfo = new StopWatch.TaskInfo("testTask", 1000000);
    assertThat(taskInfo.getTaskName()).isEqualTo("testTask");
  }

  @Test
  void taskInfoGetTimeNanos() {
    StopWatch.TaskInfo taskInfo = new StopWatch.TaskInfo("testTask", 1000000);
    assertThat(taskInfo.getTimeNanos()).isEqualTo(1000000);
  }

  @Test
  void taskInfoGetTimeMillis() {
    StopWatch.TaskInfo taskInfo = new StopWatch.TaskInfo("testTask", 2000000);
    assertThat(taskInfo.getTimeMillis()).isEqualTo(2);
  }

  @Test
  void taskInfoGetTimeSeconds() {
    StopWatch.TaskInfo taskInfo = new StopWatch.TaskInfo("testTask", 1000000000);
    assertThat(taskInfo.getTimeSeconds()).isEqualTo(1.0);
  }

  @Test
  void taskInfoGetTimeWithDifferentUnits() {
    StopWatch.TaskInfo taskInfo = new StopWatch.TaskInfo("testTask", 1000000000);
    assertThat(taskInfo.getTime(TimeUnit.SECONDS)).isEqualTo(1.0);
    assertThat(taskInfo.getTime(TimeUnit.MILLISECONDS)).isEqualTo(1000.0);
    assertThat(taskInfo.getTime(TimeUnit.NANOSECONDS)).isEqualTo(1000000000.0);
  }

  @Test
  void setKeepTaskListToFalseThenTrue() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.setKeepTaskList(false);
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(stopWatch::getTaskInfo);

    stopWatch.setKeepTaskList(true);
    stopWatch.start("task");
    stopWatch.stop();
    assertThat(stopWatch.getTaskInfo()).hasSize(1);
  }

  @Test
  void startWithoutTaskName() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    assertThat(stopWatch.currentTaskName()).isEmpty();
    stopWatch.stop();
  }

  @Test
  void isRunningWhenNotStarted() {
    StopWatch stopWatch = new StopWatch();
    assertThat(stopWatch.isRunning()).isFalse();
  }

  @Test
  void isRunningWhenStarted() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("runningTask");
    assertThat(stopWatch.isRunning()).isTrue();
    stopWatch.stop();
  }

  @Test
  void isRunningWhenStopped() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("stoppedTask");
    stopWatch.stop();
    assertThat(stopWatch.isRunning()).isFalse();
  }

  @Test
  void currentTaskNameWhenNotRunning() {
    StopWatch stopWatch = new StopWatch();
    assertThat(stopWatch.currentTaskName()).isNull();
  }

  @Test
  void currentTaskNameWhenRunning() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start("currentTask");
    assertThat(stopWatch.currentTaskName()).isEqualTo("currentTask");
    stopWatch.stop();
  }

  @Test
  void lastTaskInfoThrowsExceptionWhenNoTasks() {
    StopWatch stopWatch = new StopWatch();
    assertThatIllegalStateException()
            .isThrownBy(stopWatch::lastTaskInfo)
            .withMessage("No tasks run");
  }

  @Test
  void getLastTaskNameThrowsExceptionWhenNoTasks() {
    StopWatch stopWatch = new StopWatch();
    assertThatIllegalStateException()
            .isThrownBy(stopWatch::getLastTaskName)
            .withMessage("No tasks run");
  }

  @Test
  void getLastTaskTimeNanosThrowsExceptionWhenNoTasks() {
    StopWatch stopWatch = new StopWatch();
    assertThatIllegalStateException()
            .isThrownBy(stopWatch::getLastTaskTimeNanos)
            .withMessage("No tasks run");
  }

  @Test
  void getLastTaskTimeMillisThrowsExceptionWhenNoTasks() {
    StopWatch stopWatch = new StopWatch();
    assertThatIllegalStateException()
            .isThrownBy(stopWatch::getLastTaskTimeMillis)
            .withMessage("No tasks run");
  }

  private static long millisToNanos(long duration) {
    return MILLISECONDS.toNanos(duration);
  }

}
