package ufrn.aldcejam;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Controller;
import br.ufrn.middleware.annotations.Get;
import br.ufrn.middleware.annotations.Post;
import br.ufrn.middleware.lifecycle.Lifecycle;

import java.util.List;
import java.util.Map;

@Controller(path = "/gateway", lifecycle = Lifecycle.STATIC)
public class GatewayController {

    private static ServiceRegistry registry;

    public static void setRegistry(ServiceRegistry serviceRegistry) {
        registry = serviceRegistry;
    }

    public record RegisterRequest(String name, String host, int port, String transport) {}

    @Post(path = "/register")
    public String register(@Body RegisterRequest request) {
        if (registry != null) {
            registry.register(request.name(), request.host(), request.port(), request.transport());
            return "{\"status\": \"Registered\", \"service\": \"" + request.name() + "\"}";
        }
        return "{\"status\": \"Error\", \"message\": \"Registry not initialized\"}";
    }

    @Get(path = "/services")
    public Map<String, List<ServiceRegistry.InstanceInfo>> listServices() {
        if (registry != null) {
            return registry.getAllServices();
        }
        return Map.of();
    }
}
