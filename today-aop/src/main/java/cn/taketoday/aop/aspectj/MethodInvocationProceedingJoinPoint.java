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

package cn.taketoday.aop.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * An implementation of the AspectJ {@link ProceedingJoinPoint} interface
 * wrapping an AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}.
 *
 * <p><b>Note</b>: The {@code getThis()} method returns the current Framework AOP proxy.
 * The {@code getTarget()} method returns the current Framework AOP target (which may be
 * {@code null} if there is no target instance) as a plain POJO without any advice.
 * <b>If you want to call the object and have the advice take effect, use {@code getThis()}.</b>
 * A common example is casting the object to an introduced interface in the implementation of
 * an introduction. There is no such distinction between target and proxy in AspectJ itself.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 4.0
 */
public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {

  private static final ParameterNameDiscoverer parameterNameDiscoverer = ParameterNameDiscoverer.getSharedInstance();

  private final ProxyMethodInvocation methodInvocation;

  @Nullable
  private Object[] args;

  /** Lazily initialized signature object. */
  @Nullable
  private Signature signature;

  /** Lazily initialized source location object. */
  @Nullable
  private SourceLocation sourceLocation;

  /**
   * Create a new MethodInvocationProceedingJoinPoint, wrapping the given
   * Framework ProxyMethodInvocation object.
   *
   * @param methodInvocation the Framework ProxyMethodInvocation object
   */
  public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
    Assert.notNull(methodInvocation, "MethodInvocation must not be null");
    this.methodInvocation = methodInvocation;
  }

  @Override
  public void set$AroundClosure(AroundClosure aroundClosure) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public Object proceed() throws Throwable {
    return this.methodInvocation.invocableClone().proceed();
  }

  @Override
  @Nullable
  public Object proceed(Object[] arguments) throws Throwable {
    Assert.notNull(arguments, "Argument array passed to proceed cannot be null");
    if (arguments.length != this.methodInvocation.getArguments().length) {
      throw new IllegalArgumentException("Expecting " +
              this.methodInvocation.getArguments().length + " arguments to proceed, " +
              "but was passed " + arguments.length + " arguments");
    }
    this.methodInvocation.setArguments(arguments);
    return this.methodInvocation.invocableClone(arguments).proceed();
  }

  /**
   * Returns the Framework AOP proxy. Cannot be {@code null}.
   */
  @Override
  public Object getThis() {
    return this.methodInvocation.getProxy();
  }

  /**
   * Returns the Framework AOP target. May be {@code null} if there is no target.
   */
  @Override
  @Nullable
  public Object getTarget() {
    return this.methodInvocation.getThis();
  }

  @Override
  public Object[] getArgs() {
    if (this.args == null) {
      this.args = this.methodInvocation.getArguments().clone();
    }
    return this.args;
  }

  @Override
  public Signature getSignature() {
    if (this.signature == null) {
      this.signature = new MethodSignatureImpl();
    }
    return this.signature;
  }

  @Override
  public SourceLocation getSourceLocation() {
    if (this.sourceLocation == null) {
      this.sourceLocation = new SourceLocationImpl();
    }
    return this.sourceLocation;
  }

  @Override
  public String getKind() {
    return ProceedingJoinPoint.METHOD_EXECUTION;
  }

  @Override
  public int getId() {
    // TODO: It's just an adapter but returning 0 might still have side effects...
    return 0;
  }

  @Override
  public StaticPart getStaticPart() {
    return this;
  }

  @Override
  public String toShortString() {
    return "execution(" + getSignature().toShortString() + ")";
  }

  @Override
  public String toLongString() {
    return "execution(" + getSignature().toLongString() + ")";
  }

  @Override
  public String toString() {
    return "execution(" + getSignature().toString() + ")";
  }

  /**
   * Lazily initialized MethodSignature.
   */
  private class MethodSignatureImpl implements MethodSignature {

    @Nullable
    private volatile String[] parameterNames;

    @Override
    public String getName() {
      return methodInvocation.getMethod().getName();
    }

    @Override
    public int getModifiers() {
      return methodInvocation.getMethod().getModifiers();
    }

    @Override
    public Class<?> getDeclaringType() {
      return methodInvocation.getMethod().getDeclaringClass();
    }

    @Override
    public String getDeclaringTypeName() {
      return methodInvocation.getMethod().getDeclaringClass().getName();
    }

    @Override
    public Class<?> getReturnType() {
      return methodInvocation.getMethod().getReturnType();
    }

    @Override
    public Method getMethod() {
      return methodInvocation.getMethod();
    }

    @Override
    public Class<?>[] getParameterTypes() {
      return methodInvocation.getMethod().getParameterTypes();
    }

    @Override
    @Nullable
    public String[] getParameterNames() {
      String[] parameterNames = this.parameterNames;
      if (parameterNames == null) {
        parameterNames = parameterNameDiscoverer.getParameterNames(getMethod());
        this.parameterNames = parameterNames;
      }
      return parameterNames;
    }

    @Override
    public Class<?>[] getExceptionTypes() {
      return methodInvocation.getMethod().getExceptionTypes();
    }

    @Override
    public String toShortString() {
      return toString(false, false, false, false);
    }

    @Override
    public String toLongString() {
      return toString(true, true, true, true);
    }

    @Override
    public String toString() {
      return toString(false, true, false, true);
    }

    private String toString(boolean includeModifier, boolean includeReturnTypeAndArgs,
            boolean useLongReturnAndArgumentTypeName, boolean useLongTypeName) {

      StringBuilder sb = new StringBuilder();
      if (includeModifier) {
        sb.append(Modifier.toString(getModifiers()));
        sb.append(' ');
      }
      if (includeReturnTypeAndArgs) {
        appendType(sb, getReturnType(), useLongReturnAndArgumentTypeName);
        sb.append(' ');
      }
      appendType(sb, getDeclaringType(), useLongTypeName);
      sb.append('.');
      sb.append(getMethod().getName());
      sb.append('(');
      Class<?>[] parametersTypes = getParameterTypes();
      appendTypes(sb, parametersTypes, includeReturnTypeAndArgs, useLongReturnAndArgumentTypeName);
      sb.append(')');
      return sb.toString();
    }

    private void appendTypes(StringBuilder sb, Class<?>[] types, boolean includeArgs,
            boolean useLongReturnAndArgumentTypeName) {

      if (includeArgs) {
        for (int size = types.length, i = 0; i < size; i++) {
          appendType(sb, types[i], useLongReturnAndArgumentTypeName);
          if (i < size - 1) {
            sb.append(',');
          }
        }
      }
      else {
        if (types.length != 0) {
          sb.append("..");
        }
      }
    }

    private void appendType(StringBuilder sb, Class<?> type, boolean useLongTypeName) {
      if (type.isArray()) {
        appendType(sb, type.getComponentType(), useLongTypeName);
        sb.append("[]");
      }
      else {
        sb.append(useLongTypeName ? type.getName() : type.getSimpleName());
      }
    }
  }

  /**
   * Lazily initialized SourceLocation.
   */
  private class SourceLocationImpl implements SourceLocation {

    @Override
    public Class<?> getWithinType() {
      if (methodInvocation.getThis() == null) {
        throw new UnsupportedOperationException("No source location joinpoint available: target is null");
      }
      return methodInvocation.getThis().getClass();
    }

    @Override
    public String getFileName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getLine() {
      throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public int getColumn() {
      throw new UnsupportedOperationException();
    }
  }

}
