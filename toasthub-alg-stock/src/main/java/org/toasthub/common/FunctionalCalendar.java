package org.toasthub.common;

import java.util.Calendar;

public class FunctionalCalendar {

    public static Calendar getCalendar(){
        return Calendar.getInstance();
    }

    //returns a calendar set to int minutes minutes in the past
    public static Calendar getPastCalendar(int minutesPast){
        Calendar calendar = getCalendar();
        calendar.add(Calendar.MINUTE, -minutesPast);
        return calendar;
    }

    public static int getCurrentDay(){
        Calendar calendar = getCalendar();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getCurrentYear(){
        Calendar calendar = getCalendar();
        return calendar.get(Calendar.YEAR);
    }

    public static int getCurrentMonth(){
        Calendar calendar = getCalendar();
        return 1 + calendar.get(Calendar.MONTH);
    }

    public static int getCurrentHour(){
        Calendar calendar = getCalendar();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMinute(){
        Calendar calendar = getCalendar();
        return calendar.get(Calendar.MINUTE);
    }
    public static int getPastDay(int minutesPast){
        Calendar calendar = getPastCalendar(minutesPast);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getPastYear(int minutesPast){
        Calendar calendar = getPastCalendar(minutesPast);
        return calendar.get(Calendar.YEAR);
    }

    public static int getPastMonth(int minutesPast){
        Calendar calendar = getPastCalendar(minutesPast);
        return 1 + calendar.get(Calendar.MONTH);
    }

    public static int getPastHour(int minutesPast){
        Calendar calendar = getPastCalendar(minutesPast);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getPastMinute(int minutesPast){
        Calendar calendar = getPastCalendar(minutesPast);
        return calendar.get(Calendar.MINUTE);
    }
}
