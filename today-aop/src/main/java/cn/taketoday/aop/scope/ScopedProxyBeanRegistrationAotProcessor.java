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

package cn.taketoday.aop.scope;

import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragments;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link BeanRegistrationAotProcessor} for {@link ScopedProxyFactoryBean}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 10:59
 */
class ScopedProxyBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ScopedProxyBeanRegistrationAotProcessor.class);

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    if (beanClass.equals(ScopedProxyFactoryBean.class)) {
      String targetBeanName = getTargetBeanName(registeredBean.getMergedBeanDefinition());
      BeanDefinition targetBeanDefinition =
              getTargetBeanDefinition(registeredBean.getBeanFactory(), targetBeanName);
      if (targetBeanDefinition == null) {
        logger.warn("Could not handle {}: no target bean definition found with name {}",
                ScopedProxyFactoryBean.class.getSimpleName(), targetBeanName);
        return null;
      }
      return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
              new ScopedProxyBeanRegistrationCodeFragments(codeFragments, registeredBean,
                      targetBeanName, targetBeanDefinition));
    }
    return null;
  }

  @Nullable
  private String getTargetBeanName(BeanDefinition beanDefinition) {
    Object value = beanDefinition.getPropertyValues().getPropertyValue("targetBeanName");
    return (value instanceof String targetBeanName ? targetBeanName : null);
  }

  @Nullable
  private BeanDefinition getTargetBeanDefinition(
          ConfigurableBeanFactory beanFactory, @Nullable String targetBeanName) {

    if (targetBeanName != null && beanFactory.containsBean(targetBeanName)) {
      return beanFactory.getMergedBeanDefinition(targetBeanName);
    }
    return null;
  }

  private static class ScopedProxyBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

    private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

    private final RegisteredBean registeredBean;

    private final String targetBeanName;

    private final BeanDefinition targetBeanDefinition;

    ScopedProxyBeanRegistrationCodeFragments(BeanRegistrationCodeFragments delegate,
            RegisteredBean registeredBean, String targetBeanName, BeanDefinition targetBeanDefinition) {

      super(delegate);
      this.registeredBean = registeredBean;
      this.targetBeanName = targetBeanName;
      this.targetBeanDefinition = targetBeanDefinition;
    }

    @Override
    public ClassName getTarget(RegisteredBean registeredBean) {
      return ClassName.get(targetBeanDefinition.getResolvableType().toClass());
    }

    @Override
    public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
            ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

      return super.generateNewBeanDefinitionCode(generationContext,
              targetBeanDefinition.getResolvableType(), beanRegistrationCode);
    }

    @Override
    public CodeBlock generateSetBeanDefinitionPropertiesCode(
            GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode,
            RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

      RootBeanDefinition processedBeanDefinition = new RootBeanDefinition(beanDefinition);
      processedBeanDefinition.setTargetType(targetBeanDefinition.getResolvableType());
      processedBeanDefinition.getPropertyValues()
              .remove("targetBeanName");
      return super.generateSetBeanDefinitionPropertiesCode(generationContext,
              beanRegistrationCode, processedBeanDefinition, attributeFilter);
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {

      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods()
              .add("getScopedProxyInstance", method -> {
                method.addJavadoc("Create the scoped proxy bean instance for '$L'.",
                        registeredBean.getBeanName());
                method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
                method.returns(ScopedProxyFactoryBean.class);
                method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
                method.addStatement("$T factory = new $T()",
                        ScopedProxyFactoryBean.class, ScopedProxyFactoryBean.class);
                method.addStatement("factory.setTargetBeanName($S)", this.targetBeanName);
                method.addStatement("factory.setBeanFactory($L.getBeanFactory())", REGISTERED_BEAN_PARAMETER_NAME);
                method.addStatement("return factory");
              });
      return CodeBlock.of("$T.of($L)", InstanceSupplier.class,
              generatedMethod.toMethodReference().toCodeBlock());
    }

  }

}

