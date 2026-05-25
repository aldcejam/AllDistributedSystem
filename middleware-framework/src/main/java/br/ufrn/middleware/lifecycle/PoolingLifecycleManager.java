package br.ufrn.middleware.lifecycle;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PoolingLifecycleManager implements LifecycleManager {
    private final Class<?> controllerClass;
    private final BlockingQueue<Object> pool;

    public PoolingLifecycleManager(Class<?> controllerClass, int poolSize) throws Exception {
        this.controllerClass = controllerClass;
        this.pool = new LinkedBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(controllerClass.getDeclaredConstructor().newInstance());
        }
    }

    @Override
    public Object getInstance() throws Exception {
        Object instance = pool.poll();
        if (instance == null) {
           return controllerClass.getDeclaredConstructor().newInstance();
        }
        pool.offer(instance);
        return instance;
    }
}
