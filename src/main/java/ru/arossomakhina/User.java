package ru.arossomakhina;

public class User {
    public final String _id;
    public final String name;
    public final String currency;

    User(String _id, String name, String currency) {
        this._id = _id;
        this.name = name;
        this.currency = currency;
    }

    public User() {
        _id = "";
        name = "";
        currency = "";
    }

    @Override
    public String toString() {
        return "User{" + "id=" + _id + ", name='" + name + '\'' + ", currency='" + currency + '\'' + '}';
    }
}
