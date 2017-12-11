import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;

/**
 * Created by Gaua on 2016-09-13.
 */

public class Czas {

    private int h;
    private int m;
    private int s;
    private int ms;
    private int mon;
    private int day;
    private int year;

    public Czas(){
        Calendar cal = Calendar.getInstance();
        h = cal.get(Calendar.HOUR_OF_DAY);
        m = cal.get(Calendar.MINUTE);
        s = cal.get(Calendar.SECOND);
        ms = cal.get(Calendar.MILLISECOND);
        mon = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        year = cal.get(Calendar.YEAR);

    }

    public Czas(int hour, int minutes, int seconds, int miliseconds){
        h = hour;
        m = minutes;
        s = seconds;
        ms = miliseconds;
    }

    public static String liczRoznice(Czas pierwszy, Czas drugi) throws ParseException {
        String roznica;
       // java.text.DateFormat df = new java.text.SimpleDateFormat("hh:mm:ss");
        java.text.DateFormat df = DateFormat.getTimeInstance();
        java.util.Date date1 = df.parse(pierwszy.h+":"+pierwszy.m+":"+pierwszy.s);
        java.util.Date date2 = df.parse(drugi.h+":"+drugi.m+":"+drugi.s);
        long diff = date2.getTime() - date1.getTime();
        diff/=1000;
        long h= diff/3600;
        long m= diff/60 -h;
        long s= diff-h*3600-m*60;
       roznica = String.valueOf(h+":"+m+":"+s);
        return roznica;
    }

    public int getH() {
        return h;
    }

    public int getM() {
        return m;
    }

    public int getS() {
        return s;
    }

    public int getMs() {
        return ms;
    }

    public int getMonth(){return mon;}

    public int getDzien(){return day;}

    public int getYear(){return  year;}

    public int getMsTime(){
        return h*3600000+m*60000+s*1000+ms;
    }
     public String getTime(){
         String temp1;
         if (h<10){temp1="0";}else{temp1="";}
         String temp2;
         if (m<10){temp2="0";}else{temp2="";}
         String temp3;
         if (s<10){temp3="0";}else{temp3="";}
         return temp1+h + ":" + temp2+m + ":" + temp3+s;// + "'" + ms;
     }
}