package com.codedocsy.common;

public class CountItem {
    public CountItem(String key, Integer count) {
        this.key = key;
        this.count = count;
    }

    private String key;
    private Integer count;

    public void setKey(String key) {
        this.key = key;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "CountItem{" +
                "key='" + key + '\'' +
                ", count=" + count +
                '}';
    }
}
