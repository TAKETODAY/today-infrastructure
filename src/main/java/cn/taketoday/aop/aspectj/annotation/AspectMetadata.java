/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcut;
import cn.taketoday.aop.aspectj.TypePatternClassFilter;
import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.aop.support.ComposablePointcut;

/**
 * Metadata for an AspectJ aspect class, with an additional Framework AOP pointcut
 * for the per clause.
 *
 * <p>Uses AspectJ 5 AJType reflection API, enabling us to work with different
 * AspectJ instantiation models such as "singleton", "pertarget" and "perthis".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AspectJExpressionPointcut
 * @since 4.0
 */
public class AspectMetadata implements Serializable {

  /**
   * The name of this aspect as defined to Framework (the bean name) -
   * allows us to determine if two pieces of advice come from the
   * same aspect and hence their relative precedence.
   */
  private final String aspectName;

  /**
   * The aspect class, stored separately for re-resolution of the
   * corresponding AjType on deserialization.
   */
  private final Class<?> aspectClass;

  /**
   * AspectJ reflection information (AspectJ 5 / Java 5 specific).
   * Re-resolved on deserialization since it isn't serializable itself.
   */
  private transient AjType<?> ajType;

  /**
   * Framework AOP pointcut corresponding to the per clause of the
   * aspect. Will be the Pointcut.TRUE canonical instance in the
   * case of a singleton, otherwise an AspectJExpressionPointcut.
   */
  private final Pointcut perClausePointcut;

  /**
   * Create a new AspectMetadata instance for the given aspect class.
   *
   * @param aspectClass the aspect class
   * @param aspectName the name of the aspect
   */
  public AspectMetadata(Class<?> aspectClass, String aspectName) {
    this.aspectName = aspectName;

    Class<?> currClass = aspectClass;
    AjType<?> ajType = null;
    while (currClass != Object.class) {
      AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
      if (ajTypeToCheck.isAspect()) {
        ajType = ajTypeToCheck;
        break;
      }
      currClass = currClass.getSuperclass();
    }
    if (ajType == null) {
      throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
    }
    if (ajType.getDeclarePrecedence().length > 0) {
      throw new IllegalArgumentException("DeclarePrecedence not presently supported in Framework AOP");
    }
    this.aspectClass = ajType.getJavaClass();
    this.ajType = ajType;

    switch (this.ajType.getPerClause().getKind()) {
      case SINGLETON -> {
        this.perClausePointcut = Pointcut.TRUE;
      }
      case PERTARGET, PERTHIS -> {
        AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
        ajexp.setLocation(aspectClass.getName());
        ajexp.setExpression(findPerClause(aspectClass));
        ajexp.setPointcutDeclarationScope(aspectClass);
        this.perClausePointcut = ajexp;
      }
      case PERTYPEWITHIN -> {
        // Works with a type pattern
        this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
      }
      default -> throw new AopConfigException(
              "PerClause " + ajType.getPerClause().getKind() + " not supported by Framework AOP for " + aspectClass);
    }
  }

  /**
   * Extract contents from String of form {@code pertarget(contents)}.
   */
  private String findPerClause(Class<?> aspectClass) {
    String str = aspectClass.getAnnotation(Aspect.class).value();
    int beginIndex = str.indexOf('(') + 1;
    int endIndex = str.length() - 1;
    return str.substring(beginIndex, endIndex);
  }

  /**
   * Return AspectJ reflection information.
   */
  public AjType<?> getAjType() {
    return this.ajType;
  }

  /**
   * Return the aspect class.
   */
  public Class<?> getAspectClass() {
    return this.aspectClass;
  }

  /**
   * Return the aspect name.
   */
  public String getAspectName() {
    return this.aspectName;
  }

  /**
   * Return a Framework pointcut expression for a singleton aspect.
   * (e.g. {@code Pointcut.TRUE} if it's a singleton).
   */
  public Pointcut getPerClausePointcut() {
    return this.perClausePointcut;
  }

  /**
   * Return whether the aspect is defined as "perthis" or "pertarget".
   */
  public boolean isPerThisOrPerTarget() {
    PerClauseKind kind = getAjType().getPerClause().getKind();
    return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
  }

  /**
   * Return whether the aspect is defined as "pertypewithin".
   */
  public boolean isPerTypeWithin() {
    PerClauseKind kind = getAjType().getPerClause().getKind();
    return (kind == PerClauseKind.PERTYPEWITHIN);
  }

  /**
   * Return whether the aspect needs to be lazily instantiated.
   */
  public boolean isLazilyInstantiated() {
    return (isPerThisOrPerTarget() || isPerTypeWithin());
  }

  @Serial
  private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
    inputStream.defaultReadObject();
    this.ajType = AjTypeSystem.getAjType(this.aspectClass);
  }

}
