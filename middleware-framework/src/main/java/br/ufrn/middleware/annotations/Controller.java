package br.ufrn.middleware.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.ufrn.middleware.lifecycle.Lifecycle;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String path() default "";
    Lifecycle lifecycle() default Lifecycle.STATIC;
}
