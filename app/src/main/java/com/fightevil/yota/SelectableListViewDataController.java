package com.fightevil.yota;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SelectableListViewDataController {
    private List<Model> values;
    private ArrayAdapter<Model> adapter;
    private ListView listView;

    private static List<Model> CreateModelList(String[] textValues){
        List<Model> values = new ArrayList<Model>();
        for (int i = 0; i < textValues.length; ++i)
            values.add(new Model(textValues[i]));
        return values;
    }

    public SelectableListViewDataController(ListView listView){
        this.listView = listView;
    }

    public void SetTextData(String[] textValues){
        this.values = CreateModelList(textValues);
        this.adapter = new MyArrayAdapter(listView.getContext(), values);
        this.listView.setAdapter(this.adapter);
    }

    private void SetValuesTofDefault(){
        for (int i = 0; i < values.size(); ++i)
            values.get(i).setSelected(false);
    }

    public void SelectItem(int position){
        SetValuesTofDefault();
        values.get(position).setSelected(true);
        adapter.notifyDataSetChanged();
    }

    public int GetSelectedItemNum(){
        if (values == null)
            return -1;
        for (int i = 0; i < values.size(); ++i)
            if (values.get(i).isSelected())
                return i;
        return -1;
    }

    public boolean IsSelected(int position){
        return values.get(position).isSelected();
    }

    public void DeselectAll(){
        SetValuesTofDefault();
        adapter.notifyDataSetChanged();
    }

    static public class Model {
        private String name;
        private boolean selected;

        public Model(String name) {
            this.name = name;
            selected = false;
        }

        public Model(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    static public class MyArrayAdapter extends ArrayAdapter<Model> {
        private final Context context;
        private final List<Model> values;

        public MyArrayAdapter(Context context, List<Model> values) {
            super(context, R.layout.list_item, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_item, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.list_view_item_text);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.list_view_item_check);
            textView.setText(values.get(position).getName());
            checkBox.setChecked(values.get(position).isSelected());
            return rowView;
        }
    }
}
