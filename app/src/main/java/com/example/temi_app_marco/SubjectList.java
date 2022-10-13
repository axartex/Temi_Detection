package com.example.temi_app_marco;

import java.util.ArrayList;
import java.util.List;

public class SubjectList {
    private int size;
    private int currId;
    private List<Subject> items;
    private static SubjectList subjectList;

    private SubjectList(){
        this.size = 0;
        this.currId = 0;
        this.items = new ArrayList<Subject>();
    }

    public static SubjectList get(){
        if (subjectList == null){
            subjectList = new SubjectList();
        }
        return subjectList;
    }

    public Subject addSubject(String Name, String Gender){
        Subject subject = new Subject(Name, Gender, currId);
        items.add(subject);
        currId++;
        size++;
        return subject;
    }

    public int getSize(){
        return size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public void removeSubject(Subject sub){
        items.remove(sub);
        size--;
    }

    public void removeAllSubjects(){
        items.clear();
        size = 0;
    }

    public Subject searchId(int curId) {
        for( Subject subject : subjectList.items){
            if(subject.equals(curId)){
                return subject;
            }
        }
        return null;
    }

    public Subject searchName(String name){
        String currName;
        for( Subject subject : subjectList.items){
            currName = subject.getName();
            if (name.equals(currName)){
                return subject;
            }
        }
        return null;
    }

    public Subject searchUserId(String userId){
        String currUserId;
        for (Subject subject : subjectList.items){
            currUserId = subject.getUserId();
            if (userId.equals(currUserId)){
                return subject;
            }
        }
        return null;
    }

    public List<Subject> getItems(){
        return this.items;
    }
}
