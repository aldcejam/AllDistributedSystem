package ufrn.aldcejam;

import br.ufrn.transport.core.Transport;
import br.ufrn.transport.core.TransportFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HealthChecker {

    private static final int CHECK_INTERVAL_MS = 1000;
    private static final int MAX_FAILURES = 2;

    private final ServiceRegistry registry;
    private final Map<String, Transport> transports = new ConcurrentHashMap<>();

    public HealthChecker(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void start() {
        Thread.ofVirtual().name("health-checker").start(this::run);
    }

    private void run() {
        while (true) {
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
                checkAll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[HEALTH] Erro no health check: " + e.getMessage());
            }
        }
    }

    private void checkAll() {
        Map<String, List<ServiceRegistry.InstanceInfo>> allServices = registry.getAllServices();

        for (Map.Entry<String, List<ServiceRegistry.InstanceInfo>> entry : allServices.entrySet()) {
            String serviceName = entry.getKey();
            List<ServiceRegistry.InstanceInfo> instances = new ArrayList<>(entry.getValue());

            for (ServiceRegistry.InstanceInfo instance : instances) {
                Transport transport = transports.computeIfAbsent(instance.transport(), TransportFactory::create);
                boolean healthy = transport.checkHealth(serviceName, instance.host(), instance.port());

                if (healthy) {
                    registry.resetFailure(serviceName, instance);
                } else {
                    int failures = registry.incrementFailure(serviceName, instance);
                    if (failures >= MAX_FAILURES) {
                        registry.removeInstance(serviceName, instance);
                        System.out.println("[HEALTH] Instância removida: " + serviceName
                                + " em " + instance.host() + ":" + instance.port());
                    }
                }
            }
        }
    }
}
