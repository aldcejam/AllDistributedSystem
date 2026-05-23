package br.ufrn.middleware.remoting;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Params;
import br.ufrn.middleware.identification.RouteDefinition;
import br.ufrn.middleware.lifecycle.LifecycleManager;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class Invoker {
    private final Marshaller marshaller = new Marshaller();

    public Object invoke(MiddlewareRequest request, RouteDefinition definition, LifecycleManager lifecycleManager) throws Exception {
        Object controllerInstance = lifecycleManager.getInstance();
        Method method = definition.method();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(Body.class)) {
                args[i] = marshaller.unmarshal(request.body(), param.getParameterizedType());
            } else if (param.isAnnotationPresent(Params.class)) {
                Params paramsAnno = param.getAnnotation(Params.class);
                String paramName = paramsAnno.value().isEmpty() ? param.getName() : paramsAnno.value();
                String value = request.queryParams().get(paramName);
                // Conversão simples para String por enquanto. Poderia ser mais complexo.
                args[i] = value;
            }
        }

        return method.invoke(controllerInstance, args);
    }

    public String serializeResponse(Object response) {
        return marshaller.marshal(response);
    }
}
