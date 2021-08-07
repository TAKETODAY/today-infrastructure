/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.support;

/**
 * An instance of this class is stored in an AnnotationInvocationHandler's
 * "memberValues" map in lieu of a value for an annotation member that
 * cannot be returned due to some exceptional condition (typically some
 * form of illegal evolution of the annotation class).  The ExceptionProxy
 * instance describes the exception that the dynamic proxy should throw if
 * it is queried for this member.
 *
 * @author Josh Bloch
 * @since 1.5
 */
public abstract class ExceptionProxy implements java.io.Serializable {
  protected abstract RuntimeException generateException();
}
