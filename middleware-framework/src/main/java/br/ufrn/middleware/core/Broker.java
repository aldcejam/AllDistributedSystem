package br.ufrn.middleware.core;

import br.ufrn.middleware.annotations.Controller;
import br.ufrn.middleware.annotations.Get;
import br.ufrn.middleware.annotations.Post;
import br.ufrn.middleware.annotations.Put;
import br.ufrn.middleware.annotations.Delete;
import br.ufrn.middleware.annotations.Patch;
import br.ufrn.middleware.identification.Lookup;
import br.ufrn.middleware.identification.RouteDefinition;
import br.ufrn.middleware.lifecycle.LifecycleFactory;
import br.ufrn.middleware.lifecycle.LifecycleManager;
import br.ufrn.middleware.remoting.HttpMethod;
import br.ufrn.middleware.remoting.Invoker;
import br.ufrn.middleware.remoting.MiddlewareRequest;
import br.ufrn.middleware.remoting.RemotingError;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Broker {
    private final Lookup lookup = new Lookup();
    private final Invoker invoker = new Invoker();
    private final Map<Class<?>, LifecycleManager> lifecycleManagers = new HashMap<>();

    public void scan() {
        String callerClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        String rootPackage = callerClassName.contains(".") ? callerClassName.substring(0, callerClassName.indexOf('.')) : "";
        Reflections reflections = new Reflections(rootPackage);
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);

        for (Class<?> clazz : controllers) {
            Controller controllerAnno = clazz.getAnnotation(Controller.class);
            String basePath = normalizePath(controllerAnno.path());

            try {
                LifecycleManager lm = LifecycleFactory.create(controllerAnno.lifecycle(), clazz);
                lifecycleManagers.put(clazz, lm);

                for (Method method : clazz.getDeclaredMethods()) {
                    for (Map.Entry<Class<? extends Annotation>, HttpMethod> entry : HTTP_METHODS_MAP.entrySet()) {
                        Class<? extends Annotation> annoClass = entry.getKey();

                        if (method.isAnnotationPresent(annoClass)) {
                            Annotation anno = method.getAnnotation(annoClass);
                            String path = (String) annoClass.getMethod("path").invoke(anno);
                            registerRoute(entry.getValue(), basePath, path, clazz, method);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[BROKER] Erro ao registrar controlador " + clazz.getName() + ": " + e.getMessage());
            }
        }
    }

    private void registerRoute(HttpMethod httpMethod, String basePath, String methodPath, Class<?> clazz, Method method) {
        String fullPath = normalizePath(basePath) + normalizePath(methodPath);
        RouteDefinition def = new RouteDefinition(httpMethod, fullPath, clazz, method);
        lookup.register(httpMethod, fullPath, def);
    }

    public String dispatch(MiddlewareRequest request) {
        return lookup.find(request.method(), request.path())
                .map(def -> {
                    try {
                        Object result = invoker.invoke(request, def, lifecycleManagers.get(def.controllerClass()));
                        return invoker.serializeResponse(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return invoker.serializeResponse(new RemotingError("Erro interno na invocação: " + e.getMessage(), 500));
                    }
                })
                .orElseGet(() -> invoker.serializeResponse(new RemotingError("Rota não encontrada", 404)));
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    private static final Map<Class<? extends Annotation>, HttpMethod> HTTP_METHODS_MAP = Map.of(
            Get.class, HttpMethod.GET,
            Post.class, HttpMethod.POST,
            Put.class, HttpMethod.PUT,
            Delete.class, HttpMethod.DELETE,
            Patch.class, HttpMethod.PATCH
    );
}
