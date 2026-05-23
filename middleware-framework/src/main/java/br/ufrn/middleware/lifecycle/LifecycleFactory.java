package br.ufrn.middleware.lifecycle;

public class LifecycleFactory {
    public static LifecycleManager create(Lifecycle type, Class<?> controllerClass) throws Exception {
        return switch (type) {
            case STATIC -> new StaticLifecycleManager(controllerClass);
            case PER_REQUEST -> new PerRequestLifecycleManager(controllerClass);
            case POOLING -> new PoolingLifecycleManager(controllerClass, 5); // Default size
            case LAZY -> new LazyLifecycleManager(controllerClass);
        };
    }
}
