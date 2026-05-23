package br.ufrn.middleware.lifecycle;

public class StaticLifecycleManager implements LifecycleManager {
    private final Object instance;

    public StaticLifecycleManager(Class<?> controllerClass) throws Exception {
        this.instance = controllerClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public Object getInstance() {
        return instance;
    }
}
