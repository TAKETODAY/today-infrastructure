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

package cn.taketoday.framework.jackson;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.AccessControl;
import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.BindingReflectionHintsRegistrar;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragments;
import cn.taketoday.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} that replaces any {@link JsonMixinModuleEntries}
 * by an hard-coded equivalent. This has the effect of disabling scanning at runtime.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JsonMixinModuleEntriesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    if (registeredBean.getBeanClass().equals(JsonMixinModuleEntries.class)) {
      return BeanRegistrationAotContribution
              .withCustomCodeFragments(codeFragments -> new AotContribution(codeFragments, registeredBean));
    }
    return null;
  }

  static class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

    private final RegisteredBean registeredBean;

    private final ClassLoader classLoader;

    AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
      super(delegate);
      this.registeredBean = registeredBean;
      this.classLoader = registeredBean.getBeanFactory().getBeanClassLoader();
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
            BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
      JsonMixinModuleEntries entries = this.registeredBean.getBeanFactory()
              .getBean(this.registeredBean.getBeanName(), JsonMixinModuleEntries.class);
      contributeHints(generationContext.getRuntimeHints(), entries);
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
        Class<?> beanType = JsonMixinModuleEntries.class;
        method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName());
        method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        method.returns(beanType);
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("return $T.create(", JsonMixinModuleEntries.class).beginControlFlow("(mixins) ->");
        entries.doWithEntry(this.classLoader, (type, mixin) -> addEntryCode(code, type, mixin));
        code.endControlFlow(")");
        method.addCode(code.build());
      });
      return generatedMethod.toMethodReference().toCodeBlock();
    }

    private void addEntryCode(CodeBlock.Builder code, Class<?> type, Class<?> mixin) {
      AccessControl accessForTypes = AccessControl.lowest(AccessControl.forClass(type),
              AccessControl.forClass(mixin));
      if (accessForTypes.isPublic()) {
        code.addStatement("$L.and($T.class, $T.class)", "mixins", type, mixin);
      }
      else {
        code.addStatement("$L.and($S, $S)", "mixins", type.getName(), mixin.getName());
      }
    }

    private void contributeHints(RuntimeHints runtimeHints, JsonMixinModuleEntries entries) {
      Set<Class<?>> mixins = new LinkedHashSet<>();
      entries.doWithEntry(this.classLoader, (type, mixin) -> mixins.add(mixin));
      new BindingReflectionHintsRegistrar().registerReflectionHints(runtimeHints.reflection(),
              mixins.toArray(Class<?>[]::new));
    }

  }

}
