package com.wart.magister.util;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataTable extends ArrayList<DataRow> {

    private static final long serialVersionUID = 9169178206463011965L;
    public String TableName = "";
    public List<DataColumn> Columns = new ArrayList<DataColumn>();

    public DataTable() {
    }

    public DataTable(ArrayList<DataRow> rows) {
        if (rows.size() > 0) {
            Iterator<String> var6 = ((HashMap<String, Object>) rows.get(0)).keySet().iterator();

            while (var6.hasNext()) {
                String var7 = var6.next();
                Columns.add(new DataColumn(var7));
            }
        }

        Iterator<DataRow> var2 = rows.iterator();

        while (var2.hasNext()) {
            DataRow oldRow = var2.next();
            DataRow newRow = new DataRow();
            newRow.putAll(oldRow);
            this.add(newRow);
        }

    }

    public DataTable(Cursor var1) {
        if (var1 != null && var1.moveToFirst()) {
            String[] columnNames = var1.getColumnNames();

            for (String name : columnNames)
                Columns.add(new DataColumn(name));

            do {
                DataRow row = new DataRow();
                for (String name : columnNames)
                    row.put(name, var1.getString(var1.getColumnIndex(name)));

                this.add(row);
            } while (var1.moveToNext());
        }

    }

    public DataTable(String... strings) {
        for (String name : strings)
            Columns.add(new DataColumn(name));
    }

    public Map<String, Integer> getColumnMapping() {
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        for (int i = 0; i < Columns.size(); ++i)
            hashMap.put(Columns.get(i).Name, Integer.valueOf(i));
        return hashMap;
    }

    public String[] getColumnNames() {
        ArrayList<String> var1 = new ArrayList<String>();
        for (DataColumn column : Columns)
            var1.add(column.Name);

        return (String[]) var1.toArray();
    }

    public ArrayList<DataRow> getValuesForColumn(String name) {
        ArrayList<DataRow> result = new ArrayList<DataRow>();
        Iterator<DataRow> itter = iterator();

        while (itter.hasNext())
            result.add((DataRow) itter.next().get(name));

        return result;
    }

    public DataRow newRow() {
        DataRow row = new DataRow();
        Iterator<DataColumn> itter = Columns.iterator();

        while (itter.hasNext())
            row.put(itter.next().Name, "");

        return row;
    }

    // public void Sort(String var1) {
    // Collections.sort(this, new DataRowComparer(var1.split(","), true));
    // }
    //
    // public void Sort(String var1, boolean var2) {
    // Collections.sort(this, new DataRowComparer(var1.split(","), var2));
    // }

    public ArrayList<DataRow> toSerializebleArray() {
        return new ArrayList<DataRow>(this);
    }
}
