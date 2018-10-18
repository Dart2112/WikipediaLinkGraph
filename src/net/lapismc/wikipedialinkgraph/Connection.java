package net.lapismc.wikipedialinkgraph;

class Connection {

    private String a;
    private String b;
    private Integer weight;

    Connection(String a, String b) {
        this.a = a;
        this.b = b;
        weight = 1;
    }

    void increaseWeight() {
        weight += 1;
    }

    String getTitleA() {
        return a;
    }

    String getTitleB() {
        return b;
    }

    Integer getWeight() {
        return weight;
    }

    boolean isConnecting(String x, String y) {
        return (x.equals(a) && y.equals(b)) || (y.equals(a) && x.equals(b));
    }

}
