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
            // Se o pool estiver vazio, cria uma nova instância temporária ou espera?
            // Para simplificar, vamos criar uma nova se o pool estiver vazio.
            return controllerClass.getDeclaredConstructor().newInstance();
        }
        // Nota: Em um pool real, precisaríamos de uma forma de devolver o objeto ao pool.
        // Como o Invoker é quem gerencia a chamada, ele teria que saber como devolver.
        // Para este trabalho, vamos simplificar e apenas entregar uma instância.
        pool.offer(instance); // "Devolve" imediatamente para simplificar o fluxo sem estado.
        return instance;
    }
}
