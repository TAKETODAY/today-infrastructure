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

package infra.reflect;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class MethodInvocationBenchmark {

  private Method method;

  private MethodHandle methodHandle;

  private MyClass instance;

  private MethodInvoker invoker;

  @Setup
  public void setup() throws Exception {
    method = MyClass.class.getMethod("myMethod", int.class);
    method.setAccessible(true);

    MethodHandles.Lookup lookup = MethodHandles.lookup();
    methodHandle = lookup.unreflect(method);
    invoker = MethodInvoker.forMethod(method);
    instance = new MyClass();
  }

  @Benchmark
  public void testReflection(Blackhole bh) throws Exception {
    bh.consume(method.invoke(instance, 42));
  }

  @Benchmark
  public void testMethodHandle(Blackhole bh) throws Throwable {
    bh.consume(methodHandle.invoke(instance, 42));
  }

  @Benchmark
  public void testMethodHandleExact(Blackhole bh) throws Throwable {
    bh.consume((int) methodHandle.invokeExact(instance, 42));
  }

  @Benchmark
  public void testDirectCall(Blackhole bh) {
    bh.consume(instance.myMethod(42));
  }

  @Benchmark
  public void testMethodInvoker(Blackhole bh) {
    bh.consume(invoker.invoke(instance, new Object[] { 42 }));
  }

  public static class MyClass {

    public int myMethod(int value) {
      return value * 2;
    }

  }
}