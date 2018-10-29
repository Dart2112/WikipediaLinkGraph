package net.lapismc.wikipedialinkgraph;

class Connection {

    private final String a;
    private final String b;
    private Integer weight;

    Connection(String a, String b) {
        this(a, b, 1);
    }

    private Connection(String a, String b, Integer weight) {
        this.a = a;
        this.b = b;
        this.weight = weight;
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Connection clone() {
        return new Connection(a, b, weight);
    }

}
