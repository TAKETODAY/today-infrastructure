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

package cn.taketoday.core.type.filter;

import org.aspectj.bridge.IMessageHandler;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.bcel.BcelWorld;
import org.aspectj.weaver.patterns.Bindings;
import org.aspectj.weaver.patterns.FormalBinding;
import org.aspectj.weaver.patterns.IScope;
import org.aspectj.weaver.patterns.PatternParser;
import org.aspectj.weaver.patterns.SimpleScope;
import org.aspectj.weaver.patterns.TypePattern;

import java.io.IOException;

import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Nullable;

/**
 * Type filter that uses AspectJ type pattern for matching.
 *
 * <p>A critical implementation details of this type filter is that it does not
 * load the class being examined to match with a type pattern.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/15 23:01
 */
public class AspectJTypeFilter implements TypeFilter {

  private final World world;

  private final TypePattern typePattern;

  public AspectJTypeFilter(String typePatternExpression, @Nullable ClassLoader classLoader) {
    this.world = new BcelWorld(classLoader, IMessageHandler.THROW, null);
    this.world.setBehaveInJava5Way(true);
    PatternParser patternParser = new PatternParser(typePatternExpression);
    TypePattern typePattern = patternParser.parseTypePattern();
    typePattern.resolve(this.world);
    IScope scope = new SimpleScope(this.world, new FormalBinding[0]);
    this.typePattern = typePattern.resolveBindings(scope, Bindings.NONE, false, false);
  }

  @Override
  public boolean match(
          MetadataReader metadataReader,
          MetadataReaderFactory metadataReaderFactory) throws IOException {

    String className = metadataReader.getClassMetadata().getClassName();
    ResolvedType resolvedType = this.world.resolve(className);
    return this.typePattern.matchesStatically(resolvedType);
  }

}
