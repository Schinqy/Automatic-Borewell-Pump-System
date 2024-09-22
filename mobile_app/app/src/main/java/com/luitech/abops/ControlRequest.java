package com.luitech.abops;

public class ControlRequest {
    private String board;
    private int autonomy;

    public ControlRequest(String board, int autonomy) {
        this.board = board;
        this.autonomy = autonomy;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public int getAutonomy() {
        return autonomy;
    }

    public void setAutonomy(int autonomy) {
        this.autonomy = autonomy;
    }
}
