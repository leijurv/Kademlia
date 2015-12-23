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
    // Syntax is [int number of parameters, [int length of each parameter], protocol, [parameters]]

    ControlSocket(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket socket = server.accept();
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        new Thread() {
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
                                        for (int i = 0; i < lengthArray.length; i++) {
                                            byte[] data = new byte[lengthArray[i]];
                                            for (int j = 0; j < lengthArray[i]; j++) {
                                                data[j] = in.readByte();
                                            }
                                            paramaterArray[i] = data;
                                        }
                                        switch (protocol) {
                                            case 0:
                                                System.out.println("CONNECT");
                                                break;
                                            case 1:
                                                System.out.println("GET");
                                                String result = "result";
                                                out.writeInt(protocol);
                                                out.write(paramaterArray[0]);
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
                        }.start();

                    }
                } catch (IOException ex) {
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
}