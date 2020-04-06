package ru.arossomakhina;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.rx.client.FindObservable;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.Success;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.bson.Document;
import org.bson.conversions.Bson;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WebApp {

    private static HashMap<String, HashMap<String, Double>> currencyExchange = new HashMap<>();
    private static MongoClient client = createMongoClient();

    private static String singleParameter(String parameter, Map<String, List<String>> map) {
        List<String> parameters = map.get(parameter);
        if (parameters == null || parameters.size() != 1) {
            throw new IllegalArgumentException();
        }
        return parameters.get(0);
    }

    private static Currency toCurrency(String string) {
        switch (string.toLowerCase()) {
            case "eur":
                return Currency.EUR;
            case "usd":
                return Currency.USD;
            case "rur":
                return Currency.RUR;
        }
        throw new IllegalStateException("No such currency");
    }

    private static ObjectMapper mapper = new ObjectMapper();

    enum Currency {
        EUR, USD, RUR
    }

    public static void main(String[] args) {
        runServer().awaitShutdown();
    }

    static HttpServer<ByteBuf, ByteBuf> runServer() {
        return HttpServer
                .newServer(8080)
                .start((req, resp) -> {
                    HttpMethod method = req.getHttpMethod();
                    Map<String, List<String>> parameters = req.getQueryParameters();
                    String path = req.getDecodedPath();
                    System.out.println(path);

                    if (HttpMethod.POST.equals(method)) {
                        if ("/createUser".equals(path))
                            return resp.writeString(createUser(parameters));
                        else if ("/createGood".equals(path))
                            return resp.writeString(createGood(parameters));
                    } else if (HttpMethod.GET.equals(method)) {
                        if ("/getUser".equals(path))
                            return resp.writeString(getUser(singleParameter("id", parameters)).map(WebApp::printJson));
                        else if ("/getGoods".equals(path)) {
                            return resp.writeString(getGoods(getUser(singleParameter("id", parameters))));
                        } else if ("/getGood".equals(path))
                            return resp.writeString(getGood(singleParameter("id", parameters)).map(WebApp::printJson));
                    }

                    Observable<String> response = Observable
                            .just("Wrong path: " + path);

                    return resp.writeString(response);
                });
    }

    private static MongoClient createMongoClient() {
        HashMap<String, Double> fromRUR = new HashMap<>();
        HashMap<String, Double> fromUSD = new HashMap<>();
        HashMap<String, Double> fromEUR = new HashMap<>();
        fromRUR.put("EUR", 0.012);
        fromRUR.put("USD", 0.013);
        fromRUR.put("RUR", 1.0);

        fromEUR.put("RUR", 83.4);
        fromEUR.put("USD", 1.08);
        fromEUR.put("EUR", 1.0);

        fromUSD.put("RUR", 77.1);
        fromUSD.put("EUR", 0.92);
        fromUSD.put("USD", 1.0);
        currencyExchange.put("RUR", fromRUR);
        currencyExchange.put("EUR", fromEUR);
        currencyExchange.put("USD", fromUSD);
        return MongoClients.create("mongodb://localhost:27017");
    }

    private static Observable<String> createUser(Map<String, List<String>> parameters) {
        String name = singleParameter("name", parameters);
        Currency currency = toCurrency(singleParameter("currency", parameters));
        String id = String.valueOf(ThreadLocalRandom.current().nextLong());
        Document doc = new Document();
        doc.append("name", name).append("currency", currency.name()).append("_id", id);
        Observable<Success> result = client.getDatabase("db").getCollection("Users").insertOne(doc);
        return result.map(e -> id);
    }

    private static Observable<User> getUser(String id) {
        Bson filter = Filters.eq("_id", id);
        FindObservable<Document> result = client.getDatabase("db").getCollection("Users").find(filter);
        return result.toObservable().map(
                e -> new User(e.getString("_id"),
                        e.getString("name"),
                        e.getString("currency"))
        ).singleOrDefault(null);
    }

    private static Observable<String> createGood(Map<String, List<String>> parameters) {
        String name = singleParameter("name", parameters);
        String price = singleParameter("price", parameters);
        Currency currency = toCurrency(singleParameter("currency", parameters));
        Document doc = new Document();
        String id = String.valueOf(ThreadLocalRandom.current().nextLong());
        doc.append("name", name).append("price", price).append("currency", currency.name()).append("_id", id);
        Observable<Success> result = client.getDatabase("db").getCollection("Goods").insertOne(doc);
        return result.map(e -> id);
    }

    private static Observable<Good> getGood(String id) {
        Bson filter = Filters.eq("_id", id);
        FindObservable<Document> result = client.getDatabase("db").getCollection("Goods").find(filter);
        return result.toObservable().map(
                e -> new Good(e.getString("_id"),
                        e.getString("name"),
                        e.getString("price"),
                        e.getString("currency"))
        ).singleOrDefault(null);
    }

    private static Observable<String> getGoods(Observable<User> user) {
        return user.flatMap(u -> {
            FindObservable<Document> result = client.getDatabase("db").getCollection("Goods").find();
            List<Good> goods = new ArrayList<>();
            return result.toObservable()
                    .map(e -> new Good(
                            e.getString("_id"),
                            e.getString("name"),
                            Double.toString(convert(e.getString("currency"),
                                    u.currency,
                                    Double.parseDouble(e.getString("price")))),
                            u.currency)
                    ).collect(() -> goods, List::add)
                    .map(WebApp::printJson);
        });
    }

    private static String printJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Throwable any) {
            throw new RuntimeException(any);
        }
    }

    private static double convert(String curFrom, String curTo, Double price) {
        return price * currencyExchange.get(curFrom).get(curTo);
    }
}
