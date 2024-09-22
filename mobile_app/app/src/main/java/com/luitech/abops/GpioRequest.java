package com.luitech.abops;

public class GpioRequest {
    private String board;
    private String name;
    private int state;
    private String api_key;

    // Constructor
    public GpioRequest(String board, String name, int state, String api_key) {
        this.board = board;
        this.name = name;
        this.state = state;
        this.api_key = api_key;
    }

    // Getters and Setters
    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }
}
