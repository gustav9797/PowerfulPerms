package com.github.cheesesoftware.PowerfulPerms.command;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

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
                        int number = Integer.parseInt(currentNumber);

                        if (currentWord.equals("mi") || currentWord.equals("min") || currentWord.equals("mins") || currentWord.equals("minutes"))
                            calendar.add(Calendar.MINUTE, number);
                        else if (currentWord.equals("h") || currentWord.equals("hour") || currentWord.equals("hours"))
                            calendar.add(Calendar.HOUR, number);
                        else if (currentWord.equals("d") || currentWord.equals("day") || currentWord.equals("days"))
                            calendar.add(Calendar.DAY_OF_MONTH, number);
                        else if (currentWord.equals("m") || currentWord.equals("month") || currentWord.equals("months"))
                            calendar.add(Calendar.MONTH, number);
                        else if (currentWord.equals("y") || currentWord.equals("year") || currentWord.equals("years"))
                            calendar.add(Calendar.YEAR, number);
                        else
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
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        }
    }
}
