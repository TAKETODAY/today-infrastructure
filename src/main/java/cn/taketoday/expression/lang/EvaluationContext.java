/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.expression.lang;

import java.util.Map;

import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.ImportHandler;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2019-11-10 20:26
 */
public final class EvaluationContext extends ExpressionContext {

  @Nullable
  private final FunctionMapper fnMapper;

  @Nullable
  private final VariableMapper varMapper;

  private final ExpressionContext elContext;

  public EvaluationContext(
          ExpressionContext elContext, FunctionMapper fnMapper, VariableMapper varMapper) {
    this.elContext = elContext;
    this.fnMapper = fnMapper;
    this.varMapper = varMapper;
  }

  public ExpressionContext getContext() {
    return this.elContext;
  }

  @Nullable
  @Override
  public FunctionMapper getFunctionMapper() {
    return this.fnMapper;
  }

  @Nullable
  @Override
  public VariableMapper getVariableMapper() {
    return this.varMapper;
  }

  @Override
  public Object getContext(Class<?> key) {
    return this.elContext.getContext(key);
  }

  @Override
  public ExpressionResolver getResolver() {
    return elContext.getResolver();
  }

  @Override
  public boolean isPropertyResolved() {
    return this.elContext.isPropertyResolved();
  }

  @Override
  public void putContext(Class<?> key, Object contextObject) {
    this.elContext.putContext(key, contextObject);
  }

  @Override
  public void setPropertyResolved(boolean resolved) {
    this.elContext.setPropertyResolved(resolved);
  }

  @Override
  public void setPropertyResolved(Object base, Object property) {
    this.elContext.setPropertyResolved(base, property);
  }

  @Override
  public boolean isLambdaArgument(String arg) {
    return this.elContext.isLambdaArgument(arg);
  }

  @Override
  public Object getLambdaArgument(String arg) {
    return this.elContext.getLambdaArgument(arg);
  }

  @Override
  public void enterLambdaScope(Map<String, Object> args) {
    this.elContext.enterLambdaScope(args);
  }

  @Override
  public void exitLambdaScope() {
    this.elContext.exitLambdaScope();
  }

  @Override
  public Object convertToType(Object obj, Class<?> targetType) {
    return this.elContext.convertToType(obj, targetType);
  }

  @Override
  public ImportHandler getImportHandler() {
    return this.elContext.getImportHandler();
  }

  @Override
  public void setReturnEmptyWhenPropertyNotResolved(boolean returnEmptyWhenPropertyNotResolved) {
    elContext.setReturnEmptyWhenPropertyNotResolved(returnEmptyWhenPropertyNotResolved);
  }

  @Override
  public boolean isReturnEmptyWhenPropertyNotResolved() {
    return elContext.isReturnEmptyWhenPropertyNotResolved();
  }

}
