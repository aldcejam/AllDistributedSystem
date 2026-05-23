package br.ufrn.middleware.annotations;

import br.ufrn.middleware.remoting.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@HttpMapping(HttpMethod.PATCH)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Patch {
    String path() default "";
}
