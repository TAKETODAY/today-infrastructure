/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.util.concurrent;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Verifies the memory-model invariant of {@link AbstractFuture#tryFailure(Throwable)}:
 * <p>
 * Once {@link Future#isFailed()} returns {@code true}, {@link Future#getCause()}
 * must never be {@code null} — otherwise there is a publication window where the
 * failure cause stored in the non-volatile {@code result} field is not yet visible
 * to a reader that has already observed the terminal {@code EXCEPTIONAL} state.
 *
 * <p>This relies on {@code tryFailure} transitioning through {@code COMPLETING} and
 * publishing {@code result} via the {@code EXCEPTIONAL} terminal-state release write.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@JCStressTest
@Description("失败完成后 getCause() 必须非 null（无可见性窗口）")
// 两次读都观察到尚未失败 —— 合法
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "两次读都观察到未失败")
// 两次读之间失败完成：isFailed() 读到旧值、getCause() 读到新值 —— 合法
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "两次读之间失败完成")
// 已失败且 getCause 非 null —— 期望状态
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "已失败且 getCause 非 null")
// 已失败但 getCause 为 null —— 唯一的可见性窗口 bug
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "已失败但 getCause 为 null —— 可见性窗口 bug！")
@State
public class FutureFailureVisibilityTests {

  final Promise<Object> future = Future.forPromise();

  @Actor
  public void fail() {
    future.tryFailure(new IllegalStateException("boom"));
  }

  @Actor
  public void read(II_Result r) {
    r.r1 = future.isFailed() ? 1 : 0;
    r.r2 = future.getCause() != null ? 1 : 0;
  }

}