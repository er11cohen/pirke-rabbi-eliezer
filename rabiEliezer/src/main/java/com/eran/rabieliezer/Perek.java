package com.eran.rabieliezer;

import android.os.Parcel;
import android.os.Parcelable;

public class Perek implements Parcelable {//more efficient than Serializable 
    private int perekIndex;
    private String perekName;

    private String time;
    private int scrollY;

    //for keyPerekActivity
    public Perek(int perekIndex, String perekName) {
        this.perekIndex = perekIndex;
        this.perekName = perekName;
    }

    //for locations
    public Perek(String time, int scrollY, int perekIndex, String perekName) {
        this.perekIndex = perekIndex;
        this.perekName = perekName;
        this.time = time;
        this.scrollY = scrollY;
    }

    @Override
    public String toString() {
        return getPerekName(); //what you want displayed for each row in the listview
    }

    public int getPerekIndex() {
        return perekIndex;
    }

    public void setPerekIndex(int perekIndex) {
        this.perekIndex = perekIndex;
    }

    public String getPerekName() {
        return perekName;
    }

    public void setperekName(String perekName) {
        this.perekName = perekName;
    }

    public String getTime() {
        return time;
    }

    public void setYime(String time) {
        this.time = time;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }


    protected Perek(Parcel in) {
        perekIndex = in.readInt();
        perekName = in.readString();
        time = in.readString();
        scrollY = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(perekIndex);
        dest.writeString(perekName);
        dest.writeString(time);
        dest.writeInt(scrollY);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Perek> CREATOR = new Parcelable.Creator<Perek>() {
        @Override
        public Perek createFromParcel(Parcel in) {
            return new Perek(in);
        }

        @Override
        public Perek[] newArray(int size) {
            return new Perek[size];
        }
    };
}