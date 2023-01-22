package com.isa.control;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.isa.Coin;
import com.isa.Endpoints;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Data {
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void serializer(Object object, String file){
        saveToFile(gson.toJson(object), file);
    }
    public static Coin[] deserializeCoin(){
        return new Gson().fromJson(loadFile("coin.json"), Coin[].class);
    }
    public static Coin[] deserializeCoin(String file){
        return new Gson().fromJson(loadFile(file), Coin[].class);
    }
    public static List<String> deserializeEndpoints(){
        return new Gson().fromJson(loadFile("endpoints.json"), Endpoints.getEndpoints().getClass());
    }
    public static Map<String,String> deserialize(String file, Object object){
        return new Gson().fromJson(loadFile(file), (Type) object.getClass());
    }
    public static Map<String,String> deserializeRequest(String response, Object object){
        return new Gson().fromJson(response, (Type) object.getClass());
    }
    public static String loadFile(String file){
        Path path = Path.of("src", "main", "resources", file);
        String fromFile = null;
        try {
            fromFile = Files.readString(path);
        } catch (IOException e) {
            System.out.println("file not exist");
        }
        return fromFile;
    }
    public static void saveToFile(String data, String file){
        Path path = Path.of("src", "main", "resources", file);
        try {
            Files.writeString(path, data);
        } catch (IOException e) {
            System.out.println("path not exist " + e);
        }
    }
    public static String sendHttpRequest(String api) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(api)).build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);  //FIXME
        }
        return response.body();
    }

    public static void updateCoinList(){
        String response = sendHttpRequest(Endpoints.buildRequest());
        saveToFile(response, "coin.json");
        System.out.println("Lista zaktualizowana pomyślnie");
    }

    public static Map<String, String> deserializeCoinsNames() {
        return gson.fromJson(loadFile("availableCoins.json"), Endpoints.getCoinsNames().getClass());

    }
}
