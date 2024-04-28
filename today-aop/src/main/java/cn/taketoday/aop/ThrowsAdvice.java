/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.aop;

/**
 * Tag interface for throws advice.
 *
 * <p>There are not any methods on this interface, as methods are invoked by
 * reflection. Implementing classes must implement methods of the form:
 *
 * <pre class="code">void afterThrowing([Method, args, target], ThrowableSubclass);</pre>
 *
 * <p>Some examples of valid methods would be:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 *
 * The first three arguments are optional, and only useful if we want further
 * information about the joinpoint, as in AspectJ <b>after-throwing</b> advice.
 *
 * <p><b>Note:</b> If a throws-advice method throws an exception itself, it will
 * override the original exception (i.e. change the exception thrown to the user).
 * The overriding exception will typically be a RuntimeException; this is compatible
 * with any method signature. However, if a throws-advice method throws a checked
 * exception, it will have to match the declared exceptions of the target method
 * and is hence to some degree coupled to specific target method signatures.
 * <b>Do not throw an undeclared checked exception that is incompatible with
 * the target method's signature!</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/20 22:20
 * @see AfterReturningAdvice
 * @see MethodBeforeAdvice
 * @since 3.0
 */
public interface ThrowsAdvice extends AfterAdvice {

}

