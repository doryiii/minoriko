package com.duy.minoriko2.model;

public class Server {
    public final int id;
    public final String addr;
    public final Type type;

    public Server(int id, String addr, Type type) {
        this.id = id;
        this.addr = addr;
        this.type = type;
    }

    public Server(String addr, Type type) {
        this.id = -1;
        this.addr = addr;
        this.type = type;
    }

    public enum Type {
        DANBOORU("Danbooru 1.13"), GELBOORU("Gelbooru 0.2");

        private final String value;
        Type(String value) {
            this.value = value;
        }
        public String toString() {
            return value;
        }
    }
}
