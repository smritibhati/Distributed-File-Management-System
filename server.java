import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.*;

public class server {

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(5056);
        Map<String, String> filemeta = new HashMap<>();
        Map<String, ArrayList<String>> groupmeta = new HashMap<>();
        Map<String, Socket> usermeta = new HashMap<>();
        Map<String, Integer> userport = new HashMap<>();
        Map<String, String> userip = new HashMap<>();
        // public static final String ANSI_RESET = "\u001B[0m";

        while (true) {
            Socket sock = null;

            try {
                sock = ss.accept();

                System.out.println("A new client is connected : " + sock);

                DataInputStream dis = new DataInputStream(sock.getInputStream());
                DataOutputStream dos = new DataOutputStream(sock.getOutputStream());

                System.out.println("Assigning new thread for this client");

                Thread th = new clientthread(sock, dis, dos, filemeta, groupmeta, usermeta,userport,userip);

                th.start();

            } catch (Exception e) {
                sock.close();
                e.printStackTrace();
            }
        }
    }
}

// ClientHandler class 
class clientthread extends Thread {

    DataInputStream dis;
    DataOutputStream dos;
    Socket sock;
    Map<String, String> filemeta;
    Map<String, ArrayList<String>> groupmeta;
    Map<String, Socket> usermeta;
    Map<String, Integer> userport;
    Map<String, String> userip;

    public clientthread(Socket sock, DataInputStream dis, DataOutputStream dos, Map<String, String> filemeta, Map<String, ArrayList<String>> groupmeta, Map<String, Socket> usermeta,Map<String, Integer> userport,Map<String, String> userip) {
        this.sock = sock;
        this.dis = dis;
        this.dos = dos;
        this.filemeta = filemeta; // file-author
        this.groupmeta = groupmeta; // group-list of users
        this.usermeta = usermeta; // usernames - address
        this.userport = userport;
        this.userip = userip;
    }

