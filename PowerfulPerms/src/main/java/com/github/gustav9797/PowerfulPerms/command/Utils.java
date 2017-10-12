package com.github.gustav9797.PowerfulPerms.command;

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
            for (char cur : chars) {
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

        switch (currentWord) {
            case "s":
            case "sec":
            case "second":
            case "seconds":
                calendar.add(Calendar.SECOND, number);
                break;
            case "mi":
            case "min":
            case "mins":
            case "minutes":
                calendar.add(Calendar.MINUTE, number);
                break;
            case "h":
            case "hour":
            case "hours":
                calendar.add(Calendar.HOUR, number);
                break;
            case "d":
            case "day":
            case "days":
                calendar.add(Calendar.DAY_OF_MONTH, number);
                break;
            case "w":
            case "week":
            case "weeks":
                calendar.add(Calendar.WEEK_OF_MONTH, number);
                break;
            case "m":
            case "month":
            case "months":
                calendar.add(Calendar.MONTH, number);
                break;
            case "y":
            case "year":
            case "years":
                calendar.add(Calendar.YEAR, number);
                break;
            default:
                return false;
        }
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
