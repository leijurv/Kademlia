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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1 && args[1].equals("-v")) {
            verbose = true;
        }
        int myPort = Integer.parseInt(args[0]);
        Kademlia kad = new Kademlia(myPort);
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
                        console.log(kad.connections);
                        console.log(kad.storedData);
                        break;
                    case "get":
                    case "getr":
                        long d = com.equals("get") ? Lookup.hash(command.getBytes()) : Long.parseLong(command);
                        console.log("Getting " + d);
                        byte[] cached = kad.storedData.get(d);
                        if (cached != null) {
                            console.log("stored locally");
                            DataGUITab.incomingKeyValueData(d, cached);
                            console.log(new String(cached));
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
                        kad.getfile(keyF, storPath);
                        break;
                    case "putfile":
                        String name = command.substring(0, command.indexOf(" "));
                        String filepath = command.substring(command.indexOf(" ") + 1, command.length());
                        kad.putfile(new File(filepath), name);
                        break;
                    case "help":
                        console.log("HELP");
                        console.log("*****");
                        console.log("How to read:");
                        console.log("() - optional arguments");
                        console.log("[] - require arguments");
                        console.log("*****");
                        console.log("CONNECT (hostname) [port] - connect to a node");
                        console.log("LIST - lists all connected nodes");
                        console.log("GET [key] - gets value based on key");
                        console.log("GETR [hash of key] - gets value based on hash of key");
                        console.log("GETFILE [key] [path] - gets a file on the network based on key and stores it at the provided path");
                        console.log("PUT [key] [value] - stores a value on the network");
                        console.log("PUTFILE [key] [path] - stores a file on the network");
                        console.log("HELP - gets help");
                        console.log("EXIT - exits program");
                        break;
                    case "exit":
                        System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                console.log("but we still good homie. keep on hitting me up with them requests");
            }
        }
    }
    public static void test(String[] args) throws IOException, InterruptedException {
        Kademlia k1 = new Kademlia(5021);
        Kademlia k2 = new Kademlia(5022);
        Kademlia k3 = new Kademlia(5023);
        Connection k2tok3 = k2.establishConnection(k3.myself);
        Connection k1tok2 = k1.establishConnection(k2.myself);
        Thread.sleep(500);
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        k2tok3.sendRequest(new RequestStore(58009, "yolo swag 420 noscope".getBytes(), System.currentTimeMillis()));
        Thread.sleep(500);
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        new Lookup(58009, k1, true).execute();
        Thread.sleep(1000);
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        new Lookup(58008, k1, true).execute();
        Thread.sleep(1000);
        System.exit(0);
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k2tok3.sendRequest(new RequestStore(123123312, "kuoea20".getBytes()));
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k1tok2.sendRequest(new RequestStore(5021, "kush 420".getBytes()));
        //console.log("main is done");
    }
    final int port;
    final Node myself;
    final Bucket[] buckets;
    final ArrayList<Connection> connections;
    final DataStore storedData;
    final String dataStorageDir;
    volatile boolean shouldSave = true;
    public Kademlia(int port) throws IOException {
        this.port = port;
        dataStorageDir = System.getProperty("user.home") + "/.kademlia/port" + port + "/";
        storedData = new DataStore(this);
        String ip = whatIsMyIp();
        long nodeid;
        console.log("I am " + ip);
        this.buckets = new Bucket[64];
        if (getSaveFile().exists()) {
            console.log("Kademlia is reading from save");
            try (FileInputStream fileIn = new FileInputStream(getSaveFile())) {
                DataInputStream in = new DataInputStream(fileIn);
                nodeid = in.readLong();
                for (int i = 0; i < 64; i++) {
                    buckets[i] = new Bucket(i, this, in);
                }
            }
        } else {
            nodeid = Math.abs(new Random().nextLong());
            for (int i = 0; i < 64; i++) {
                buckets[i] = new Bucket(i, this);
            }
        }
        this.myself = new Node(nodeid, ip, port);
        this.connections = new ArrayList<>();
        runKademlia();
        startSaveThread();
    }
    private void startSaveThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (shouldSave) {
                            shouldSave = false;
                            writeToSave();
                        }
                    }
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    private void writeToSave() throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(getSaveFile())) {
            DataOutputStream out = new DataOutputStream(fileOut);
            out.writeLong(myself.nodeid);
            for (int i = 0; i < 64; i++) {
                buckets[i].write(out);
            }
        }
    }
    private File getSaveFile() {
        return new File(dataStorageDir + "main");
    }
    public void getfile(String keyF, File storPath) throws IOException {
        getfile(keyF, storPath.getAbsolutePath());
    }
    public void getfile(String keyF, String storPath) throws IOException {
        console.log("Getting " + keyF + " and storing in " + storPath);
        byte[] caced = storedData.get(Lookup.hash(keyF.getBytes()));
        if (caced != null) {
            console.log("metadata stored locally");
            new FileAssembly(caced, this, storPath).assemble();
            return;
        }
        new Lookup(keyF, this, true, true, storPath).execute();
    }
    public void putfile(File file, String name) throws IOException, InterruptedException {
        console.log("Putting " + file + " under name " + name);
        try (FileInputStream in = new FileInputStream(file)) {
            int size = in.available();
            int partSize = 524288;
            int partitions = (int) Math.ceil(((double) size) / ((double) partSize));
            console.log("Dividing file of size " + size + " into " + partitions + " partitions of size " + partSize);
            ByteArrayOutputStream theData = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(theData);
            out.writeInt(size);
            out.writeInt(partSize);
            progress = 0;
            max = partitions + 1;
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
                console.log("psize: " + psize + ", i: " + i + ", hash: " + hash);
                new Thread() {
                    @Override
                    public void run() {
                        new Lookup(hash, Kademlia.this, y, start).execute();
                    }
                }.start();
                Thread.sleep(100);
            }
            new Lookup(name, this, theData.toByteArray(), start).execute();
        }
    }
    public void put(long key, byte[] contents) {
        new Lookup(key, this, contents, System.currentTimeMillis()).execute();
    }
    public void put(String key, byte[] contents) {
        new Lookup(key, this, contents, System.currentTimeMillis()).execute();
    }
    public void get(String key) {
        long d = Lookup.hash(key.getBytes());
        console.log("Getting " + d);
        byte[] cached = storedData.get(d);
        if (cached != null) {
            console.log("stored locally");
            DataGUITab.incomingKeyValueData(d, cached);
            console.log(new String(cached));
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
    public static int bucketIndexFromDistance(long distance) {//todo replace with bsearch (low priority)
        for (int i = 0; i < 64; i++) {
            if (distance <= ((1L << i) - 1)) {
                return i;
            }
        }
        throw new IllegalStateException("your mom " + distance);
    }
    public void addOrUpdate(Node node) {
        if (node.equals(myself)) {
            if (Kademlia.verbose) {
                console.log(myself + " trying to add/update myself, returning");
            }
            return;
        }
        Bucket bucket = bucketFromNode(node);
        if (bucket.addOrUpdate(node)) {
            if (Kademlia.verbose) {
                console.log(myself + " Inserting " + node + " into " + bucket);
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
    public ArrayList<Node> findNClosest(int num, long search) {//less efficent, but works correctly
        /*ArrayList<Node> closest = new ArrayList<>();
         for (Bucket bucket : buckets) {
         for (long nodeid : bucket.nodeids) {
         closest.add(bucket.nodes.get(nodeid));
         }
         }*/
        ArrayList<Node> closest = Stream.of(buckets).flatMap(bucket -> bucket.nodeids.stream().map(nodeid -> bucket.nodes.get(nodeid))).collect(Collectors.toCollection(ArrayList::new));
        closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ search).compareTo(o2.nodeid ^ search));
        if (closest.size() > num) {
            return new ArrayList<>(closest.subList(0, num));
        }
        return closest;
    }
    public ArrayList<Node> findNClosest1(int num, long search) {//this is a more efficient way to do it, but it doesn't quite work right
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
                    console.log(myself + " listening " + server);
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
            console.log("couldn't find " + n + " in " + connections + ", so making new");
        }
        return establishConnection(n);
    }
    public Connection handleSocket(Socket socket) throws IOException {
        myself.write(new DataOutputStream(socket.getOutputStream()));
        Node other = new Node(new DataInputStream(socket.getInputStream()));
        if (Kademlia.verbose) {
            console.log(myself + " Received node data " + other + " from socket " + socket);
        }
        addOrUpdate(other);
        Connection conn = new Connection(other, socket, this);
        new Thread() {
            @Override
            public void run() {
                try {
                    conn.doListen();
                } catch (IOException ex) {
                    connections.remove(conn);
                    conn.isStillRunning = false;
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                    console.log("Error with connection " + conn + ", removing from list");
                    ConnectionGUITab.stoppedConnection(conn.node.nodeid);
                    //heyThisNodeIsBeingAnnoying(other);
                    //for an error with a connection, don't delete the node
                    //but if reconnect fails, block that sucker
                }
            }
        }.start();
        ConnectionGUITab.addConnection();
        connections.add(conn);
        return conn;
    }
    private Connection establishConnection(Node node) throws IOException {
        try {
            if (Kademlia.verbose) {
                console.log(myself + " is proactively establishing connection to " + node);
            }
            if (node.equals(myself)) {
                throw new IllegalArgumentException("can't make a connection to yourself");
            }
            Socket s = new Socket(node.host, node.port);
            Connection conn = handleSocket(s);
            if (conn.node.nodeid != node.nodeid || !conn.node.sameHost(node)) {
                s.close();//this will trigger an IOException to remove conn from the list
                heyThisNodeIsBeingAnnoying(node);
                heyThisNodeIsBeingAnnoying(conn.node);
                throw new IllegalStateException("Tried to connect to " + node + " and they said they were " + conn.node);
            }
            return conn;
        } catch (IllegalArgumentException | IOException | IllegalStateException e) {
            //if error while establishing connection, node is probably down
            heyThisNodeIsBeingAnnoying(node);
            throw e;//still throw that exception. better than returning null and getting null pointer exceptions fo days
        }
    }
    public static String whatIsMyIp() throws IOException {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
