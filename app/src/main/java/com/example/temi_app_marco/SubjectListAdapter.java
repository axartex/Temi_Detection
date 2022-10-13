package com.example.temi_app_marco;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SubjectListAdapter extends ArrayAdapter<Subject> {

    private List<Subject> items;
    private int layoutResource;
    private Context context;

    public SubjectListAdapter(@NonNull Context context, int resource, @NonNull List<Subject> objects) {
        super(context, resource, objects);
        this.layoutResource = resource;
        this.items = objects;
        this.context = context;
    }

    @SuppressLint("ViewHolder")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row;
        SubHolder holder;

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResource, parent, false);

        holder = new SubHolder();
        holder.subject = items.get(position);
        holder.subjectId = row.findViewById(R.id.subjectIdTextView);
        holder.subjectName = row.findViewById(R.id.subjectNameTextView);
        holder.update = row.findViewById(R.id.updateSubjectButton);
        holder.update.setTag(holder.subject);
        holder.locate = row.findViewById(R.id.locateSubjectButton);
        holder.locate.setTag(holder.subject);
        holder.delete = row.findViewById(R.id.deleteUserButton);
        holder.delete.setTag(holder.subject);

        row.setTag(holder);
        setView(holder);
        return row;
    }

    private void setView(SubHolder subHolder){
        subHolder.subjectId.setText(String.valueOf(subHolder.subject.getId()));
        subHolder.subjectName.setText(subHolder.subject.getName());
    }

    public static class SubHolder{
        Subject subject;
        TextView subjectId;
        TextView subjectName;
        Button delete;
        Button update;
        Button locate;
    }
}
