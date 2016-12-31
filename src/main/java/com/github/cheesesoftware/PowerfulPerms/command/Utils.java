package com.github.cheesesoftware.PowerfulPerms.command;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static Date getDate(String date) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            // Not timestamp format, try parse
            Calendar calendar = Calendar.getInstance();

            String currentNumber = "";
            String currentWord = "";
            char[] chars = date.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                char cur = chars[i];
                if (Character.isDigit(cur)) {
                    if (!currentNumber.isEmpty() && !currentWord.isEmpty()) {

                        if (!finish(calendar, currentNumber, currentWord))
                            return null;
                        currentNumber = "";
                        currentWord = "";
                    }
                    currentNumber += cur;
                } else if (!currentNumber.isEmpty()) {
                    if (!Character.isSpaceChar(cur))
                        currentWord += cur;
                } else
                    return null;
            }

            if (!finish(calendar, currentNumber, currentWord))
                return null;

            calendar.add(Calendar.SECOND, 1);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        }
    }

    private static boolean finish(Calendar calendar, String currentNumber, String currentWord) {
        int number = Integer.parseInt(currentNumber);

        if (currentWord.equals("s") || currentWord.equals("sec") || currentWord.equals("second") || currentWord.equals("seconds"))
            calendar.add(Calendar.SECOND, number);
        else if (currentWord.equals("mi") || currentWord.equals("min") || currentWord.equals("mins") || currentWord.equals("minutes"))
            calendar.add(Calendar.MINUTE, number);
        else if (currentWord.equals("h") || currentWord.equals("hour") || currentWord.equals("hours"))
            calendar.add(Calendar.HOUR, number);
        else if (currentWord.equals("d") || currentWord.equals("day") || currentWord.equals("days"))
            calendar.add(Calendar.DAY_OF_MONTH, number);
        else if (currentWord.equals("w") || currentWord.equals("week") || currentWord.equals("weeks"))
            calendar.add(Calendar.WEEK_OF_MONTH, number);
        else if (currentWord.equals("m") || currentWord.equals("month") || currentWord.equals("months"))
            calendar.add(Calendar.MONTH, number);
        else if (currentWord.equals("y") || currentWord.equals("year") || currentWord.equals("years"))
            calendar.add(Calendar.YEAR, number);
        else
            return false;
        return true;
    }

    public static String getExpirationDateString(Date expires) {
        if (expires == null)
            return null;
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(expires);
    }

    public static boolean isAny(Date expires) {
        return expires != null && expires.getTime() == 0;
    }

    public static Date getAnyDate() {
        return new Date(0);
    }

    public static boolean dateApplies(Date date, Date input) {
        if (date == null && input == null)
            return true;
        else if (Utils.isAny(input))
            return true;
        else if (date == null || input == null)
            return false;
        return date.equals(input);
    }
}
