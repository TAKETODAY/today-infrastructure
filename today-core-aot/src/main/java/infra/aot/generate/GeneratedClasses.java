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

package infra.aot.generate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import infra.javapoet.ClassName;
import infra.javapoet.TypeSpec;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A managed collection of generated classes.
 *
 * <p>This class is stateful, so the same instance should be used for all class
 * generation.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GeneratedClass
 * @since 4.0
 */
public class GeneratedClasses {

  private final ClassNameGenerator classNameGenerator;

  private final List<GeneratedClass> classes;

  private final Map<Owner, GeneratedClass> classesByOwner;

  /**
   * Create a new instance using the specified naming conventions.
   *
   * @param classNameGenerator the class name generator to use
   */
  GeneratedClasses(ClassNameGenerator classNameGenerator) {
    this(classNameGenerator, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  private GeneratedClasses(ClassNameGenerator classNameGenerator,
          List<GeneratedClass> classes, Map<Owner, GeneratedClass> classesByOwner) {
    Assert.notNull(classNameGenerator, "'classNameGenerator' is required");
    this.classNameGenerator = classNameGenerator;
    this.classes = classes;
    this.classesByOwner = classesByOwner;
  }

  /**
   * Get or add a generated class for the specified {@code featureName} and no
   * particular component. If this method has previously been called with the
   * given {@code featureName} the existing class will be returned, otherwise
   * a new class will be generated.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param type a {@link Consumer} used to build the type
   * @return an existing or newly generated class
   */
  public GeneratedClass getOrAddForFeature(String featureName,
          Consumer<TypeSpec.Builder> type) {

    Assert.hasLength(featureName, "'featureName' must not be empty");
    Assert.notNull(type, "'type' is required");
    Owner owner = new Owner(this.classNameGenerator.getFeatureNamePrefix(), featureName, null);
    GeneratedClass generatedClass = this.classesByOwner.computeIfAbsent(owner, key -> createAndAddGeneratedClass(featureName, null, type));
    generatedClass.assertSameType(type);
    return generatedClass;
  }

  /**
   * Get or add a generated class for the specified {@code featureName}
   * targeting the specified {@code component}. If this method has previously
   * been called with the given {@code featureName}/{@code target} the
   * existing class will be returned, otherwise a new class will be generated,
   * otherwise a new class will be generated.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param targetComponent the target component
   * @param type a {@link Consumer} used to build the type
   * @return an existing or newly generated class
   */
  public GeneratedClass getOrAddForFeatureComponent(String featureName,
          ClassName targetComponent, Consumer<TypeSpec.Builder> type) {

    Assert.hasLength(featureName, "'featureName' must not be empty");
    Assert.notNull(targetComponent, "'targetComponent' is required");
    Assert.notNull(type, "'type' is required");
    Owner owner = new Owner(this.classNameGenerator.getFeatureNamePrefix(), featureName, targetComponent);
    GeneratedClass generatedClass = this.classesByOwner.computeIfAbsent(owner, key ->
            createAndAddGeneratedClass(featureName, targetComponent, type));
    generatedClass.assertSameType(type);
    return generatedClass;
  }

  /**
   * Get or add a generated class for the specified {@code featureName}
   * targeting the specified {@code component}. If this method has previously
   * been called with the given {@code featureName}/{@code target} the
   * existing class will be returned, otherwise a new class will be generated,
   * otherwise a new class will be generated.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param targetComponent the target component
   * @param type a {@link Consumer} used to build the type
   * @return an existing or newly generated class
   */
  public GeneratedClass getOrAddForFeatureComponent(String featureName,
          Class<?> targetComponent, Consumer<TypeSpec.Builder> type) {

    return getOrAddForFeatureComponent(featureName, ClassName.get(targetComponent), type);
  }

  /**
   * Add a new generated class for the specified {@code featureName} and no
   * particular component.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param type a {@link Consumer} used to build the type
   * @return the newly generated class
   */
  public GeneratedClass addForFeature(String featureName, Consumer<TypeSpec.Builder> type) {
    Assert.hasLength(featureName, "'featureName' must not be empty");
    Assert.notNull(type, "'type' is required");
    return createAndAddGeneratedClass(featureName, null, type);
  }

  /**
   * Add a new generated class for the specified {@code featureName} targeting
   * the specified {@code component}.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param targetComponent the target component
   * @param type a {@link Consumer} used to build the type
   * @return the newly generated class
   */
  public GeneratedClass addForFeatureComponent(String featureName,
          ClassName targetComponent, Consumer<TypeSpec.Builder> type) {

    Assert.hasLength(featureName, "'featureName' must not be empty");
    Assert.notNull(targetComponent, "'targetComponent' is required");
    Assert.notNull(type, "'type' is required");
    return createAndAddGeneratedClass(featureName, targetComponent, type);
  }

  /**
   * Add a new generated class for the specified {@code featureName} targeting
   * the specified {@code component}.
   *
   * @param featureName the name of the feature to associate with the
   * generated class
   * @param targetComponent the target component
   * @param type a {@link Consumer} used to build the type
   * @return the newly generated class
   */
  public GeneratedClass addForFeatureComponent(String featureName,
          Class<?> targetComponent, Consumer<TypeSpec.Builder> type) {

    return addForFeatureComponent(featureName, ClassName.get(targetComponent), type);
  }

  private GeneratedClass createAndAddGeneratedClass(String featureName,
          @Nullable ClassName targetComponent, Consumer<TypeSpec.Builder> type) {

    ClassName className = this.classNameGenerator.generateClassName(featureName, targetComponent);
    GeneratedClass generatedClass = new GeneratedClass(className, type);
    this.classes.add(generatedClass);
    return generatedClass;
  }

  /**
   * Write the {@link GeneratedClass generated classes} using the given
   * {@link GeneratedFiles} instance.
   *
   * @param generatedFiles where to write the generated classes
   */
  void writeTo(GeneratedFiles generatedFiles) {
    Assert.notNull(generatedFiles, "'generatedFiles' is required");
    List<GeneratedClass> generatedClasses = new ArrayList<>(this.classes);
    generatedClasses.sort(Comparator.comparing(GeneratedClass::getName));
    for (GeneratedClass generatedClass : generatedClasses) {
      generatedFiles.addSourceFile(generatedClass.generateJavaFile());
    }
  }

  /**
   * Create a new {@link GeneratedClasses} instance using the specified feature
   * name prefix to qualify generated class names for a dedicated round of code
   * generation.
   *
   * @param featureNamePrefix the feature name prefix to use
   * @return a new instance for the specified feature name prefix
   */
  GeneratedClasses withFeatureNamePrefix(String featureNamePrefix) {
    return new GeneratedClasses(this.classNameGenerator.withFeatureNamePrefix(featureNamePrefix),
            this.classes, this.classesByOwner);
  }

  private record Owner(String featureNamePrefix, String featureName, @Nullable ClassName target) {
  }

}
