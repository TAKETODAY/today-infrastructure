/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.agent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;

/**
 * Record of an invocation of a method relevant to {@link cn.taketoday.aot.hint.RuntimeHints}.
 * <p>The {@link RuntimeHintsAgent} instruments bytecode and intercepts invocations of
 * {@link InstrumentedMethod specific methods}; invocations are recorded during test execution
 * to match them against an existing {@link cn.taketoday.aot.hint.RuntimeHints} configuration.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public final class RecordedInvocation {

  @Nullable
  private final Object instance;

  private final InstrumentedMethod instrumentedMethod;

  private final Object[] arguments;

  @Nullable
  private final Object returnValue;

  private final List<StackWalker.StackFrame> stackFrames;

  private RecordedInvocation(InstrumentedMethod instrumentedMethod, @Nullable Object instance,
          Object[] arguments, @Nullable Object returnValue, List<StackWalker.StackFrame> stackFrames) {
    this.instance = instance;
    this.instrumentedMethod = instrumentedMethod;
    this.arguments = arguments;
    this.returnValue = returnValue;
    this.stackFrames = stackFrames;
  }

  /**
   * Initialize a builder for the given {@link InstrumentedMethod}.
   *
   * @param instrumentedMethod the instrumented method
   * @return a builder
   */
  public static Builder of(InstrumentedMethod instrumentedMethod) {
    Assert.notNull(instrumentedMethod, "InstrumentedMethod must not be null");
    return new Builder(instrumentedMethod);
  }

  /**
   * Return the category of {@link RuntimeHints} this invocation relates to.
   *
   * @return the hint type
   */
  public HintType getHintType() {
    return this.instrumentedMethod.getHintType();
  }

  /**
   * Return a simple representation of the method invoked here.
   *
   * @return the method reference
   */
  public MethodReference getMethodReference() {
    return this.instrumentedMethod.methodReference();
  }

  /**
   * Return the stack trace of the current invocation.
   *
   * @return the stack frames
   */
  public Stream<StackWalker.StackFrame> getStackFrames() {
    return this.stackFrames.stream();
  }

  /**
   * Return the instance of the object being invoked.
   *
   * @return the object instance
   * @throws IllegalStateException in case of static invocations (there is no {@code this})
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance() {
    Assert.state(this.instance != null, "Cannot resolve 'this' for static invocations");
    return (T) this.instance;
  }

  /**
   * Return the Type reference of the object being invoked.
   *
   * @return the instance type reference, or {@code null}
   * @throws IllegalStateException in case of static invocations (there is no {@code this})
   */
  public TypeReference getInstanceTypeReference() {
    Assert.state(this.instance != null, "Cannot resolve 'this' for static invocations");
    return TypeReference.of(this.instance.getClass());
  }

  /**
   * Return whether the current invocation is static.
   *
   * @return {@code true} if the invocation is static
   */
  public boolean isStatic() {
    return this.instance == null;
  }

  /**
   * Return the argument values used for the current reflection invocation.
   *
   * @return the invocation arguments
   */
  public List<Object> getArguments() {
    return Arrays.asList(this.arguments);
  }

  /**
   * Return the argument value at the given index used for the current reflection invocation.
   *
   * @param index the parameter index
   * @return the argument at the given index
   */
  @SuppressWarnings("unchecked")
  public <T> T getArgument(int index) {
    return (T) this.arguments[index];
  }

  /**
   * Return the types of the arguments used for the current reflection invocation.
   *
   * @return the argument types
   */
  public List<TypeReference> getArgumentTypes() {
    return getArgumentTypes(0);
  }

  /**
   * Return the types of the arguments used for the current reflection invocation,
   * starting from the given index.
   *
   * @return the argument types, starting at the given index
   */
  public List<TypeReference> getArgumentTypes(int index) {
    return Arrays.stream(this.arguments).skip(index).map(param -> TypeReference.of(param.getClass())).toList();
  }

  /**
   * Return the value actually returned by the invoked method.
   *
   * @return the value returned by the invocation
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T getReturnValue() {
    return (T) this.returnValue;
  }

  /**
   * Whether the given hints cover the current invocation.
   * <p>If the given hint doesn't match this invocation might fail at execution time depending on the target runtime.
   *
   * @return whether the given hints match
   */
  public boolean matches(RuntimeHints hints) {
    return this.instrumentedMethod.matcher(this).test(hints);
  }

  @Override
  public String toString() {
    if (isStatic()) {
      return "<%s> invocation of <%s> with arguments %s".formatted(
              getHintType().hintClassName(), getMethodReference(), getArguments());
    }
    else {
      Class<?> instanceType = (getInstance() instanceof Class<?> clazz) ? clazz : getInstance().getClass();
      return "<%s> invocation of <%s> on type <%s> with arguments %s".formatted(
              getHintType().hintClassName(), getMethodReference(), instanceType.getCanonicalName(), getArguments());
    }
  }

  /**
   * Builder for {@link RecordedInvocation}.
   */
  public static class Builder {

    @Nullable
    private Object instance;

    private final InstrumentedMethod instrumentedMethod;

    private Object[] arguments = new Object[0];

    @Nullable
    private Object returnValue;

    Builder(InstrumentedMethod instrumentedMethod) {
      this.instrumentedMethod = instrumentedMethod;
    }

    /**
     * Set the {@code this} object instance used for this invocation.
     *
     * @param instance the current object instance, {@code null} in case of static invocations
     * @return {@code this}, to facilitate method chaining
     */
    public Builder onInstance(Object instance) {
      this.instance = instance;
      return this;
    }

    /**
     * Use the given object as the unique argument.
     *
     * @param argument the invocation argument
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withArgument(@Nullable Object argument) {
      if (argument != null) {
        this.arguments = new Object[] { argument };
      }
      return this;
    }

    /**
     * Use the given objects as the invocation arguments.
     *
     * @param arguments the invocation arguments
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withArguments(@Nullable Object... arguments) {
      if (arguments != null) {
        this.arguments = arguments;
      }
      return this;
    }

    /**
     * Use the given object as the return value for the invocation.
     *
     * @param returnValue the return value
     * @return {@code this}, to facilitate method chaining
     */
    public Builder returnValue(@Nullable Object returnValue) {
      this.returnValue = returnValue;
      return this;
    }

    /**
     * Create a {@link RecordedInvocation} based on the state of this builder.
     *
     * @return a recorded invocation
     */
    public RecordedInvocation build() {
      List<StackWalker.StackFrame> stackFrames = StackWalker.getInstance().walk(stream -> stream
              .dropWhile(stackFrame -> stackFrame.getClassName().startsWith(getClass().getPackageName()))
              .toList());
      return new RecordedInvocation(this.instrumentedMethod, this.instance, this.arguments, this.returnValue, stackFrames);
    }

  }

}