    public void run() {
        String msg;
        String sendmsg;
        String newuser = "";
        int newport=0;
        String newip="";
        try{
            
            msg = dis.readUTF();
            String[] s = msg.split(":");
            newport = Integer.parseInt(s[0]);
            newip = s[1];
        }
        catch(IOException e){
            e.printStackTrace();
        }
        while (true) {
            try {
                dos.writeUTF("Enter your command");
                // System.out.println("Server rcvd: ");
                msg = dis.readUTF();

                System.out.println(msg);
                if (msg.equals("Exit")) {
                    this.sock.close();
                    System.out.println("Client left");
                    this.dis.close();
                    this.dos.close();
                    break;

                }
                String[] splitmsg = msg.split(" ");

                switch (splitmsg[0]) 
                {

                    case "create_user":
                        newuser = splitmsg[1];
                        if(usermeta.containsKey(newuser))
                            dos.writeUTF("Username already exists, try again");
                        else{
                            sendmsg = "New user " + newuser + " created.";
                            dos.writeUTF(sendmsg);
                            usermeta.put(newuser, sock);
                            userport.put(newuser,newport);
                            userip.put(newuser,newip);
                        }
                        break;

                    case "upload":
                        if (newuser.equals("")) {
                            dos.writeUTF("User empty");
                        } else {
                            dos.writeUTF("Permission granted");
                            dos.flush();
                            System.out.println("\n" + newuser + " is uploading " + splitmsg[1]);
                            
                            String filename = splitmsg[1]+"@" + newuser;
                            
                            FileOutputStream fos = new FileOutputStream(filename);
                            byte[] buffer = new byte[4096];
                            int x;
                            int left = Integer.parseInt(splitmsg[2]);
                            while (left > 0 && (x = dis.read(buffer, 0, Math.min(buffer.length, left))) > 0) {
                                left -= x;
                                fos.write(buffer, 0, x);
                                fos.flush();
                            }
                            fos.close();
                            System.out.println("File " + filename + " has been saved.");
                            filemeta.put(filename, newuser);
                        }
                        break;
                    
                    case "upload_udp":
                        if (newuser.equals("")) {
                            dos.writeUTF("User empty");
                        } else {
                            dos.writeUTF("Permission granted");
                            dos.flush();
                            System.out.println("\n" + newuser + " is uploading " + splitmsg[1]);
                            String filename_ = splitmsg[1]+"@" + newuser;
                            
                            byte buff_[]=new byte[4096];
                            DatagramSocket dgsock = new DatagramSocket(40010);
                            File fileudp = new File(filename_);
                            FileOutputStream fos = new FileOutputStream(fileudp);
                            DatagramPacket dp = new DatagramPacket(buff_, buff_.length);
                            int left_ = Integer.parseInt(splitmsg[2]);
                            while(true)
                            {
                                dgsock.receive(dp);
                                if (new String(dp.getData(), 0, dp.getLength()).equals("end")) 
                                { 
                                    System.out.println("Documents received");
                                    fos.close();
                                    dgsock.close();
                                    break;
                                }
                                fos.write(dp.getData(), 0, Math.min(left_,dp.getLength()));
                                fos.flush(); 
                                left_ = left_ - dp.getLength();
                                
                            }
                            fos.close();
                            dgsock.close();
                            System.out.println("File " + filename_ + " has been saved.");
                            filemeta.put(filename_, newuser);
                        }
                        break;

                    case "create_folder":
                        new File(splitmsg[1]).mkdirs();
                        dos.writeUTF("\nFolder created successfully");
                        break;

                    case "move_file":
                        File myfile = new File(splitmsg[1]);
                        String[] oldname = splitmsg[1].split("/");
                       
                        if(myfile.renameTo(new File(splitmsg[2]+"/"+ oldname[oldname.length-1]))){
                            dos.writeUTF("\nFile moved successfully.");
                        }
                        else{
                            dos.writeUTF("\nCommand failed.");
                        }
                        // // System.out.println("Moving from "++ " to " + splitmsg[2]);
                        // // Path temp = Files.move(Paths.get(splitmsg[1]), Paths.get(splitmsg[2]));
                        // if (temp == null) {
                            
                        // } else {
                            
                        // }
                        break;

                    case "create_group":
                        if (groupmeta.containsKey(splitmsg[1])) {
                            dos.writeUTF("\nGroup already exists");
                        } else {
                            groupmeta.put(splitmsg[1], new ArrayList<String>());
                            dos.writeUTF("\nGroup " + splitmsg[1] + " created");
                        }
                        break;

                    case "list_groups":
                        Set<String> keys = groupmeta.keySet();
                        sendmsg = "";
                        for (String key : keys) {
                            sendmsg += key + "*";
                        }
                        dos.writeUTF(sendmsg);
                        break;

                    case "join_group":
                        if (newuser.equals("")) {
                            dos.writeUTF("\nUser empty");
                        } else if (!groupmeta.containsKey(splitmsg[1])) {
                            dos.writeUTF("\nNo such group exists.");
                        } else {
                            ArrayList<String> newl = groupmeta.get(splitmsg[1]);
                            newl.add(newuser);
                            groupmeta.put(splitmsg[1], newl);
                            dos.writeUTF("Joined successfully");
                        }
                        break;

                    case "leave_group":
                        if (newuser.equals("")) {
                            dos.writeUTF("\nUser empty");
                        } else if (!groupmeta.containsKey(splitmsg[1])) {
                            dos.writeUTF("\nNo such group exists.");
                        } else {
                            ArrayList<String> newl = groupmeta.get(splitmsg[1]);
                            newl.remove(newuser);
                            groupmeta.put(splitmsg[1], newl);
                            dos.writeUTF("Left successfully");
                        }
                        break;
                    
                    case "list_detail":
                        if (!groupmeta.containsKey(splitmsg[1])) {
                            dos.writeUTF("\nNo such group exists.");
                        } else {
                            ArrayList<String> newl = groupmeta.get(splitmsg[1]);
                            sendmsg = "";
                            //users separated by *
                            for (String u : newl) {
                                sendmsg += u + "*";
                            }
                            if (sendmsg.equals("")) {
                                sendmsg = " ";
                            }
                            sendmsg += "#";
                            //users and groups separated by #
                            Set<String> filenames = filemeta.keySet();
                            if (filenames.size() == 0) {
                                sendmsg += " ";
                            }
                            //filenames separated by *
                            for (String f : filenames) {
                                if (newl.contains(filemeta.get(f))) {
                                    sendmsg += f + "*";
                                }
                            }
                            dos.writeUTF(sendmsg);
                        }
                        break;
                    case "share_msg":
                        if (newuser.equals("")) {
                            dos.writeUTF("User Empty");
                        } else {
                            ArrayList<Integer> finalport = new ArrayList();
                            ArrayList<String> finalip = new ArrayList();
                            ArrayList<String> mem = groupmeta.get(splitmsg[1]);
                            int port;
                            for (String u:mem){
                                if(!u.equals(newuser)){
                                    finalport.add(userport.get(u));
                                    finalip.add(userip.get(u));
                                }
                            }
                            String sharemsg = "";
                            for(int k=2;k<splitmsg.length;k++)
                                sharemsg = sharemsg + splitmsg[k]+ " ";
                            sharemsg = newuser + "@" + sharemsg;
                            InetAddress ip;
                            for (int i=0;i<finalip.size();i++){
                                ip=InetAddress.getByName(finalip.get(i));
                                port= finalport.get(i);
                                Socket stemp = new Socket(ip, port);
                                DataOutputStream dos2 = new DataOutputStream(stemp.getOutputStream());
                                dos2.writeUTF(sharemsg);
                                dos2.flush();
                                dos2.close();
                                stemp.close();
                            }
                            dos.writeUTF("Message shared successfully");
                        }
                        break;
                        case "get_file":
                            String path = System.getProperty("user.dir");
                            String[] split2 = splitmsg[1].split("/");
                            String fname = path + "/" + split2[splitmsg.length-1] + "@" + split2[splitmsg.length-2];
                            System.out.println("Request for "+fname);
                            if(!filemeta.containsKey(fname)){
                                dos.writeUTF("File not found.");
                                dos.flush();
                            }
                            else{
                                File f = new File(fname);
                                sendmsg = fname + " " + Long.toString(f.length());
                                dos.writeUTF(sendmsg);
                                dos.flush();
                                FileInputStream fis_ = new FileInputStream(fname);
                                byte[] buffer_ = new byte[4096];
                                int x_;
                                while ((x_ = fis_.read(buffer_)) != -1) {
                                    dos.write(buffer_, 0, x_);
                                    dos.flush();
                                }
                                dos.flush();
                                fis_.close();
                                System.out.println("SEND COMPLETE.");
                            }
                            break;
                    default:
                        dos.writeUTF("Invalid input");
                        break;
                }
            }catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            // closing resources 
            this.dis.close();
            this.dos.close();


        } catch (Exception e) {
            e.printStackTrace();
           
        }
    }
}
