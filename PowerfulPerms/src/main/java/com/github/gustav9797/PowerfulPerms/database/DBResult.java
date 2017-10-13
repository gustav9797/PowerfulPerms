package com.github.gustav9797.PowerfulPerms.database;

import java.util.ArrayList;

import com.github.gustav9797.PowerfulPermsAPI.DBDocument;

public class DBResult {

    protected ArrayList<DBDocument> rows;
    protected boolean result;
    protected int amount = 0;
    protected int index = -1;

    public DBResult(ArrayList<DBDocument> rows) {
        this.rows = rows;
        this.result = true;
    }

    public DBResult(boolean result) {
        this.result = result;
    }

    public DBResult(boolean result, int amount) {
        this.result = result;
        this.amount = amount;
    }

    public DBDocument next() {
        index++;
        if (rows.size() >= index + 1)
            return rows.get(index);
        return null;
    }

    public boolean hasNext() {
        if (rows.size() >= index + 2)
            return rows.get(index + 1) != null;
        return false;
    }
    
    public int getRows() {
        return rows.size();
    }

    public boolean booleanValue() {
        return this.result;
    }

    public int rowsChanged() {
        return this.amount;
    }

}
