package br.ufrn.middleware.identification;

import br.ufrn.middleware.remoting.HttpMethod;
import java.lang.reflect.Method;

public record RouteDefinition(HttpMethod httpMethod, String path, Class<?> controllerClass, Method method) {
}
