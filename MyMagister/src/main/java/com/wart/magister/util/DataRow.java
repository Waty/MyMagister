package com.wart.magister.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataRow extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean containsKey(Object var1) {
        return super.containsKey(((String) var1).toLowerCase());
    }

    @Override
    public Object get(Object var1) {
        String var2 = ((String) var1).toLowerCase();
        if (super.containsKey(var2)) {
            return super.get(var2);
        } else {
            System.out.println("Kolom [" + var2 + "] niet gevonden.");
            return null;
        }
    }

    public DataRow merge(Map<String, Object> var1) {
        return this.merge(var1, false);
    }

    public DataRow merge(Map<String, Object> var1, boolean override) {
        Iterator<String> var3 = var1.keySet().iterator();

        while (var3.hasNext()) {
            String var4 = var3.next();
            if (override || containsKey(var4)) put(var4, var1.get(var4));
        }

        return this;
    }

    @Override
    public Object put(String var1, Object var2) {
        return super.put(var1.toLowerCase(), var2);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        Iterator<? extends String> itter = map.keySet().iterator();

        while (itter.hasNext()) {
            String var3 = itter.next();
            put(var3.toLowerCase(), map.get(var3));
        }
    }
}
