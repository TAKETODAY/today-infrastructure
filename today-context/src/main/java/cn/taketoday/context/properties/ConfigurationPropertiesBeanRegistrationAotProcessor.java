/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import java.lang.reflect.Executable;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragments;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} for immutable configuration properties.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConstructorBound
 * @since 4.0
 */
class ConfigurationPropertiesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    if (!isImmutableConfigurationPropertiesBeanDefinition(registeredBean.getMergedBeanDefinition())) {
      return null;
    }
    return BeanRegistrationAotContribution.withCustomCodeFragments(
            codeFragments -> new ConfigurationPropertiesBeanRegistrationCodeFragments(
                    codeFragments, registeredBean));

  }

  private boolean isImmutableConfigurationPropertiesBeanDefinition(BeanDefinition beanDefinition) {
    return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(beanDefinition));
  }

  private static class ConfigurationPropertiesBeanRegistrationCodeFragments
          extends BeanRegistrationCodeFragmentsDecorator {

    private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

    private final RegisteredBean registeredBean;

    ConfigurationPropertiesBeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments,
            RegisteredBean registeredBean) {
      super(codeFragments);
      this.registeredBean = registeredBean;
    }

    @Override
    public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
            Predicate<String> attributeFilter) {
      return super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode,
              beanDefinition, attributeFilter.or(BindMethodAttribute.NAME::equals));
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
            boolean allowDirectSupplierShortcut) {
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
        Class<?> beanClass = this.registeredBean.getBeanClass();
        method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(beanClass)
                .addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME)
                .addStatement("$T beanFactory = registeredBean.getBeanFactory()", BeanFactory.class)
                .addStatement("$T beanName = registeredBean.getBeanName()", String.class)
                .addStatement("$T<?> beanClass = registeredBean.getBeanClass()", Class.class)
                .addStatement("return ($T) $T.from(beanFactory, beanName, beanClass)", beanClass,
                        ConstructorBound.class);
      });
      return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, beanRegistrationCode.getClassName(),
              generatedMethod.getName());
    }

  }

}
