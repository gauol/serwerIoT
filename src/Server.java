import javax.swing.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static int printetChart = 0;
    public static JDB jdb;
    public static List<String> tabele;
    private static int port = 90;
    public static JTextArea LogTextField;
    public static Main mn;

    public static void main(String[] args) {

        mn = new Main();
        Main.createLogWindow();

        go();
    }

     Server(int port){
        Server.port = port;
        go();
     }
     Server(int port, JTextArea jta){
        Server.port = port;
        LogTextField = jta;
        go();
    }

    public static void go(){
        try {
            jdb = new JDB();
            runSerwer();
        }catch (Exception sql){
            Server.print("FATAL ERROR");
            Server.print(sql.toString());
        }
    }

    public static void getHtmlBuffer(String path, SocketChannel dos) throws IOException {
        tabele = jdb.listTablesToArray();
        print("Zapytanie : " + path);
        if (path.equals("/"))
            path = "/index.html";
        if(path.charAt(1)=='?') {
            try{
                printetChart = Integer.parseInt(path.replaceAll("[\\D]", ""));
            }catch (NumberFormatException exc){
                printetChart = 0;
            }
            path = "/index.html";
        }

        try{
            Path pathF = Paths.get(System.getProperty("user.dir")+"/web" + path);
            byte bytes[] = Files.readAllBytes(pathF);
            String response = new String(bytes, "UTF-8");

            if(path.equals("/index.html")){ //wklejam dane z bazy
                String nazwaWykresu;
                int index;
                if(printetChart == 0){
                    nazwaWykresu = "Wybierz wykres z menu";
                    index = response.indexOf("<!-- TuWklejPomiary -->"); //     ",\r\n['" + data + ", " + temp + "]"
                    String smile = ",['*', 15, 0],\n" +
                            "['*', 30, -5],\n" +
                            "['*', 15, -8],\n" +
                            "['*', 13, -10],\n"+
                            "['*', 15, -8],\n" +
                            "['*', 30, -5],\n" +
                            "['*', 15, -0]\n";
                    response = new StringBuilder(response).insert(index, smile).toString();
                }else {
                    nazwaWykresu = "Wykres : " + tabele.get(printetChart - 1);
                    index = response.indexOf("<!-- TuWklejPomiary -->"); //     ",\r\n['" + data + ", " + temp + "]"
                    String daneZBazy = "";
                    try {
                        daneZBazy = jdb.getDataHTML(tabele.get(printetChart - 1));
                    }catch (IndexOutOfBoundsException ignored){
                    }finally {
                        if (daneZBazy.equals(""))
                            daneZBazy = ",['*', 15, 0],\n" + "['*', 0, 15],\n";
                        response = new StringBuilder(response).insert(index, daneZBazy).toString();
                    }
                }
                index = response.indexOf("<!-- TuWklejSensory -->");
                response = new StringBuilder(response).insert(index, getTablesListHTTP()).toString();
                index = response.indexOf("<!-- TuWklejNazweWykresu -->");
                response = new StringBuilder(response).insert(index, nazwaWykresu).toString();
            }
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            dos.write(encoder.encode(CharBuffer.wrap("HTTP/1.1 200 OK\r\nContent-length="+response.length())));
            if(path.equals("/index.html"))
                dos.write(encoder.encode(CharBuffer.wrap("\r\nContent-type:text/html; charset=UTF-8\r\n\r\n")));
            else
                dos.write(encoder.encode(CharBuffer.wrap("\r\n\r\n")));

            dos.write(encoder.encode(CharBuffer.wrap(response)));
        }
        catch (InvalidPathException | IOException error){
            print(error.toString());
            String str = "HTTP/1.1 200 OK\r\n" +
                    "\r\n <h1>404</h1>";
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            dos.write(encoder.encode(CharBuffer.wrap(str)));
        }finally {
            dos.close();
            print("\tZamykam połączenie");
        }
    }

    private static void runSerwer() {
        try {
            SocketAddress localport = new InetSocketAddress(port);
            ServerSocketChannel tcpserver = ServerSocketChannel.open();
            tcpserver.socket().bind(new InetSocketAddress(port));
            DatagramChannel udpserver = DatagramChannel.open();

            udpserver.socket().bind(localport);
            ByteBuffer bufferTCP = ByteBuffer.allocate(1024);

            tcpserver.configureBlocking(false);
            udpserver.configureBlocking(false);

            Selector selector = Selector.open();

            tcpserver.register(selector, SelectionKey.OP_ACCEPT);
            udpserver.register(selector, SelectionKey.OP_READ);

            ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
            print("http://localhost:" + port);
            for (; ; )
                try {
                    selector.select();
                    Set keys = selector.selectedKeys();
                    for (Iterator i = keys.iterator(); i.hasNext(); ) {
                        SelectionKey key = (SelectionKey) i.next();
                        Channel c = key.channel();

                        if (key.isAcceptable() && c == tcpserver) {
                            SocketChannel client = tcpserver.accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                        }
                        else if (key.isReadable() && c == udpserver) {
                            SocketAddress clientAddress = udpserver.receive(receiveBuffer);
                            receiveBuffer.position(0);
                            interpreterUDP(clientAddress.toString(), new String( receiveBuffer.array(),"UTF-8"));
                        }else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            client.read(bufferTCP);
                            String output = new String(bufferTCP.array()).trim();
                            bufferTCP.clear();
                            if (client != null) {
                                String httpqr = getHTTPquery(output);
                                Server.getHtmlBuffer(httpqr, client);
                            }
                        }
                        i.remove();
                    }
                } catch (java.io.IOException e) {

                    print("IOException in Server "+ e);
                } catch (Throwable t) {
                    print("FATAL error in Server "+ t);
                    System.exit(1);
                }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    static void printRln(String msg){
        String ANSI_RED = "\u001B[31m";
        String ANSI_RESET = "\u001B[0m";
        Server.print(ANSI_RED + msg + ANSI_RESET);
    }

    private static String getHTTPquery(String daneIN){
        String strTrim = daneIN.substring(daneIN.indexOf("GET")+4, daneIN.length());
        String astrTrim = strTrim.substring(0, strTrim.indexOf(" "));
        //print(astrTrim);
        return astrTrim;
    }
    private static String getTablesListHTTP(){
        StringBuilder listaTabelHTML = new StringBuilder();
        int ctr = 1;
        for(String str : tabele ) {
            String sensorName = ctr + ". " + str;
            listaTabelHTML.append("<a class=\"w3-bar-item w3-button\" href=\"?id=").append(ctr).append("\">").append(sensorName).append("</a>\r\n");
            ctr++;
        }
            return listaTabelHTML.toString();
    }

    private static void interpreterUDP(String from, String data){
        Pattern p = Pattern.compile("(SEN=)(\\D+):(TEMP1=)(.?\\d+.\\d+):(TEMP2=)(.?\\d+.\\d+)");
        Matcher m = p.matcher(data);
        if(m.find()) {
            String id = m.group(2);
            String temp1 = m.group(4);
            String temp2 = m.group(6);
            print("Odebrano Dane : SEN = "+id+" : " + temp1 +" : "+ temp2);
            jdb.addData(id, Float.parseFloat(temp1),Float.parseFloat(temp2));
        }
        else
            printRln("Error Interpretacji" + data);
    }

    public static void print(String str){
        System.out.println(str);
        if(LogTextField!=null)
            LogTextField.setText(new Czas().getTime()+"\t"+ str +"\r\n"+LogTextField.getText());
    }
}
