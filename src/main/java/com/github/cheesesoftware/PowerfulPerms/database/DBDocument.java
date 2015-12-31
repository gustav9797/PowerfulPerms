package com.github.cheesesoftware.PowerfulPerms.database;

import java.util.Map;

public class DBDocument {
    
    protected Map<String, Object> data;
    
    public DBDocument(Map<String, Object> data) {
        this.data = data;
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public String getString(String key) {
        return (String)data.get(key);
    }
    
    public int getInt(String key) {
        return (int)((long)data.get(key));
    }
}
