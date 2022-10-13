package com.example.temi_app_marco;

public class Subject {
    private String name;
    private boolean gender; //true if male, false if female (only for voice pitch purpose)
    private String userId = null;
    private final int Id;
    private String ImageKey = null;

    public Subject(String fName, String Gender, int id){
        this.name = fName;
        this.Id = id;
        this.gender = Gender.equals("Male");
    }

    public String getName() {
        return name;
    }

    public boolean isGender() {
        return gender;
    }

    public int getId(){
        return Id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean equals(int id){
        return this.Id == id;
    }

    public void editName(String subjectFName) {
        this.name = subjectFName;
    }

    public void editGender(String subjectGender) {
        this.gender = subjectGender.equals("Male");
    }

    public String getImageKey() {
        return ImageKey;
    }

    public void setImageKey(String imageKey) {
        ImageKey = imageKey;
    }
}
