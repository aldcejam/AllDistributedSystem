package br.ufrn.middleware.util;

import com.google.gson.Gson;

public class JsonMarshaller {
    private static final Gson gson = new Gson();

    /**
     * Converte um objeto (como um Map ou POJO) em uma String JSON.
     * Semelhante ao JSON.stringify do JavaScript.
     */
    public static String stringify(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Converte uma String JSON em um objeto de uma classe específica.
     * Semelhante ao JSON.parse do JavaScript.
     */
    public static <T> T parse(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}
