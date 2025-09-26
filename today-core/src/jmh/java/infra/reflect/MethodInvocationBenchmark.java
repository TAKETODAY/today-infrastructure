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