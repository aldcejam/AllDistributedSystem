package br.ufrn.middleware.lifecycle;

public class LazyLifecycleManager implements LifecycleManager {
    private final Class<?> controllerClass;
    private Object instance;

    public LazyLifecycleManager(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    @Override
    public synchronized Object getInstance() throws Exception {
        if (instance == null) {
            instance = controllerClass.getDeclaredConstructor().newInstance();
        }
        return instance;
    }
}
