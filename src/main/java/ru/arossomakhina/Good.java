package ru.arossomakhina;

public class Good {
    public final String _id;
    public final String name;
    public final String price;
    public final String currency;

    Good(String id, String name, String price, String currency) {
        this._id = id;
        this.name = name;
        this.price = price;
        this.currency = currency;
    }

    public Good() {
        _id = "";
        name = "";
        price = "";
        currency = "";
    }

    @Override
    public String toString() {
        return "User{" + "id=" + _id + ", name='" + name + '\'' + ", price='" + price + '\'' + ", currency='" + currency + '\'' + '}';
    }
}
