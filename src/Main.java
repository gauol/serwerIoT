import sun.rmi.runtime.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Created by Gaua on 11.04.2017.
 */
public class Main extends JFrame implements ActionListener{

    private static JTextArea LogTextField;
    private JCheckBox SaveCheckBox;

    private final static String addStr = "Dodaj Sensor";
    private final static String delStr = "Usuń Sensor";
    private final static String openWebStr = "Otwórz przeglądarkę";
    public static void main(String args[]) throws IOException {
        Server.print("To jest pierwsza linia programu \r\n to jest druga linia");
        Server.mn = new Main();
    }

    public Main(){
        super("IoT Serwer");
        setSize(400,250);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(null);

        JButton AddButton = new JButton(addStr);
        AddButton.setBounds(220,10,150,50);
        AddButton.addActionListener(this);
        add(AddButton);

        JButton DeleteButton = new JButton(delStr);
        DeleteButton.setBounds(220,100,150,50);
        DeleteButton.addActionListener(this);
        add(DeleteButton);

        JButton OpenWebButton = new JButton(openWebStr);
        OpenWebButton.setBounds(10,100,150,50);
        OpenWebButton.addActionListener(this);
        add(OpenWebButton);

        JButton LogButton = new JButton("Pokaż Log");
        LogButton.setBounds(10,10,150,50);
        LogButton.addActionListener(this);
        add(LogButton);

        SaveCheckBox = new JCheckBox("Zapisac zmiany w bazie po zamknięciu?");
        SaveCheckBox.setBounds(10, 160, 400,50);
        add(SaveCheckBox);

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(Server.mn,
                        "Czy jesteś pewny że chcesz wyjść z programu?", "Na pewno?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                    if (SaveCheckBox.isSelected())
                        Server.jdb.closeStatements();
                        Server.jdb.closeDatabase();
                    System.exit(0);
                }
            }
        });

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e){
        String name = e.getActionCommand();
        Server.print(e.getActionCommand());
        switch (name) {
            case addStr:
                if (LogTextField != null)
                    LogTextField.setText(new Czas().getTime() + "\t Wcisnieto "+addStr+"\n\r" + LogTextField.getText());
                createAddOrDeleteWindow(name);
                break;
            case delStr:
                if (LogTextField != null)
                    LogTextField.setText(new Czas().getTime() + "\t Wcisnieto " + delStr + "\n\r" + LogTextField.getText());
                createAddOrDeleteWindow(name);
                break;
            case "Pokaż Log":
                if (LogTextField != null)
                    LogTextField.setText(new Czas().getTime() + "\t Pokaż Log\n\r" + LogTextField.getText());
                createLogWindow();
                break;
            case openWebStr:
                if(Desktop.isDesktopSupported())
                {
                    try {
                        Desktop.getDesktop().browse(new URI("http://localhost:90"));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
        }

    }

    public static void createLogWindow(){
        JFrame frame = new JFrame ("Log");
        frame.setSize(500,500);
        frame.setResizable(true);
        LogTextField = new JTextArea();
        LogTextField.setSize(400,400);
        LogTextField.setLineWrap(true);
        LogTextField.setEditable(false);
        LogTextField.setVisible(true);
        Server.LogTextField = LogTextField;
        JScrollPane scroll = new JScrollPane (LogTextField);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scroll);
        Point spawnPoint = Server.mn.getLocation();
        spawnPoint.setLocation(spawnPoint.getX()+400,spawnPoint.getY());
        frame.setLocation(spawnPoint);
        frame.setVisible(true);
    }

    public static void createAddOrDeleteWindow(String str){
        switch (str){
            case addStr :
                String input = JOptionPane.showInputDialog(new JFrame(str),"Podaj nazwę sensora");
                if (!input.equals("")){
                    Server.jdb.createTable(input);
                    Server.print("Dodano sensor : " + input);
                }else
                    infoBox("Nie podałeś nazwy sensora!", "Error!!!");

                break;
            case delStr :
                Server.tabele = Server.jdb.listTablesToArray();
                String[] possibilities = Server.tabele.toArray(new String[Server.tabele.size()]);
                String s = (String)JOptionPane.showInputDialog(
                        Server.mn,
                        "Wybierz sensor który chcesz usunąć",
                        "Usuń sensor",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        possibilities,
                        possibilities[0]);
                Server.print("Usuwam : " + s);
                break;
        }
    }

    public static void infoBox(String infoMessage, String titleBar)
    {
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }
}
