/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.lang.Nullable;

/**
 * Wrapper for an object to adapt it to the {@link Classifier} interface.
 *
 * @param <C> the type of the thing to classify
 * @param <T> the output of the classifier
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ClassifierAdapter<C, T> implements Classifier<C, T> {

  private MethodInvoker invoker;

  @Nullable
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
  public ClassifierAdapter(@Nullable Classifier<C, T> delegate) {
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
    MethodInvoker invoker = MethodInvoker.forAnnotation(cn.taketoday.classify.annotation.Classifier.class, delegate);
    if (invoker == null) {
      try {
        invoker = MethodInvoker.forSingleArgument(delegate);
      }
      catch (IllegalStateException e) {
        throw new IllegalStateException("No single argument public method with or without "
                + "@Classifier was found in delegate of type " + delegate.getClass(), e);
      }
    }
    this.invoker = invoker;
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
