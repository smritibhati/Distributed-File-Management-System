import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.*;

public class client {

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String serverip = "10.2.137.15";
    public static void main(String[] args) throws IOException {
       
        try {
            Scanner scn = new Scanner(System.in);
            InetAddress ip = InetAddress.getByName(serverip);
			Socket s = new Socket(ip, 5056);

			Thread th = new listenthread(Integer.parseInt(args[0]));
			th.start();
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			dos.writeUTF(args[0]+":" + args[1]);
			
			while (true) {
				// System.out.println("Waiting");
                String msg = dis.readUTF();
                System.out.println(ANSI_GREEN + msg + ANSI_RESET);
                String sendmsg = scn.nextLine();

                if (sendmsg.equals("Exit")) {
                    dos.writeUTF(sendmsg);
                    s.close();
                    System.out.println("Connection closed");
                    break;
                }
                String[] split = sendmsg.split(" ");

                switch (split[0]) {
                    case "upload":
                        File f = new File(split[1]);
                        sendmsg = sendmsg + " " + Long.toString(f.length());
                        dos.writeUTF(sendmsg);
                        msg = dis.readUTF();
                        System.out.println(msg);
                        if (msg.equals("User empty")) {
                            break;
                        }
                        System.out.println("\nUploading " + split[1] + "...");
                        FileInputStream fis = new FileInputStream(split[1]);
                        byte[] buffer = new byte[4096];
                        int x;
                        while ((x = fis.read(buffer)) != -1) {
//							dos.write(buffer);
                            dos.write(buffer, 0, x);
                            dos.flush();
                        }
                        dos.flush();
                        fis.close();

                        System.out.println("Upload complete.");
						break;
						case "upload_udp":
							File f1 = new File(split[1]);
							sendmsg = sendmsg + " " + Long.toString(f1.length());
							dos.writeUTF(sendmsg);
							msg = dis.readUTF();
							System.out.println(msg);
							if (msg.equals("User empty")) {
								break;
							}
							System.out.println("\nUploading " + split[1] + "...");
							DatagramSocket dsoc = new DatagramSocket(10010,InetAddress.getByName(serverip));
                            File f2 = new File(split[1]);
                            FileInputStream fis_ = new FileInputStream(f2);
                            byte[] buf = new byte[1024];

                            DatagramPacket dp = new DatagramPacket(buf, buf.length,InetAddress.getByName(serverip),40010);
                            int len;
                            while((len=fis_.read(buf))!=-1){
                                // dp=new DatagramPacket(buf, buf.length,InetAddress.getByName("localhost"),40010);
                                dp.setLength(len);
                                dsoc.send(dp);
                            }
                            buf = "end".getBytes();
                            DatagramPacket edp = new DatagramPacket(buf, buf.length,InetAddress.getByName(serverip),40010);
                            dsoc.send(edp);
                            dsoc.close();
                            fis_.close();
							System.out.println("Upload complete.");
                
                        break;
					case "get_file":
                        dos.writeUTF(sendmsg);
                        dos.flush();
                        msg = dis.readUTF();
                        if(msg.equals("File not found.")){
                            System.out.println("File not found on server.");
                        }
                        else{
                            split = msg.split(" ");
                            System.out.println("Downloading " + split[0] + "...");
                            String filename = split[0] + "-dwnld";
                            FileOutputStream fos = new FileOutputStream(filename);
                            byte[] buff = new byte[4096];
                            int y;
                            int rem = Integer.parseInt(split[1]);
                            while (rem > 0 && (y = dis.read(buff, 0, Math.min(buff.length, rem))) > 0) {
                                rem -= y;
                                fos.write(buff, 0, y);
                                fos.flush();
                            }
                            fos.close();
                            System.out.println("File " + filename + " has been saved.");      
                        }
                        break;
                    case "list_groups":
                        dos.writeUTF(sendmsg);
                        msg = dis.readUTF();
                        split = msg.split("\\*");
                        for (String group : split) {
                            System.out.println(group);
                        }
                        break;
                    case "list_detail":
                        // System.out.println(sendmsg);
                        dos.writeUTF(sendmsg);
                        msg = dis.readUTF();
                        // System.out.println(msg);
                        split = msg.split("#");
                        System.out.println("\n------------------------USERS-------------------------");
                        String[] user = split[0].split("\\*");
                        for (String u : user) {
                            System.out.println(u);
                        }
                        System.out.println("\n------------------------FILES-------------------------");
                        String[] group = split[1].split("\\*");
                        for (String g : group) {
                            System.out.println(g);
                        }
                        System.out.println();
                        break;

                    default:
                        dos.writeUTF(sendmsg);
                        msg = dis.readUTF();
                        System.out.println(ANSI_RED + msg + ANSI_RESET);
                }
            }

            // closing resources 
            scn.close();
            dis.close();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
            // s.close();
        }
    }
}
class listenthread extends Thread {
	ServerSocket ss;
	Socket s;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
	public listenthread(int port) throws IOException {
		this.ss= new ServerSocket(port);
		this.s =null;
	}

	public void run() {
        String msg;
        String[] splitmsg;
		while(true){
			Socket s = null;
			try {
				s = ss.accept();
				DataInputStream dis = new DataInputStream(s.getInputStream());
                msg = dis.readUTF();
                splitmsg = msg.split("@");

				System.out.println(ANSI_BLUE + "Message from " + splitmsg[0] + ": " + splitmsg[1] + ANSI_RESET);
				dis.close();
				s.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
	}
}