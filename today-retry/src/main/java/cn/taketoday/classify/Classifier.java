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

import java.io.Serializable;

/**
 * Interface for a classifier. At its simplest a {@link Classifier} is just a map from
 * objects of one type to objects of another type.
 *
 * Note that implementations can only be serializable if the parameter types are
 * themselves serializable.
 *
 * @param <C> the type of the thing to classify
 * @param <T> the output of the classifier
 * @author Dave Syer
 */
public interface Classifier<C, T> extends Serializable {

  /**
   * Classify the given object and return an object of a different type, possibly an
   * enumerated type.
   *
   * @param classifiable the input object. Can be null.
   * @return an object. Can be null, but implementations should declare if this is the
   * case.
   */
  T classify(C classifiable);

}
