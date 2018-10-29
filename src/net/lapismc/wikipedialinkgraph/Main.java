package net.lapismc.wikipedialinkgraph;

class Main {

    public static void main(String[] args){
        Runnable r = LinkGraph::new;
        Thread thread = new Thread(r);
        thread.start();
    }

}
