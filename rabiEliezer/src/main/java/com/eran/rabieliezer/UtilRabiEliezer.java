package com.eran.rabieliezer;

import java.util.ArrayList;

public class UtilRabiEliezer {

    public static String[] pereks = {
            "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
            "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ",
            "כא", "כב", "כג", "כד", "כה", "כו", "כז", "כח", "כט", "ל",
            "לא", "לב", "לג", "לד", "לה", "לו", "לז", "לח", "לט", "מ",
            "מא", "מב", "מג", "מד", "מה", "מו", "מז", "מח", "מט", "נ",
            "נא", "נב", "נג", "נד"
    };

    public static ArrayList<Perek> buildPerekArr() {
        Perek perek;
        ArrayList<Perek> arrListPerek = new ArrayList<Perek>();
        Perek menu = new Perek(-1, "תפריט");
        arrListPerek.add(menu);
        for (int i = 0; i < pereks.length; i++) {
            perek = new Perek(i, "פרק " + pereks[i]);
            arrListPerek.add(perek);
        }
        return arrListPerek;
    }

}
