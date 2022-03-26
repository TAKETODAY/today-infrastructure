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
package cn.taketoday.classify;

import cn.taketoday.classify.util.MethodInvoker;
import cn.taketoday.classify.util.MethodInvokerUtils;
import cn.taketoday.lang.Assert;

/**
 * Wrapper for an object to adapt it to the {@link Classifier} interface.
 *
 * @param <C> the type of the thing to classify
 * @param <T> the output of the classifier
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class ClassifierAdapter<C, T> implements Classifier<C, T> {

  private MethodInvoker invoker;

  private Classifier<C, T> classifier;

  /**
   * Default constructor for use with setter injection.
   */
  public ClassifierAdapter() {
    super();
  }

  /**
   * Create a new {@link Classifier} from the delegate provided. Use the constructor as
   * an alternative to the {@link #setDelegate(Object)} method.
   *
   * @param delegate the delegate
   */
  public ClassifierAdapter(Object delegate) {
    setDelegate(delegate);
  }

  /**
   * Create a new {@link Classifier} from the delegate provided. Use the constructor as
   * an alternative to the {@link #setDelegate(Classifier)} method.
   *
   * @param delegate the classifier to delegate to
   */
  public ClassifierAdapter(Classifier<C, T> delegate) {
    this.classifier = delegate;
  }

  public void setDelegate(Classifier<C, T> delegate) {
    this.classifier = delegate;
    this.invoker = null;
  }

  /**
   * Search for the {@link cn.taketoday.classify.annotation.Classifier
   * Classifier} annotation on a method in the supplied delegate and use that to create
   * a {@link Classifier} from the parameter type to the return type. If the annotation
   * is not found a unique non-void method with a single parameter will be used, if it
   * exists. The signature of the method cannot be checked here, so might be a runtime
   * exception when the method is invoked if the signature doesn't match the classifier
   * types.
   *
   * @param delegate an object with an annotated method
   */
  public final void setDelegate(Object delegate) {
    this.classifier = null;
    this.invoker = MethodInvokerUtils
            .getMethodInvokerByAnnotation(cn.taketoday.classify.annotation.Classifier.class, delegate);
    if (this.invoker == null) {
      this.invoker = MethodInvokerUtils.<C, T>getMethodInvokerForSingleArgument(delegate);
    }
    Assert.state(this.invoker != null, "No single argument public method with or without "
            + "@Classifier was found in delegate of type " + delegate.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public T classify(C classifiable) {
    if (this.classifier != null) {
      return this.classifier.classify(classifiable);
    }
    return (T) this.invoker.invokeMethod(classifiable);
  }

}
