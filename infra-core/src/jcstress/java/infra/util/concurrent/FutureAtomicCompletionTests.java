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
import org.openjdk.jcstress.infra.results.III_Result;

/**
 * Verifies the <em>atomicity</em> of completion: when several completion
 * operations ({@link AbstractFuture#trySuccess(Object)},
 * {@link AbstractFuture#tryFailure(Throwable)} and
 * {@link AbstractFuture#cancel(Throwable, boolean)}) race on the same
 * {@code NEW} future, <em>exactly one</em> must win. The single {@code NEW ->
 * COMPLETING} CAS guarantees that a future completes at most once, so at most
 * one actor may observe a {@code true} return, and — since each actor must run
 * against the shared initial {@code NEW} state — at least one must win.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@JCStressTest
@Description("并发完成操作恰好一个成功（唯一胜者）")
@Outcome(id = "1, 0, 0", expect = Expect.ACCEPTABLE, desc = "仅 trySuccess 获胜")
@Outcome(id = "0, 1, 0", expect = Expect.ACCEPTABLE, desc = "仅 tryFailure 获胜")
@Outcome(id = "0, 0, 1", expect = Expect.ACCEPTABLE, desc = "仅 cancel 获胜")
// 其余任何取值（多于一个或零个获胜）都违背“唯一胜者”不变量
@Outcome(expect = Expect.FORBIDDEN, desc = "多于一个或零个操作获胜 —— 原子性 bug！")
@State
public class FutureAtomicCompletionTests {

  final Promise<Object> future = Future.forPromise();

  @Actor
  public void succeed(III_Result r) {
    if (future.trySuccess(new Object())) {
      r.r1 = 1;
    }
    else {
      r.r1 = 0;
    }
  }

  @Actor
  public void fail(III_Result r) {
    if (future.tryFailure(new IllegalStateException("boom"))) {
      r.r2 = 1;
    }
    else {
      r.r2 = 0;
    }
  }

  @Actor
  public void cancel(III_Result r) {
    if (future.cancel("test reason")) {
      r.r3 = 1;
    }
    else {
      r.r3 = 0;
    }
  }

}