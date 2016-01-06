/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import kademlia.lookup.LookupAssemblyMetadata;
import kademlia.lookup.Lookup;
import kademlia.lookup.LookupPut;
import kademlia.lookup.LookupNormalGet;
import kademlia.lookup.LookupSubscribe;
import kademlia.sub.SubscriptionManager;
import kademlia.request.RequestStore;
import kademlia.gui.GUI;
import kademlia.gui.ConnectionGUITab;
import kademlia.gui.DataGUITab;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DeflaterInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author leijurv
 */
public class Kademlia {
    public static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    public static final int k = 3;
    static public boolean verbose = false;
    static public boolean silent = false;
    static public boolean noGUI = false;
    public volatile transient int progress = 0;
    public volatile transient int max = 0;
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws org.apache.commons.cli.ParseException
     */
    public static void main(String[] args) throws IOException, ParseException {
        System.out.println(System.getProperty("java.home"));
        Options options = new Options();
        options.addOption("v", "verbose", false, "enables verbose mode");
        options.addOption("p", "port", true, "sets port for communication with other nodes");
        options.addOption("C", "enable-cli", false, "enables cli mode");
        options.addOption("G", "disable-gui", false, "disables gui mode");
        options.addOption("c", "control", false, "enables the control socket");
        options.addOption("P", "control-port", true, "sets the port for the control socket");
        options.addOption("s", "silent", false, "enables silent mode");
        options.addOption("h", "help", false, "help");
        options.addOption("ip", "manual-wan-ip", true, "set the WAN IP manually when Kademlia can't determine it automatically");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("kademlia", options);
            System.exit(0);
            return;
        }
        if (cmd.hasOption("v")) {
            verbose = true;
        }
        if (cmd.hasOption("s")) {
            silent = true;
        }
        int myPort = 7705;
        if (cmd.hasOption("p")) {
            myPort = Integer.parseInt(cmd.getOptionValue("p"));
        }
        Kademlia kad;
        if (cmd.hasOption("ip")) {
            kad = new Kademlia(myPort, cmd.getOptionValue("ip"));
        } else {
            kad = new Kademlia(myPort);
        }
        if (cmd.hasOption("c")) {
            int csPort = 7707;
            if (cmd.hasOption("P")) {
                csPort = Integer.parseInt(cmd.getOptionValue("P"));
            }
            ControlSocket controlSocket = new ControlSocket(csPort, kad);
        }
        if (!cmd.hasOption("G")) {
            new Thread() {
                @Override
                public void run() {
                    GUI.main(args, kad);
                }
            }.start();
        } else {
            noGUI = true;
        }
        if (cmd.hasOption("C")) {
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
                            kad.connectToHostAndPort(host, port);
                            break;
                        case "list":
                            synchronized (kad.connectionsLock) {
                                console.log(kad.connections);
                            }
                            console.log("RAM: " + kad.storedData.bytesStoredInRAM());
                            console.log("Disk: " + kad.storedData.bytesStoredOnDisk());
                            console.log("Total: " + kad.storedData.bytesStoredInTotal());
                            break;
                        case "flushram":
                            kad.storedData.flushRAM();
                            break;
                        case "flushall":
                            kad.storedData.flushAll();
                            break;
                        case "get":
                        case "getr":
                            long d = com.equals("get") ? Lookup.maskedHash(command.getBytes(), DDT.STANDARD_PUT_GET) : Long.parseLong(command);
                            console.log("Getting " + d);
                            byte[] cached = kad.storedData.get(d);
                            if (cached != null) {
                                console.log("stored locally");
                                if (!noGUI) {
                                    DataGUITab.incomingKeyValueData(d, cached);
                                }
                                console.log(new String(cached));
                                break;
                            }
                            new LookupNormalGet(d, kad).execute();
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
                        case "sub":
                            new LookupSubscribe(Lookup.maskedHash(command.getBytes(), DDT.STANDARD_PUT_GET), kad).execute();
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
        new LookupNormalGet(58009, k1).execute();
        Thread.sleep(1000);
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        console.log();
        new LookupNormalGet(58008, k1).execute();
        Thread.sleep(1000);
        System.exit(0);
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k2tok3.sendRequest(new RequestStore(123123312, "kuoea20".getBytes()));
        //k1tok2.sendRequest(new RequestFindNode(55543543));
        //k1tok2.sendRequest(new RequestStore(5021, "kush 420".getBytes()));
        //console.log("main is done");
    }
    final int port;
    public final Node myself;
    final Bucket[] buckets;
    public final ArrayList<Connection> connections;
    public final Object connectionsLock = new Object();
    public final DataStore storedData;
    final String dataStorageDir;
    public final Settings settings;
    final SubscriptionManager subManager;
    final HashMap<Node, ClientSubscriber> clientSubManager;
    private volatile boolean shouldSave = true;
    private final BigInteger myPrivateKey;
    public Kademlia(int port) throws IOException {
        this(port, whatIsMyIp());
    }
    public Kademlia(int port, String ip) throws IOException {
        this.port = port;
        dataStorageDir = System.getProperty("user.home") + "/.kademlia/port" + port + "/";
        console.log("I am " + ip);
        this.buckets = new Bucket[64];
        if (getSaveFile().exists()) {
            console.log("Kademlia is reading from save " + getSaveFile().getCanonicalPath());
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(getSaveFile())))) {
                byte[] priv = new byte[33];
                in.read(priv, 1, 32);
                myPrivateKey = new BigInteger(priv);
                for (int i = 0; i < 64; i++) {
                    buckets[i] = new Bucket(i, this, in);
                }
                settings = new Settings(in, this);
            }
        } else {
            myPrivateKey = new BigInteger(256, new SecureRandom());
            for (int i = 0; i < 64; i++) {
                buckets[i] = new Bucket(i, this);
            }
            settings = new Settings(this);
        }
        this.myself = new Node(ECPoint.base.multiply(myPrivateKey), ip, port);
        this.connections = new ArrayList<>();
        this.subManager = new SubscriptionManager(this);
        this.clientSubManager = new HashMap<>();
        console.log("Kademlia is using settings: " + settings);
        storedData = new DataStore(this);
        runKademlia();
        startSaveThread();
        startPingAllThread();
    }
    private void startPingAllThread() {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 64; i++) {
                    buckets[i].pingAll();
                }
            }
        });
    }
    public void heyYouShouldSaveSoon() {
        shouldSave = true;
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
        console.log("Kademlia is writing to save file");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getSaveFile())))) {
            out.write(ECPoint.toNormal(myPrivateKey.toByteArray()));
            for (int i = 0; i < 64; i++) {
                buckets[i].write(out);
            }
            settings.write(out);
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
        long key = Lookup.maskedHash(keyF.getBytes(), DDT.FILE_METADATA);
        byte[] caced = storedData.get(key);
        if (caced != null) {
            console.log("metadata stored locally");
            new FileAssembly(caced, this, storPath).assemble();
            return;
        }
        new LookupAssemblyMetadata(key, this, storPath).execute();
    }
    public void putfile(File file, String name) throws IOException, InterruptedException {
        console.log("Putting " + file + " under name " + name);
        try (FileInputStream fileIn = new FileInputStream(file)) {
            console.log("File is size " + fileIn.available());
            InputStream in = new DeflaterInputStream(fileIn);
            int partSize = 524288;
            progress = 0;
            max = 1;
            ArrayList<Long> hashes = new ArrayList<>();
            int summedSize = 0;
            boolean wl = false;
            int numParts = 0;
            while (true) {
                byte[] y = new byte[partSize];
                int j = in.read(y);
                if (j < 0) {
                    if (!wl) {
                        if (numParts == 1) {
                            console.log("This file was divided into only one part");
                        } else {
                            if (numParts == 0) {
                                throw new IllegalStateException("i=0");
                            }
                            console.log("This file was exactly divisible into parts of " + partSize + ", or I messed something up");
                        }
                    }
                    break;
                }
                if (wl) {
                    throw new IllegalStateException("More than one partial piece");
                }
                if (j != partSize) {
                    wl = true;
                }
                max++;
                summedSize += j;
                numParts++;
                long hash = Lookup.maskedHash(y, 0, j, DDT.CHUNK);
                hashes.add(hash);
                console.log("j: " + j + ", i: " + numParts + ", hash: " + hash);
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        new LookupPut(hash, Kademlia.this, y, System.currentTimeMillis(), 0, j).execute();
                    }
                });
                Thread.sleep(10);
            }
            console.log("Dividing file of size " + summedSize + " into " + hashes.size() + " partitions of size " + partSize);
            ByteArrayOutputStream theData = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(theData);
            out.writeInt(summedSize);
            out.writeInt(numParts);
            for (long l : hashes) {
                out.writeLong(l);
            }
            long metadataKey = Lookup.maskedHash(name.getBytes(), DDT.FILE_METADATA);
            new LookupPut(metadataKey, this, theData.toByteArray(), System.currentTimeMillis()).execute();
        }
    }
    public void put(long key, byte[] contents) {
        if (DDT.getFromKey(key) != DDT.STANDARD_PUT_GET) {
            throw new IllegalStateException("Provided key " + key + " has DDT " + DDT.getFromKey(key) + ", which should be STANDARD_PUT_GET");
        }
        new LookupPut(key, this, contents, System.currentTimeMillis()).execute();
    }
    public void put(String key, byte[] contents) {
        put(Lookup.maskedHash(key.getBytes(), DDT.STANDARD_PUT_GET), contents);
    }
    public void get(String stringKey) {
        long key = Lookup.maskedHash(stringKey.getBytes(), DDT.STANDARD_PUT_GET);
        console.log("Getting " + key);
        byte[] cached = storedData.get(key);
        if (cached != null) {
            console.log("stored locally");
            if (!noGUI) {
                DataGUITab.incomingKeyValueData(key, cached);
            }
            console.log(new String(cached));
            return;
        }
        new LookupNormalGet(key, this).execute();
    }
    private Bucket bucketFromNode(Node n) {
        return bucketFromDistance(myself.nodeid ^ n.nodeid);
    }
    private Bucket bucketFromDistance(long distance) {
        return buckets[bucketIndexFromDistance(distance)];
    }
    private static int bucketIndexFromDistance(long distance) {//todo replace with bsearch (low priority)
        for (int i = 0; i < 64; i++) {
            if (distance <= ((1L << i) - 1)) {
                return i;
            }
        }
        throw new IllegalStateException("Literally no possible way this could happen. Cosmic rays man. Apparently the long " + distance + " isn't less than or equal to Long.MAX_VALUE");
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
            shouldSave = true;
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
        shouldSave = true;
    }
    public ArrayList<Node> findNClosest(int num, long search) {//less efficent, but works correctly
        /*ArrayList<Node> closest = new ArrayList<>();
         for (Bucket bucket : buckets) {
         for (long nodeid : bucket.nodeids) {
         closest.add(bucket.nodes.get(nodeid));
         }
         }*/
        ArrayList<Node> closest = Stream.of(buckets).flatMap(bucket -> bucket.nodeids.stream().map(nodeid -> bucket.nodes.get(nodeid))).collect(Collectors.toCollection(ArrayList::new));
        closest.sort(Node.createDistanceComparator(search));
        if (closest.size() > num) {
            return new ArrayList<>(closest.subList(0, num));
        }
        return closest;
    }
    public ArrayList<Node> findNClosest1(int num, long search) {//this is a more efficient way to do it, but it doesn't quite work right
        ArrayList<Node> closest = new ArrayList<>();
        final Comparator<Node> distanceComparator = Node.createDistanceComparator(search);
        long currWorst = 0;
        int nf = 0;
        for (Bucket bucket : buckets) {
            for (long nodeid : bucket.nodeids) {
                if (nf < num) {
                    closest.add(bucket.nodes.get(nodeid));
                    nf++;
                } else {
                    if (nf == num) {
                        closest.sort(distanceComparator);
                        currWorst = closest.get(num - 1).nodeid ^ search;
                        nf++;
                    }
                    long distance = nodeid ^ search;
                    if (distance < currWorst) {
                        closest.add(bucket.nodes.get(nodeid));
                        closest.sort(distanceComparator);
                        closest.remove(num);
                        currWorst = closest.get(num - 1).nodeid ^ search;
                    }
                }
            }
        }
        if (nf <= num) {
            closest.sort(distanceComparator);
        }
        return closest;
    }
    private void runKademlia() throws IOException {
        createMainServer();
    }
    public void connectToHostAndPort(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        s.getOutputStream().write((byte) 0);
        handleSocket(s);
    }
    private void createMainServer() throws IOException {
        ServerSocket server = new ServerSocket(port);
        new Thread() {
            @Override
            public void run() {
                try {
                    console.log(myself + " listening " + server);
                    while (true) {
                        Socket socket = server.accept();
                        threadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DataInputStream in = new DataInputStream(socket.getInputStream());
                                    byte requestType = in.readByte();
                                    switch (requestType) {
                                        case 0:
                                            handleSocket(socket);
                                            break;
                                        case 7:
                                            handleSubscriber(socket);
                                            break;
                                        default:
                                            throw new IOException("Invalid request type " + requestType);
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
    public void handleSubscriber(Socket s) throws IOException {
        subManager.onSubscriberSocket(s);
    }
    public ClientSubscriber getClientSubscriberToNode(Node n) throws IOException {
        if (clientSubManager.get(n) == null) {
            clientSubManager.put(n, new ClientSubscriber(n));
        }
        return clientSubManager.get(n);
    }
    public Connection getConnectionToNode(Node n) {
        synchronized (connectionsLock) {
            for (Connection conn : connections) {
                if (conn.node.nodeid == n.nodeid) {
                    return conn;
                }
            }
            return null;
        }
    }
    public Connection getOrCreateConnectionToNode(Node n) throws IOException {
        Connection already = getConnectionToNode(n);
        if (already != null) {
            return already;
        }
        if (Kademlia.verbose) {
            synchronized (connectionsLock) {
                console.log("couldn't find " + n + " in " + connections + ", so making new");
            }
        }
        return establishConnection(n);
    }
    public ECPoint getSharedSecret(Node n) {
        return n.publicKey.multiply(myPrivateKey);
    }
    public Connection handleSocket(Socket socket) throws IOException {
        return handleSocket(socket, null);
    }
    public Connection handleSocket(Socket socket, Node expected) throws IOException {
        myself.write(new DataOutputStream(socket.getOutputStream()));
        Node other = new Node(new DataInputStream(socket.getInputStream()));
        console.log(myself + " Received node data " + other + " from socket " + socket + " with expectation " + expected);
        if (myself.equals(other)) {
            socket.close();//lol forgot this last time
            throw new IOException(new IllegalArgumentException("what the hell"));
        }
        Connection conn;
        try {
            conn = new Connection(other, socket, this);
        } catch (IllegalStateException x) {
            socket.close();
            throw new IOException(x);
        }
        addOrUpdate(other);//do this after establishing connection. the constructor for connection checks if they are telling the truth about their pubkey
        //and we only want to add nodes that tell the truth
        if (expected != null && (other.nodeid != expected.nodeid || !other.sameHost(expected))) {//todo think long and hard about what the behavior should be in this case
            //we don't run heyThisNodeIsBeingAnnoying(other)
            //because as far as we know, other hasn't done anything wrong.
            heyThisNodeIsBeingAnnoying(expected);//however we now know our expected is wrong, so we want to remove that
            conn.close();
            throw new IllegalStateException("Tried to connect to " + expected + " and they said they were " + other);
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    conn.doListen();
                } catch (IOException ex) {
                    synchronized (connectionsLock) {
                        connections.remove(conn);
                    }
                    conn.close();
                    Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, ex);
                    console.log("Error with connection " + conn + ", removing from list");
                    //heyThisNodeIsBeingAnnoying(other);
                    //for an error with a connection, don't delete the node
                    //but if reconnect fails, block that sucker
                }
            }
        });
        if (!noGUI) {
            ConnectionGUITab.addConnection();
        }
        synchronized (connectionsLock) {
            connections.add(conn);
        }
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
            s.getOutputStream().write(0);//connection mode is normal
            Connection conn = handleSocket(s, node);
            return conn;
        } catch (IOException e) {
            console.log("Unable to establish connection to " + node);
            Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, e);
            //if error while establishing connection, node is probably down
            heyThisNodeIsBeingAnnoying(node);
            throw e;//still throw that exception. better than returning null and getting null pointer exceptions fo days
        } catch (IllegalStateException e) {
//the only thing that causes illegalstateexception is if they say they aren't what we expected
            console.log("Unable to establish connection to " + node + " because they really are a different node");
            Logger.getLogger(Kademlia.class.getName()).log(Level.SEVERE, null, e);
            //heythisnodeisbeingannoying was already called by handlesocket
            throw new IOException(e);
        }
    }
    private static final List<String> blacklistIP = Arrays.asList(new String[]{"127.0.0.1", "127.0.1.1", "localhost"});
    public static String whatIsMyIp() throws IOException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        if (blacklistIP.contains(ip)) {
            throw new IllegalStateException("Unable to determine WAN IP. Please use -ip option to set manually");
        }
        return ip;
    }
}
