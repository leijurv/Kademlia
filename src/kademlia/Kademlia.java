/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Kademlia {
    static final int k = 3;
    static public boolean verbose = false;
    int progress = 0;
    int max = 0;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1 && args[1].equals("-v")) {
            verbose = true;
        }
        int myPort = Integer.parseInt(args[0]);
        long id = Math.abs(new Random().nextLong());
        Kademlia kad = new Kademlia(myPort, id);
        new Thread() {
            @Override
            public void run() {
                GUI.main(args, kad);
            }
        }.start();
        Scanner scan = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("> ");
                String command = scan.nextLine();
                String com = command.split(" ")[0];
                com = com.toLowerCase();
                if (command.contains(" ")) {
                    command = command.substring(command.indexOf(" ") + 1, command.length());
                }
                switch (com) {
                    case "connect":
                        String host;
                        int port;
                        if (command.contains(" ")) {
                            host = command.split(" ")[0];
                            port = Integer.parseInt(command.split(" ")[1]);
                        } else {
                            host = "localhost";
                            port = Integer.parseInt(command);
                        }
                        kad.handleSocket(new Socket(host, port));
                        break;
                    case "list":
                        System.out.println(kad.connections);
                        System.out.println(kad.storedData);
                        break;
                    case "get":
                    case "getr":
                        long d = com.equals("get") ? Lookup.hash(command.getBytes()) : Long.parseLong(command);
                        System.out.println("Getting " + d);
                        byte[] cached = kad.storedData.get(d);
                        if (cached != null) {
                            System.out.println("stored locally");
                            GUI.incomingKeyValueData(d, cached);
                            System.out.println(new String(cached));
                            break;
                        }
                        new Lookup(d, kad, true).execute();
                        break;
                    case "put":
                        String path = command.substring(0, command.indexOf(" "));
                        byte[] contents = command.substring(command.indexOf(" ") + 1, command.length()).getBytes();
                        kad.put(path, contents);
                        break;
                    case "getfile":
                        String storPath = command.split(" ")[1];
                        String keyF = command.split(" ")[0];
                        System.out.println("Getting " + keyF + " and storing in " + storPath);
                        byte[] caced = kad.storedData.get(Lookup.hash(keyF.getBytes()));
                        if (caced != null) {
                            System.out.println("metadata stored locally");
                            new FileAssembly(caced, kad, storPath).assemble();
                            break;
                        }
                        new Lookup(keyF, kad, true, true, storPath).execute();
                        break;
                    case "putfile":
                        String name = command.substring(0, command.indexOf(" "));
                        String filepath = command.substring(command.indexOf(" ") + 1, command.length());
                        System.out.println("Putting " + filepath + " under name " + name);
                        FileInputStream in = new FileInputStream(new File(filepath));
                        int size = in.available();
                        int partSize = 524288;
                        int partitions = (int) Math.ceil(((double) size) / ((double) partSize));
                        System.out.println("Dividing file of size " + size + " into " + partitions + " partitions of size " + partSize);
                        ByteArrayOutputStream theData = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(theData);
                        out.writeInt(size);
                        out.writeInt(partSize);
                        kad.progress = 0;
                        kad.max = partitions + 1;
                        long start = System.currentTimeMillis();
                        for (int i = 0; i < partitions; i++) {
                            int psize = partSize;
                            if (i == partitions - 1) {
                                psize = size - (partitions - 1) * partSize;
                            }
                            byte[] y = new byte[psize];
                            int j = in.read(y);
                            if (j != psize) {
                                throw new IllegalStateException("screw you fileinputstream");
                            }
                            long hash = Lookup.hash(y);
                            out.writeLong(hash);
                            System.out.println("psize: " + psize + ", i: " + i + ", hash: " + hash);
                            new Thread() {
                                @Override
                                public void run() {
                                    new Lookup(hash, kad, y, start).execute();
                                }
                            }.start();
                            Thread.sleep(100);
                        }
                        new Lookup(name, kad, theData.toByteArray(), start).execute();
                        in.close();
                        break;
                    case "help":
                        System.out.println("HELP");
                        System.out.println("*****");
                        System.out.println("How to read:");
                        System.out.println("() - optional arguments");
                        System.out.println("[] - require arguments");
                        System.out.println("*****");
                        System.out.println("CONNECT (hostname) [port] - connect to a node");
                        System.out.println("LIST - lists all connected nodes");
                        System.out.println("GET [key] - gets value based on key");
                        System.out.println("GETR [hash of key] - gets value based on hash of key");
                        System.out.println("GETFILE [key] [path] - gets a file on the network based on key and stores it at the provided path");
                        System.out.println("PUT [key] [value] - stores a value on the network");
                        System.out.println("PUTFILE [key] [path] - stores a file on the network");
                        System.out.println("HELP - gets help");
                        System.out.println("EXIT - exits program");
                        break;
                    case "exit":
                        System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("but we still good homie. keep on hitting me up with them requests");
            }
        }
    }
    public static void test(String[] args) throws IOException, InterruptedException {
        Kademlia k1 = new Kademlia(5021, 5);
        Kademlia k2 = new Kademlia(5022, 6);
        Kademlia k3 = new Kademlia(5023, 58008);
        Connection k2tok3 = k2.establishConnection(k3.myself);
        Connection k1tok2 = k1.establishConnection(k2.myself);
        Thread.sleep(500);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        k2tok3.sendRequest(new RequestStore(58009, "yolo swag 420 noscope".getBytes(), System.currentTimeMillis()));
        Thread.sleep(500);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        new Lookup(58009, k1, true).execute();
        Thread.sleep(1000);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        new Lookup(58008, k1, true).execute();
        Thread.sleep(1000);
        System.exit(0);
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k2tok3.sendRequest(new RequestStore(123123312, "kuoea20".getBytes()));
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k1tok2.sendRequest(new RequestStore(5021, "kush 420".getBytes()));
        //System.out.println("main is done");
    }
    final int port;
    final Node myself;
    final Bucket[] buckets;
    final ArrayList<Connection> connections;
    final DataStore storedData;
    public Kademlia(int port, long nodeid) throws IOException {
        this.port = port;
        String ip = whatIsMyIp();
        System.out.println("I am " + ip);
        this.myself = new Node(nodeid, ip, port);
        this.buckets = new Bucket[64];
        for (int i = 0; i < 64; i++) {
            buckets[i] = new Bucket(i, this);
        }
        this.connections = new ArrayList<>();
        storedData = new DataStore("port" + port, this);
        runKademlia();
    }
    public void put(long key, byte[] contents) {
        new Lookup(key, this, contents, System.currentTimeMillis()).execute();
    }
    public void put(String key, byte[] contents) {
        new Lookup(key, this, contents, System.currentTimeMillis()).execute();
    }
    public void get(String key) {
        long d = Lookup.hash(key.getBytes());
        System.out.println("Getting " + d);
        byte[] cached = storedData.get(d);
        if (cached != null) {
            System.out.println("stored locally");
            GUI.incomingKeyValueData(d, cached);
            System.out.println(new String(cached));
            return;
        }
        new Lookup(d, this, true).execute();
    }
    public Bucket bucketFromNode(Node n) {
        return bucketFromDistance(myself.nodeid ^ n.nodeid);
    }
    public Bucket bucketFromDistance(long distance) {
        return buckets[bucketIndexFromDistance(distance)];
    }
    public static int bucketIndexFromDistance(long distance) {
        for (int i = 0; i < 64; i++) {
            //System.out.println(i + " " + distance + " " + ((1L << i) - 1));
            if (distance <= ((1L << i) - 1)) {
                return i;
            }
        }
        throw new IllegalStateException("your mom " + distance);
    }
    public void addOrUpdate(Node node) {
        if (node.nodeid == myself.nodeid) {
            if (Kademlia.verbose) {
                System.out.println(myself + " trying to add/update myself, returning");
            }
            return;
        }
        Bucket bucket = bucketFromNode(node);
        if (bucket.addOrUpdate(node)) {
            if (Kademlia.verbose) {
                System.out.println(myself + " Inserting " + node + " into " + bucket);
            }
        }
    }
    public void heyThisNodeIsBeingAnnoying(Node node) {
        if (node.equals(myself)) {
            throw new IllegalArgumentException("IM NOT ANNOYING");
        }
        Bucket bucket = bucketFromNode(node);
        bucket.removeNode(node);
    }
    public ArrayList<Node> findNClosest(int num, long search) {
        ArrayList<Node> closest = new ArrayList<>();
        long currWorst = 0;
        int nf = 0;
        for (Bucket bucket : buckets) {
            for (long nodeid : bucket.nodeids) {
                if (nf < num) {
                    closest.add(bucket.nodes.get(nodeid));
                    nf++;
                } else {
                    if (nf == num) {
                        closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ search).compareTo(o2.nodeid ^ search));
                        currWorst = closest.get(num - 1).nodeid ^ search;
                        nf++;
                    }
                    long distance = nodeid ^ search;
                    if (distance < currWorst) {
                        closest.add(bucket.nodes.get(nodeid));
                        closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ search).compareTo(o2.nodeid ^ search));
                        closest.remove(num);
                        currWorst = closest.get(num - 1).nodeid ^ search;
                    }
                }
            }
        }
        if (nf <= num) {
            closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ search).compareTo(o2.nodeid ^ search));
        }
        return closest;
    }
    private void runKademlia() throws IOException {
        createMainServer();
    }
    public void createMainServer() throws IOException {
        ServerSocket server = new ServerSocket(port);
        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println(myself + " listening " + server);
                    while (true) {
                        Socket socket = server.accept();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    handleSocket(socket);
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
    public Connection getConnectionToNode(Node n) {
        for (Connection conn : connections) {
            if (conn.node.nodeid == n.nodeid) {
                return conn;
            }
        }
        return null;
    }
    public Connection getOrCreateConnectionToNode(Node n) throws IOException {
        Connection already = getConnectionToNode(n);
        if (already != null) {
            return already;
        }
        if (Kademlia.verbose) {
            System.out.println("couldn't find " + n + " in " + connections + ", so making new");
        }
        return establishConnection(n);
    }
    private Connection handleSocket(Socket socket) throws IOException {
        myself.write(new DataOutputStream(socket.getOutputStream()));
        Node other = new Node(new DataInputStream(socket.getInputStream()));
        if (Kademlia.verbose) {
            System.out.println(myself + " Received node data " + other + " from socket " + socket);
        }
        addOrUpdate(other);
        Connection conn = new Connection(other, socket, this);
        new Thread() {
            @Override
            public void run() {
                try {
                    conn.doListen();
                } catch (IOException ex) {
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Error with connection " + conn + ", removing from list");
                    connections.remove(conn);
                    conn.isStillRunning = false;
                    heyThisNodeIsBeingAnnoying(other);
                }
            }
        }.start();
        connections.add(conn);
        return conn;
    }
    private Connection establishConnection(Node node) throws IOException {
        if (Kademlia.verbose) {
            System.out.println(myself + " is proactively establishing connection to " + node);
        }
        if (node.equals(myself)) {
            throw new IllegalArgumentException("can't make a connection to yourself");
        }
        Socket s = new Socket(node.host, node.port);
        Connection conn = handleSocket(s);
        if (conn.node.nodeid != node.nodeid || !conn.node.sameHost(node)) {
            throw new IllegalStateException("Tried to connect to " + node + " and they said they were " + conn.node);
        }
        return conn;
    }
    public static String whatIsMyIp() throws IOException {
        try (Socket s = new Socket("www.google.com", 80)) {
            return s.getLocalAddress().getHostAddress();
        }
    }
}
