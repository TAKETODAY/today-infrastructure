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

package cn.taketoday.web.handler.method.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * A {@link UriComponentsContributor} containing a list of other contributors
 * to delegate to and also encapsulating a specific {@link ConversionService} to
 * use for formatting method argument values as Strings.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/7 22:03
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {

  private final List<Object> contributors;

  private final ConversionService conversionService;

  /**
   * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
   * {@link cn.taketoday.web.bind.resolver.ParameterResolvingStrategy ParameterResolvingStrategies}.
   * Since both of these tend to be implemented by the same class, the most convenient option is
   * to obtain the configured {@code ParameterResolvingStrategies} in {@code RequestMappingHandlerAdapter}
   * and provide that to this constructor.
   *
   * @param contributors a collection of {@link UriComponentsContributor}
   * or {@link ParameterResolvingStrategies HandlerMethodArgumentResolvers}
   */
  public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
    this.contributors = Arrays.asList(contributors);
    this.conversionService = new DefaultFormattingConversionService();
  }

  /**
   * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
   * {@link ParameterResolvingStrategies ParameterResolvingStrategies}. Since both of these tend to be implemented
   * by the same class, the most convenient option is to obtain the configured
   * {@code HandlerMethodArgumentResolvers} in {@code RequestMappingHandlerAdapter}
   * and provide that to this constructor.
   *
   * @param contributors a collection of {@link UriComponentsContributor}
   * or {@link ParameterResolvingStrategies ParameterResolvingStrategies}
   */
  public CompositeUriComponentsContributor(Collection<?> contributors) {
    this(contributors, null);
  }

  /**
   * Create an instance from a collection of {@link UriComponentsContributor UriComponentsContributors} or
   * {@link ParameterResolvingStrategies ParameterResolvingStrategies}. Since both of these tend to be implemented
   * by the same class, the most convenient option is to obtain the configured
   * {@code HandlerMethodArgumentResolvers} in the {@code RequestMappingHandlerAdapter}
   * and provide that to this constructor.
   * <p>If the {@link ConversionService} argument is {@code null},
   * {@link cn.taketoday.format.support.DefaultFormattingConversionService}
   * will be used by default.
   *
   * @param contributors a collection of {@link UriComponentsContributor}
   * or {@link ParameterResolvingStrategies ParameterResolvingStrategies}
   * @param cs a ConversionService to use when method argument values
   * need to be formatted as Strings before being added to the URI
   */
  public CompositeUriComponentsContributor(@Nullable Collection<?> contributors, @Nullable ConversionService cs) {
    this.contributors = (contributors != null ? new ArrayList<>(contributors) : Collections.emptyList());
    this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
  }

  /**
   * Determine if this {@code CompositeUriComponentsContributor} has any
   * contributors.
   *
   * @return {@code true} if this {@code CompositeUriComponentsContributor}
   * was created with contributors to delegate to
   */
  public boolean hasContributors() {
    return !this.contributors.isEmpty();
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    ResolvableMethodParameter resolvable = null;
    for (Object contributor : this.contributors) {
      if (contributor instanceof UriComponentsContributor ucc) {
        if (ucc.supportsParameter(parameter)) {
          return true;
        }
      }
      else if (contributor instanceof ParameterResolvingStrategy resolver) {
        if (resolvable == null) {
          resolvable = new ResolvableMethodParameter(parameter);
        }
        if (resolver.supportsParameter(resolvable)) {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public void contributeMethodArgument(MethodParameter parameter, Object value,
          UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
    ResolvableMethodParameter resolvable = new ResolvableMethodParameter(parameter);
    for (Object contributor : this.contributors) {
      if (contributor instanceof UriComponentsContributor ucc) {
        if (ucc.supportsParameter(parameter)) {
          ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
          break;
        }
      }
      else if (contributor instanceof ParameterResolvingStrategy resolver) {
        if (resolver.supportsParameter(resolvable)) {
          break;
        }
      }
    }
  }

  /**
   * An overloaded method that uses the ConversionService created at construction.
   */
  public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
          Map<String, Object> uriVariables) {

    contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
  }

}
