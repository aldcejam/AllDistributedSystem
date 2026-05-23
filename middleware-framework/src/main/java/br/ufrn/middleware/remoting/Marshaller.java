package br.ufrn.middleware.remoting;

import com.google.gson.Gson;
import java.lang.reflect.Type;

public class Marshaller {
    private final Gson gson = new Gson();

    public <T> T unmarshal(String data, Type type) {
        if (data == null || data.isEmpty()) return null;
        return gson.fromJson(data, type);
    }

    public String marshal(Object object) {
        return gson.toJson(object);
    }
}
