package de.exbio.reposcapeweb.communication.cache;

import de.exbio.reposcapeweb.communication.reponses.WebNode;

public class Node {

    private int id;
    private String name;
    private boolean hasEdge =false;


    public Node(int id, String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean hasEdge() {
        return hasEdge;
    }

    public void setHasEdge(boolean hasEdge) {
        this.hasEdge = hasEdge;
    }

    public WebNode toWebNode(){
        return new WebNode(id,name,hasEdge);
    }
}