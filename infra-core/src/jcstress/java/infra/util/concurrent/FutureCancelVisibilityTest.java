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
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Verifies the memory-model invariant of {@link Future#cancel(Throwable, boolean)}:
 * <p>
 * Once {@link Future#isCancelled()} returns {@code true}, {@link Future#getCause()}
 * must never be {@code null} — otherwise there is a publication window where the
 * cancellation cause stored in the non-volatile {@code result} field is not yet
 * visible to a reader that has already observed the terminal cancelled state.
 *
 * <p>This relies on {@code cancel} transitioning through {@code COMPLETING} and
 * publishing {@code result} via the terminal-state release write.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@JCStressTest
@Description("取消后 getCause() 必须非 null（无可见性窗口）")
// 两次读都观察到尚未取消 —— 合法
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "两次读都观察到未取消")
// 两次读之间取消完成：isCancelled() 读到旧值、getCause() 读到新值 —— 合法
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "两次读之间取消完成")
// 已取消且 getCause 非 null —— 期望状态
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "已取消且 getCause 非 null")
// 已取消但 getCause 为 null —— 唯一的可见性窗口 bug
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "已取消但 getCause 为 null —— 可见性窗口 bug！")
@State
public class FutureCancelVisibilityTest {

  final Promise<Object> future = Future.forPromise();

  @Actor
  public void cancel() {
    future.cancel("test reason");
  }

  @Actor
  public void read(II_Result r) {
    r.r1 = future.isCancelled() ? 1 : 0;
    r.r2 = future.getCause() != null ? 1 : 0;
  }

  @Arbiter
  public void arbiter(II_Result r) {
    // 不变量由 @Outcome 声明：若 r.r1 == 1（已取消），则 r.r2 必须为 1。
    // 出现 (1, 0) 即 FORBIDDEN，说明存在可见性窗口。
  }
}
