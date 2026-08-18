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
 * Verifies that the completion of a {@link Future} releases the writes to the
 * value <em>inside</em> the result, not just the reference to it — a full
 * happens-before (the {@code NORMAL} terminal-state release write synchronizes
 * with the reader's acquire read of {@code state}).
 * <p>
 * The writer fills {@code holder.value} (a plain, non-volatile field) and then
 * completes the future. A reader that observes the future as successfully
 * completed and retrieves {@code holder} must therefore see {@code holder.value}
 * already set. If only the reference were published without the inner write, a
 * reader could observe the completed {@code NORMAL} state and the {@code holder}
 * reference while still reading the stale default value — a missing causal/global
 * ordering bug.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@JCStressTest
@Description("成功完成后 result 内部字段必须可见（因果一致性）")
// 读者未观察到成功，或 getNow() 尚未返回该 holder —— 合法
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "未观察到 holder 或尚未完成")
// 观察到 holder 且其内部字段 value 已写入 —— 期望状态
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "观察到 holder 且 value 可见")
// 观察到 holder 但内部字段 value 仍是旧值 —— 因果一致性 bug
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "观察到 holder 但 value 未可见 —— 因果性 bug！")
@State
public class FutureCausalVisibilityTests {

  private static final int VALUE = 0xCAFE_BABE;

  final Promise<Holder> future = Future.forPromise();
  final Holder holder = new Holder();

  @Actor
  public void write() {
    holder.value = VALUE;
    future.trySuccess(holder);
  }

  @Actor
  public void read(II_Result r) {
    Holder got = future.getNow();
    boolean observed = got == holder;
    r.r1 = observed ? 1 : 0;
    r.r2 = observed && got.value == VALUE ? 1 : 0;
  }

  /** A holder whose {@code value} is a plain, non-volatile field. */
  static final class Holder {

    int value;
  }

}