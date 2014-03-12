package com.wart.magister.util;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Locale;

public class DataColumn {

    public int Affinity = 0;
    public Type DataType = null;
    public String Name = "";


    public DataColumn(String var1) {
        this.Name = var1.toLowerCase(Locale.US);
    }

    public DataColumn(String var1, Type var2) {
        this.Name = var1.toLowerCase(Locale.US);
        this.setDataType(var2);
    }

    public void setDataType(Type var1) {
        this.DataType = var1;
        this.Affinity = 0;
        if (var1 == Date.class) {
            this.Affinity = 5;
        } else {
            if (var1 == Integer.class || var1 == Long.class || var1 == Long.TYPE || var1 == Integer.TYPE || var1 == Boolean.TYPE || var1 == Boolean.class) {
                this.Affinity = 2;
                return;
            }

            if (var1 == String.class) {
                this.Affinity = 1;
                return;
            }

            if (var1 == Double.class || var1 == Float.class || var1 == Double.TYPE || var1 == Float.TYPE) {
                this.Affinity = 3;
                return;
            }
        }

    }
}
