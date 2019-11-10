/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package com.sun.el.lang;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.EvaluationListener;
import javax.el.FunctionMapper;
import javax.el.ImportHandler;
import javax.el.VariableMapper;

/**
 * @author TODAY <br>
 *         2019-11-10 20:26
 */
public final class EvaluationContext extends ELContext {

    private final ELContext elContext;

    public EvaluationContext(ELContext elContext) {
        this.elContext = elContext;
    }

    public ELContext getELContext() {
        return elContext;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return elContext.getFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper() {
        return elContext.getVariableMapper();
    }

    @Override
    public Object getContext(Class<?> key) {
        return elContext.getContext(key);
    }

    @Override
    public ELResolver getELResolver() {
        return elContext.getELResolver();
    }

    @Override
    public boolean isPropertyResolved() {
        return elContext.isPropertyResolved();
    }

    @Override
    public void putContext(Class<?> key, Object contextObject) {
        elContext.putContext(key, contextObject);
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        elContext.setPropertyResolved(resolved);
    }

    @Override
    public Locale getLocale() {
        return elContext.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        elContext.setLocale(locale);
    }

    @Override
    public void setPropertyResolved(Object base, Object property) {
        elContext.setPropertyResolved(base, property);
    }

    @Override
    public ImportHandler getImportHandler() {
        return elContext.getImportHandler();
    }

    @Override
    public void addEvaluationListener(EvaluationListener listener) {
        elContext.addEvaluationListener(listener);
    }

    @Override
    public List<EvaluationListener> getEvaluationListeners() {
        return elContext.getEvaluationListeners();
    }

    @Override
    public void notifyBeforeEvaluation(String expression) {
        elContext.notifyBeforeEvaluation(expression);
    }

    @Override
    public void notifyAfterEvaluation(String expression) {
        elContext.notifyAfterEvaluation(expression);
    }

    @Override
    public void notifyPropertyResolved(Object base, Object property) {
        elContext.notifyPropertyResolved(base, property);
    }

    @Override
    public boolean isLambdaArgument(String name) {
        return elContext.isLambdaArgument(name);
    }

    @Override
    public Object getLambdaArgument(String name) {
        return elContext.getLambdaArgument(name);
    }

    @Override
    public void enterLambdaScope(Map<String, Object> arguments) {
        elContext.enterLambdaScope(arguments);
    }

    @Override
    public void exitLambdaScope() {
        elContext.exitLambdaScope();
    }

    @Override
    public Object convertToType(Object obj, Class<?> type) {
        return elContext.convertToType(obj, type);
    }
}
