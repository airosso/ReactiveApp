package ru.arossomakhina;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ReactiveTest {
    private static HttpServer<ByteBuf, ByteBuf> server;

    @BeforeClass
    public static void before() {
        server = WebApp.runServer();
    }

    @AfterClass
    public static void after() {
        server.shutdown();
    }

    private static <T> T parse(String string, Class<T> clazz) throws Exception {
        return new ObjectMapper().readValue(string, clazz);
    }


    @Test
    public void addUserAndGetItBack() throws Exception {
        String body = makeRequest("POST", "createUser", Map.of("name", "Arina", "currency", "EUR"));
        String user = makeRequest("GET", "getUser", Map.of("id", body));
        User arina = parse(user, User.class);
        assertEquals(arina._id, body);
        assertEquals(arina.name, "Arina");
        assertEquals(arina.currency, "EUR");
    }

    @Test
    public void addGoodAndGetItBack() throws Exception{
        String body = makeRequest("POST", "createGood", Map.of("name", "Sugar", "currency", "RUR", "price", "30"));
        String good = makeRequest("GET", "getGood", Map.of("id", body));
        Good sugar = parse(good, Good.class);
        assertEquals(sugar._id, body);
        assertEquals(sugar.name, "Sugar");
        assertEquals(sugar.currency, "RUR");
        assertEquals(sugar.price, "30");
    }


    @Test
    public void getAllGoodsWithUserCurrency() throws Exception{
        String body = makeRequest("POST", "createUser", Map.of("name", "Arina", "currency", "EUR"));
        makeRequest("POST", "createGood", Map.of("name", "Salt", "currency", "RUR", "price", "20"));
        makeRequest("POST", "createGood", Map.of("name", "Pepper", "currency", "USD", "price", "2"));
        String goods = makeRequest("GET", "getGoods", Map.of("id", body));
        Good[] products = parse(goods, Good[].class);
        for (Good product : products) {
            assertEquals(product.currency, "EUR");
        }
    }


    private String makeRequest(String method, String path, Map<String, String> parameters) throws IOException, InterruptedException {
        String params = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/" + path + "?" + params))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
