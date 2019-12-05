package cn.taketoday.context.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.Scope;
import cn.taketoday.context.utils.Bean.P;
import cn.taketoday.context.utils.Bean.S;

@S("s")
@P("p")
public class Bean {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @C(scope = Scope.PROTOTYPE)
    public @interface A {
        String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @C(scope = Scope.SINGLETON)
    public @interface S {
        String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface C {
        String[] value() default {};

        Scope scope() default Scope.SINGLETON;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @A
    public @interface D {
        String[] value() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @D
    public @interface P {
        String[] value() default {};
    }
}
