package br.ufrn.middleware.lifecycle;

public class PerRequestLifecycleManager implements LifecycleManager {
    private final Class<?> controllerClass;

    public PerRequestLifecycleManager(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    @Override
    public Object getInstance() throws Exception {
        return controllerClass.getDeclaredConstructor().newInstance();
    }
}
