package ufrn.aldcejam;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceRegistry {
    public record InstanceInfo(String host, int port, String transport) {
    }

    private final Map<String, List<InstanceInfo>> services = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();

    public void register(String name, String host, int port, String transport) {
        InstanceInfo info = new InstanceInfo(host, port, transport);
        List<InstanceInfo> instances = services.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>());
        if (!instances.contains(info)) {
            instances.add(info);
            roundRobinCounters.putIfAbsent(name, new AtomicInteger(0));
            logRegistryState();
        }
    }

    public InstanceInfo getNextInstance(String name) {
        List<InstanceInfo> instances = services.get(name);
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        AtomicInteger counter = roundRobinCounters.get(name);
        int index = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(index);
    }

    public Map<String, List<InstanceInfo>> getAllServices() {
        return services;
    }

    public void removeInstance(String name, InstanceInfo instance) {
        List<InstanceInfo> instances = services.get(name);
        if (instances != null) {
            if (instances.remove(instance)) {
                failureCounts.remove(instanceKey(name, instance));
                if (instances.isEmpty()) {
                    services.remove(name);
                    roundRobinCounters.remove(name);
                }
                logRegistryState();
            }
        }
    }

    private synchronized void logRegistryState() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[SERVICE REGISTRY] ----------------------------------------\n");
        if (services.isEmpty()) {
            sb.append(" (Empty registry)\n");
        } else {
            services.forEach((name, instances) -> {
                sb.append(String.format(" %-20s | ", name));
                for (int i = 0; i < instances.size(); i++) {
                    InstanceInfo inst = instances.get(i);
                    sb.append(inst.host()).append(":").append(inst.port());
                    if (i < instances.size() - 1)
                        sb.append(", ");
                }
                sb.append("\n");
            });
        }
        sb.append("------------------------------------------------------------\n");
        System.out.print(sb.toString());
    }

    public int incrementFailure(String name, InstanceInfo instance) {
        String key = instanceKey(name, instance);
        return failureCounts.merge(key, 1, Integer::sum);
    }

    public void resetFailure(String name, InstanceInfo instance) {
        failureCounts.remove(instanceKey(name, instance));
    }

    private String instanceKey(String name, InstanceInfo instance) {
        return name + "@" + instance.host() + ":" + instance.port();
    }
}
