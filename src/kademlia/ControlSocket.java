/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aidan
 */
public class ControlSocket {
    private final ServerSocket server;
    // Syntax is [int number of parameters, [int length of each parameter], protocol, id, [parameters]]
    ControlSocket(int port, Kademlia kad) throws IOException {
        server = new ServerSocket(port);
    }
    public void run() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket socket = server.accept();
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        Kademlia.threadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (true) {
                                        int paramaters = in.readInt();
                                        int[] lengthArray = new int[paramaters];
                                        for (int i = 0; i < paramaters; i++) {
                                            lengthArray[i] = in.readInt();
                                        }
                                        byte[][] paramaterArray = new byte[paramaters][];
                                        int protocol = in.readInt();
                                        int id = in.readInt();
                                        for (int i = 0; i < lengthArray.length; i++) {
                                            byte[] data = new byte[lengthArray[i]];
                                            in.readFully(data);
                                            /*for (int j = 0; j < lengthArray[i]; j++) {
                                             data[j] = in.readByte();
                                             }*/
                                            paramaterArray[i] = data;
                                        }
                                        switch (protocol) {
                                            case 0:
                                                System.out.println("CONNECT");
                                                break;
                                            case 1:
                                                System.out.println("GET");
                                                String result = "result";
                                                out.writeInt(id);
                                                out.writeInt(result.getBytes().length);
                                                out.write(result.getBytes());
                                                break;
                                            case 2:
                                                System.out.println("PUT");
                                                break;
                                            default:
                                                continue;
                                        }
                                        for (int i = 0; i < paramaterArray.length; i++) {
                                            System.out.println(new String(paramaterArray[i]));
                                        }
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
}
