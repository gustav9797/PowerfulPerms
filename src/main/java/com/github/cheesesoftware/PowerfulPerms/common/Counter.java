package com.github.cheesesoftware.PowerfulPerms.common;

public class Counter {

    private int amount = 0;

    public Counter() {

    }

    public void add(int amount) {
        this.amount += amount;
    }

    public int amount() {
        return this.amount;
    }
}
