import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MWServer {
    public final static int NONE = 0;
    public final static int WAITTING = 1;
    public final static int FULL = 2;
    public final static int PLAYING = 3;
    public final static int CLOSE = 4;
            
    //public static int PORT = 25001;
    public static int PORT = 25001;
    private ServerSocket echoServer = null;
    private Socket clientSocket = null;
    private int numConnections = 0;
    private Connect connect;
    private Queue<Socket> sockets = new LinkedList<Socket>();
    
    
    private List<Battle> battles = new ArrayList<Battle>();
    private List<Room> rooms = new ArrayList<Room>();
    private Dictionary<String, List<Client>> users = new Hashtable<String, List<Client>>();
    private List<Client> clients = new ArrayList<Client>();
            
    public static void main(String[] args) {
	MWServer server = new MWServer();
	server.startServer();
    }

    public MWServer(){
        connect = new Connect();
        connect.updateStatus();
    }
    
    public void startServer() {
	
        try {
	    echoServer = new ServerSocket(PORT);
        }
        
        catch (IOException e) {
	    System.out.println(e);
        } 
	System.out.println("Waitting a connection.....");
	
        long startTime = 0;
	while (true) {
	    try {
		numConnections++;
                clientSocket = echoServer.accept();
                if (clientSocket != null)
                    sockets.add(clientSocket);
                startTime = System.currentTimeMillis();
                while (!sockets.isEmpty()){
                    if (System.currentTimeMillis() - startTime > 20){
                        startTime = System.currentTimeMillis();
                        clientSocket = sockets.poll();
                        ClientHandle client = new ClientHandle(clientSocket, numConnections, this, connect);
                        new Thread(client).start();
                    }
                }
		
	    }   
	    catch (IOException e) {
		System.out.println(e);
	    }
	}
    }
    
    public void stopServer() {
	System.exit(0);
    }
    
    public void addClient(String name, Socket socket){
        clients.add(new Client(socket, name, 0));
    }
    
    public void removeClient(String name){
        for (int index = 0; index <  clients.size(); index++)
            if (clients.get(index).name.equals(name)){
                clients.remove(index);
            }
    }
    
    public String getUserFromSocket(Socket socket){
        for (int index = 0; index <  clients.size(); index++)
            if (clients.get(index).socket == socket){
                return clients.get(index).name;
            }
        return null;
    }
    
    public Socket getSocketFromUser(String bossname, String user){
        List<Client> list = users.get(bossname);
        for (int index = 0; index <  list.size(); index++)
            if (list.get(index).name.equals(user)){
                return list.get(index).socket;
            }
        return null;
    }
    
    public void createRoom(Socket client, String username, String mascotname, String bossname, int HP, int attack, int bossHP, int bossAttack){
        removeRoomFromUser(username);
        Room room = new Room(client, username, mascotname, bossname, HP, attack, bossHP, bossAttack);
        this.rooms.add(room);
    }
    
    public void addUserListBoss(String username, Socket socket, String bossname){
        Client client = new Client(socket, username, 0);
        List<Client> list = users.get(bossname);
        if (list == null)
            list = new ArrayList<Client>();
        list.add(client);
        users.put(bossname, list);
    }
    
    public boolean isUserListBoss(String bossname, String user){
        List<String> usernames = getUsersListBoss(bossname);
        if (usernames == null)
            return false;
        for (int index = 0; index < usernames.size(); index++)
            if (usernames.get(index).equals(user))
                return true;
        return false;
    }
    
    public List<String> getUsersListBoss(String bossname){
        List<Client> list = users.get(bossname);
        List<String> usernames = new ArrayList<String>();
        if (list == null)
            return null;
        for (int index = 0; index < list.size(); index++)
            usernames.add(list.get(index).name);
        return usernames;
    }
    
    public List<String> getFreeUsersListBoss(String bossname){
        List<Client> list = users.get(bossname);
        List<String> usernames = new ArrayList<String>();
        if (list == null)
            return null;
        for (int index = 0; index < list.size(); index++)
            if(list.get(index).status == NONE)
                usernames.add(list.get(index).name);
        return usernames;
    }
    
    public boolean removeUserListBoss(String username, String bossname){
        List<Client> list = users.get(bossname);
        if (list == null)
            return false;
        for (int index = 0; index < list.size(); index++)
            if (list.get(index).name.equals(username)){
                list.remove(index);
            }
        users.put(bossname, list);
        return false;
    }
    
    public void setUserStatusBoss(String username, String bossname, int status){
        List<Client> list = users.get(bossname);
        if (list != null){
            System.out.println(list.size());
            for (int index = 0; index < list.size(); index++)
                if (list.get(index).name.equals(username)){
                    list.get(index).status = status;
                    users.put(bossname, list);
                    return ;
                }
        }
    } 
    
    public int getUserStatusBoss(String username, String bossname){
        List<Client> list = users.get(bossname);
        if (list != null){
            System.out.println(list.size());
            for (int index = 0; index < list.size(); index++)
                if (list.get(index).name.equals(username)){
                    return list.get(index).status;
                }
        }
        return -1;
    }
        
    public void sendDataToRoomList(String bossname, Socket clientSocket, String command){
        List<Client> list = users.get(bossname);
        if (list != null){
            System.out.println("sendDataToRoomList size socket = " + list.size());
            for (int index = 0; index < list.size(); index++){
                Socket socket = list.get(index).socket;
                if ((socket != null) && (socket != clientSocket)){
                    System.out.println("index = " + index + " :" + socket.getPort());

                    try {
                        PrintStream os = new PrintStream(socket.getOutputStream(), false);
                        os.println(command);
                        System.out.println("from " + list.get(index).name + ": " + command);
                    } catch (IOException ex) {
                        Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
                     }
                }
            }
        }
    }
    
    public Room joinRoom(Socket client, String username, String mascotname, String keyname, int HP, int attack){
        for (int index = 0; index < rooms.size(); index++){
            if (rooms.get(index).keyname.equals(keyname)){
                rooms.get(index).joinRoom(client, username, mascotname, keyname, HP, attack);
                return rooms.get(index);
            }
        }
        return null;
    }
    
    public String getListRoom(String bossname){
        String result = "022RT##";
        for (int index = 0; index < rooms.size(); index++)
            if (rooms.get(index).bossname.equals(bossname) && rooms.get(index).status == 0){
                result += rooms.get(index).keyname + "," + rooms.get(index).count + "##";
            }
        return result;
    }
    
    public Room getRoomFromKey(String username){
        for (int index = 0; index < rooms.size(); index++){
            if (rooms.get(index).keyname.equals(username))
                return rooms.get(index);
        }
        return null;
    }
    
    public Room getRoomFromUser(String username){
        for (int index = 0; index < rooms.size(); index++){
            if (rooms.get(index).getUser(username) != -1)
                return rooms.get(index);
        }
        return null;
    }
    
    public void removeRoomFromUser(String username){
        Room key = getRoomFromUser(username);
        if (key == null)
            return;
        for (int index = 0; index < rooms.size(); index++){
            if (rooms.get(index).keyname.equals(key.keyname)){
                rooms.remove(index);
                break;
            }
        }
    }
    
    public void removeRoomFromKey(String username){
        for (int index = 0; index < rooms.size(); index++){
            if (rooms.get(index).keyname.equals(username))
                rooms.remove(index);
        }
    }
    
    public Battle searchBattle(Socket clientSocket, String username, String mascotname, int HP, int attack, int level){
        Battle battle;
        for (int index = 0; index < battles.size(); index++)
            if ((battles.get(index).count == 1) && (Math.abs(battles.get(index).level1 - level) < 3)) {
                if (battles.get(index).username1.equals(username))
                    return battles.get(index);
                battles.get(index).joinBattle(clientSocket, username, mascotname, HP, attack, level);
                return battles.get(index);
            }
        battle = new Battle(clientSocket, username, mascotname, HP, attack, level); 
        battles.add(battle);
        return battle;
    }
    
    public Battle getBattle(Socket client){
        for (int index = 0; index < battles.size(); index++){
            if (battles.get(index).client1 == client)
            {
                return battles.get(index);
            }
            if (battles.get(index).client2 == client)
            {
                return battles.get(index);
            }
        }
        return null;
    }
    
    public void removeBattle(String username){
        //battles.remove(battle);
        for (int index = 0; index < battles.size(); index++){
            
            if (battles.get(index).username1 != null && battles.get(index).username1.equals(username))
            {
                battles.remove(index);
                return;
            }
            
            if (battles.get(index).username2 != null && battles.get(index).username2.equals(username))
            {
                battles.remove(index);
                return;
            }
        }
    }
    
    public Battle requestNewRound(String username){
        for (int index = 0; index < battles.size(); index++){
            if (battles.get(index).username1.equals(username)){
                    battles.get(index).isUpdateClient1 = true;
                    return battles.get(index);
            }
            if (battles.get(index).username2.equals(username)){
                    battles.get(index).isUpdateClient2 = true;
                    return battles.get(index);
            }
        }
        return null;
    }
    
    public Battle requestPointRound(String username, String mascotname, int attack, int defence){
        for (int index = 0; index < battles.size(); index++)
            if (battles.get(index).count == 2){
            if (battles.get(index).username1.equals(username) ){
                battles.get(index).defence1 = defence;
                battles.get(index).attack1 = attack;
                battles.get(index).isUpdateClient1 = true;
                return battles.get(index);
            }
            if (battles.get(index).username2.equals(username) ){
                battles.get(index).defence2 += defence;
                battles.get(index).attack2 = attack;
                battles.get(index).isUpdateClient2 = true;
                return battles.get(index);
            }
        }
        return null;
    }
    
    public Battle requestResultMatch(String username){
        for (int index = 0; index < battles.size(); index++){
            if (battles.get(index).username1.equals(username) || battles.get(index).username2.equals(username)){
               return battles.get(index); 
            }
        }
        return null;
    }
}

class Client {
    public final static int LOG = 0;
    public final static int BOSS_NONE = 1;
    public final static int BOSS_PLAYING = 2;
    public Socket socket;
    public String name;
    public int status;
    
    public Client(Socket socket, String name, int status){
        this.socket = socket;
        this.name = name;
        this.status = status;
    }
}

class Room {
    public final static int NONE = 0;
    public final static int WAITTING = 1;
    public final static int FULL = 2;
    public final static int PLAYING = 3;
    public final static int CLOSE = 4;
    
    public int count = 0;
    public int status = NONE;
    public String bossname;
    public String keyname;
    public int HP = 0;
    public int attack = 0;
    public int defence = 0;
    public int bossHP = 0;
    public int bossAttack = 0;
    public int bossDefence = 0;
    
    public List<Socket> client = new ArrayList<Socket>();
    public List<String> username = new ArrayList<String>();
    public List<String> mascotname = new ArrayList<String>();
    public List<Boolean> isUpdateClient = new ArrayList<Boolean>();
    public List<Boolean> isReceiveResult = new ArrayList<Boolean>();
    
    public Room(Socket client, String username, String mascotname, String bossname, int HP, int attack, int bossHP, int bossAttack){
        count++;
        this.bossHP = bossHP;
        this.bossAttack = bossAttack;
        this.bossname = bossname;
        this.keyname = username;
        this.HP += HP;
        this.attack += attack;
        this.username.add(username);
        this.client.add(client);
        this.mascotname.add(mascotname);
        this.isUpdateClient.add(false);
        this.isReceiveResult.add(false);
    }
    
    public int getUser(String username){
        for (int index = 0; index < this.username.size(); index++)
            if (this.username.get(index).equals(username))
                return index;
        return -1;
    }
    
    public boolean isUpdate(){
        for (int index = 0; index < isUpdateClient.size(); index++)
            if (this.isUpdateClient.get(index) == false)
                return false;
        return true;
    }
    
    public boolean isResult(){
        for (int index = 0; index < isReceiveResult.size(); index++)
            if (this.isReceiveResult.get(index) == false)
                return false;
        return true;
    }
    
    public void updatePointRound(int attack, int defence, int bossAT, int bossDF, String username){
        int index = getUser(username);
        this.attack += attack;
        this.defence += defence;
        this.bossAttack += bossAT;
        isUpdateClient.set(index, true);
    }
    
    public void updateFinalRound(String username){
        int index = getUser(username);
        isReceiveResult.set(index, true);
    }
    
    public void sendData(String command){
        for (int index = 0; index < client.size(); index++){
            Socket socket = client.get(index);
            if (!socket.isClosed()){
                PrintStream os;
                try {
                    os = new PrintStream(socket.getOutputStream(), true);
                    os.println(command);
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
                 }
            }
        }
       
    }
    
    public void removeUser(String user){
        for (int index = 0; index < username.size(); index++)
            if (this.username.get(index).equals(user)){
                this.username.remove(index);
                this.isReceiveResult.remove(index);
                this.isUpdateClient.remove(index);
                this.mascotname.remove(index);
                this.client.remove(index);
                this.count--;
                return;
            }
    }
    
    public boolean getStatus(){
        if (count >= 2 && count <= 4)
            return true;
        else
            return false;
    }
    
    public boolean joinRoom(Socket client, String username, String mascotname, String keyname, int HP, int attack){
//        if (!getStatus())
//            //return false;
        
        count++;
        this.HP += HP;
        this.attack += attack;
        this.username.add(username);
        this.client.add(client);
        this.mascotname.add(mascotname);
        this.isUpdateClient.add(false);
        this.isReceiveResult.add(false);
        return true;
        
    }
}

class Battle {
    public Socket client1 = null;
    public Socket client2 = null;
    public String username1 = null;
    public String username2 = null;
    public String mascotname1 = null;
    public String mascotname2 = null;
    public int level1 = 0;
    public int level2 = 0;
    public int HP1 = 0;
    public int HP2 = 0;
    public int attack1 = 0;
    public int attack2 = 0;
    public int defence1 = 0;
    public int defence2 = 0;
    public int count = 0;
    public boolean isUpdateClient1 = false;
    public boolean isUpdateClient2 = false;
    public boolean isReceiveResult1 = false;
    public boolean isReceiveResult2 = false;
    public boolean isRematch1 = false;
    public boolean isRematch2 = false;
    public int maxHP1 = 0;
    public int maxHP2 = 0;
    
    public Battle(Socket client, String username, String mascotname, int HP, int attack, int level){
        count++;
        HP1 = HP;
        maxHP1 = HP;
        attack1 = attack;
        client1 = client;
        username1 = username;
        mascotname1 = mascotname;
        this.level1 = level;
    }
    
    public void updateBattle(int HP1, int attack1, int HP2, int attack2){
            this.HP1 = HP1;
            this.HP2 = HP2;
            this.attack1 = attack1;
            this.attack2 = attack2;
            maxHP1 = HP1;
            maxHP2 = HP2;
            defence1 = 0;
            defence2 = 0;
            count = 0;
            isUpdateClient1 = false;
            isUpdateClient2 = false;
            isReceiveResult1 = false;
            isReceiveResult2 = false;
    }
    
    public void setReceiveResult(String username, boolean status){
        if (username1 == username)
            isReceiveResult1 = status;
        else
            isReceiveResult2 = status;
    }
    
    public void increaseHP(Socket socket, int hp){
        if (client1 == socket){
            HP1 += hp;
            if (HP1 > maxHP1)
                HP1 = maxHP1;
        }
        else { 
            HP2 += hp;
            if (HP2 > maxHP2)
                HP2 = maxHP2;
        }
    }
    
    public Socket getRivalSocket(Socket socket){
        if (client1 == socket)
            return client2;
        else 
            return client1;
    }
    
    public void joinBattle(Socket client, String username, String mascotname, int HP, int attack, int level){
        count++;
        HP2 = HP;
        maxHP2 = HP;
        attack2 = attack;
        client2 = client;
        username2 = username;
        mascotname2 = mascotname;
        level2 = level;
    }
}

class ClientHandle implements Runnable {
    
    private BufferedReader read;
    private PrintStream print;
    public Connect connect;
    private Socket clientSocket;
    private int id;
    private MWServer server;
    private boolean isFinal;
    private int winner;
    private String username;
    private String mascotname;
    private String bossname;
    //check client online
    private long timeout = 0, runtime = 0;
    private final long MAX_TIME_CHECK = 8000;
    private final long MAX_TIME_OUT = 12000;
    public boolean isOnline = true;
    
    
    
    public ClientHandle(Socket clientSocket, int id, MWServer server, Connect connect) {
	this.clientSocket = clientSocket;
	this.id = id;
	this.server = server;
        isFinal = false;
        winner = 0;
        this.connect = connect;
	System.out.println( "Connection " + id + " established with: " + clientSocket);
	try {
	    read = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	    print = new PrintStream(clientSocket.getOutputStream());
	} catch (IOException e) {
	    System.out.println(e);
	}
    }
    
    private void WriteDatatoClient(Socket socket, String command){
       PrintStream os;
        try {
            os = new PrintStream(socket.getOutputStream(), true);
            os.println(command);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void run() {
        String[] lines;
        boolean serverStop = false;
        long timeStart = System.currentTimeMillis();
        Queue<String> commands = new LinkedList<String>();
        int received = 0; 
        
        while (isOnline) {
            try {
                
                if (clientSocket.isClosed())
                    break;
                received = clientSocket.getInputStream().available();
                if (received > 0){
                    
                    String command = read.readLine();
                    commands.add(command);
                    while (!commands.isEmpty()){
                        String line = commands.poll();
                        lines = line.split("##");
                        if (lines[0].equals("008RT") || lines[0].equals("TB")){
                               // System.out.println("008RT from " + id + " runtime " + runtime + " IP = " + clientSocket.getInetAddress().getHostAddress());
                        }
                        else
                            System.out.println("Received " + line + " from Connection " + username + ".");
                        packetHandler(lines);
                    }
                    
                    runtime = 0;
                    timeStart = System.currentTimeMillis();
                    
                } else {
                    if (runtime < MAX_TIME_CHECK){
                        runtime = System.currentTimeMillis() - timeStart;
                        if (runtime >= MAX_TIME_CHECK){
                            print.println("008RQ");
                            //System.out.println("008RQ " + clientSocket.getInetAddress().getHostAddress());
                            timeStart = System.currentTimeMillis();
                        }
                    } else {
                        timeout = System.currentTimeMillis() - timeStart;
                        if (timeout > MAX_TIME_OUT){
                            System.out.println("close socket " + id + " " + username);
                            closeClient();
                            break;
                        }
                    }
                }
            }

            catch (IOException ex) {
                Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void packetHandler(String[] lines) {
        
        if (lines[0].equals("001RQ")){
            username = lines[1];
            String result = connect.checkLogin(lines[1], lines[2]);
            if (!result.equals("001RT##FAIL##User logined from other device") && !result.equals("001RT##FAIL##Account invalid"))
                server.addClient(username, clientSocket);
            System.out.println(result);
            print.println(result);
        } 
        else if (lines[0].equals("001.1RQ")){
            username = lines[1];
            boolean isExist = connect.checkUser(username, lines[2]);
            if (isExist && (connect.getStatus(username) == 0)){
               WriteDatatoClient(clientSocket, "001.1RT##SUCCESS");
               connect.updateStatus(username, 1);
               server.removeClient(username);
               server.addClient(username, clientSocket);
            }
            else
               WriteDatatoClient(clientSocket, "001.1RT##FAIL");
        }
        else
        if (lines[0].equals("002RQ")){
            register(lines);
            
        }
        else
        if (lines[0].equals("003RQ")){
            mascotname = lines[1];
            isFinal = false;
            winner = 0;
            requestBattle(username, mascotname);
        }
        else
        if (lines[0].equals("005RQ")){
            String mascotname = lines[1];
            int attack = Integer.parseInt(lines[2]);
            int defence = Integer.parseInt(lines[3]);
            requestPointRound(username, mascotname, attack, defence);
        }
        else
        if (lines[0].equals("005.1RQ")){
            requestNewRound(username);
        }
        else
        if (lines[0].equals("006RQ")){
            isFinal = true;
            requestResult(username);
        }
        else 
        if (lines[0].equals("008RT")){
        }
        else 
        if (lines[0].equals("009RQ")){
            requestUserInfo(username);
        }
        else 
        if (lines[0].equals("010RQ")){
            String password = lines[1];
            String email = lines[2];
            String avatar = lines[3];
            updateInfo(username, password, email, avatar);
        }
        else 
        if (lines[0].equals("011RQ")){
            topRank();
        }
        else 
        if (lines[0].equals("012RQ")){
            isFinal = false;
            Battle battle = server.getBattle(clientSocket);
            rematch(battle);
        }
        else 
        if (lines[0].equals("012RT")){
            isFinal = false;
            Battle battle = server.getBattle(clientSocket);
            String matrix = createMatrix();
            if (lines[1].equals("YES")){
                int HP1 = getHP(battle.username1, battle.mascotname1);
                int HP2 = getHP(battle.username2, battle.mascotname2);
                WriteDatatoClient(battle.client1, "012RT##YES##" + HP1 + "##" + HP2 + "##" + matrix);
                WriteDatatoClient(battle.client2, "012RT##YES##" + HP2 + "##" + HP1 + "##" + matrix);
                connect.updateBattles(battle.username1);
                connect.updateBattles(battle.username2);
                battle.isRematch1 = false;
                battle.isRematch2 = false;
            } else {
                WriteDatatoClient(battle.client1, "012RT##NO");
                WriteDatatoClient(battle.client2, "012RT##NO");
                server.removeBattle(battle.username1);
            }
        }
        else
        if (lines[0].equals("013RQ")){
            server.removeBattle(username);
        }
        else
        if (lines[0].equals("014RQ")){
            
        }
        else
        if (lines[0].equals("015RQ")){
            String mascotname = lines[1];
            requestMascotInfo(username, mascotname);
        }
        else
        if (lines[0].equals("016RQ")){
            String code = lines[1];
            requestItems(username, mascotname, code);
        }
        else
        if (lines[0].equals("017RQ")){
            String result = connect.requestItemsInfo(username);
            WriteDatatoClient(clientSocket, result);
        }
        else
        if (lines[0].equals("018RQ")){
            String result = connect.usingMascotItem(lines[1], lines[2], Integer.parseInt(lines[3]));
            WriteDatatoClient(clientSocket, result);
        }
        else
        if (lines[0].equals("019RQ")){
            String result = connect.requestMyItemsList(username);
            WriteDatatoClient(clientSocket, result);
            System.out.println(result);
        }
        else
        if (lines[0].equals("020RQ")){
            String itemname = lines[1];
            int value = Integer.parseInt(lines[2]);
            boolean status = connect.updateItems(username, itemname, value);
            if (status)
                WriteDatatoClient(clientSocket, "020RT##SUCCESS##update success");
            else
                WriteDatatoClient(clientSocket, "020RT##FAIL##update fail");
        }
        else
        if (lines[0].equals("021RQ")){ // request boss list
            String result = connect.requestBossList();
            WriteDatatoClient(clientSocket, result);
            System.out.println(result);
        }
        else
        if (lines[0].equals("022RQ")){ // list room
            bossname = lines[1];
            String result = server.getListRoom(bossname);
            WriteDatatoClient(clientSocket, result);
            server.addUserListBoss(username, clientSocket, bossname);
            System.out.println("free" + username);
            System.out.println(result);
            
//            result = server.getListRoom(bossname);
//            server.sendDataToRoomList(bossname, clientSocket, result);
//            System.out.println(result);
        }
        else
        if (lines[0].equals("022.1RQ")){ // list room
            bossname = lines[1];
            String result = server.getListRoom(bossname);
            WriteDatatoClient(clientSocket, result);
            System.out.println("022.1RQ from " + username + ": " + result);
            
//            result = server.getListRoom(bossname);
//            server.sendDataToRoomList(bossname, clientSocket, result);
//            System.out.println(result);
        }
        else
        if (lines[0].equals("023RQ")){ // shop items
            String result = connect.requestItemsList();
            WriteDatatoClient(clientSocket, result);
            System.out.println(result);
        }
        else
        if (lines[0].equals("024RQ")){ // create room
            String mascotname = lines[2];
            bossname = lines[1];
            Room room = server.getRoomFromKey(username);
            if (room != null){
                WriteDatatoClient(clientSocket, "024RT##FAIL");
                return;
            }
            
            room = server.getRoomFromUser(username);
            if (room != null){
                WriteDatatoClient(clientSocket, "024RT##FAIL");
                return;
            }
            
            String result = createRoom(username, mascotname, bossname);
            WriteDatatoClient(clientSocket, result);
            result = server.getListRoom(bossname);
            server.setUserStatusBoss(username, bossname, server.WAITTING);
            server.sendDataToRoomList(bossname, clientSocket, result);
            System.out.println(result);
        }
        else
        if (lines[0].equals("025RQ")){ // join room
            String mascotname = lines[2];
            String keyname = lines[3];
            joinRoom(username, mascotname, bossname, keyname);
            
            String result = server.getListRoom(bossname);
            server.sendDataToRoomList(bossname, clientSocket, result);
            server.setUserStatusBoss(username, bossname, server.WAITTING);
        }
        else
        if (lines[0].equals("026RQ")){ // request point round
            String mascotname = lines[1];
            int attack = Integer.parseInt(lines[2]);
            int defence = Integer.parseInt(lines[3]);
            requestPointBoss(username, mascotname, attack, defence);
        }
        else
        if (lines[0].equals("027RQ")){ // request result attack
            requestFinalPointBoss(username, bossname);
        }
        else
        if (lines[0].equals("028RQ")){ // request start attack boss
            bossname = lines[1];
            Room room = server.getRoomFromKey(username);
            String matrix = createMatrix();
            room.sendData("028RT##" + matrix);
            room.status = room.PLAYING;
            System.out.println(bossname);
            String result = server.getListRoom(bossname);
            System.out.println(result);
            server.sendDataToRoomList(bossname, clientSocket, result);
        }
        else
        if (lines[0].equals("032RQ")){ // exit list room
            bossname = lines[1];
            Room room = server.getRoomFromUser(username);
            if (room == null)
                return;
            //server.sendDataToRoomList(bossname, clientSocket, "032RQ");
            WriteDatatoClient(clientSocket, "032RQ");
            server.removeRoomFromUser(username);
            String result = server.getListRoom(bossname);
            server.sendDataToRoomList(bossname, clientSocket, result);
           
        }
        else
        if (lines[0].equals("033RQ")){ // exit select boos
            bossname = lines[1];
            server.removeRoomFromUser(username);
            server.removeUserListBoss(username, bossname);
            String result = server.getListRoom(bossname);
            server.sendDataToRoomList(bossname, clientSocket, result);
            
        }
        else
        if (lines[0].equals("035RQ")){ // cancel room 
            bossname = lines[1];
            //server.removeRoomFromUser(username);
            Room room = server.getRoomFromUser(username);
            if (room != null){
                room.removeUser(username);
                room.sendData("035RT##" + username);
                String result = server.getListRoom(bossname);
                server.sendDataToRoomList(bossname, clientSocket, result);
            }
            
        }
        else
        if (lines[0].equals("036RQ")){ // request free list
            String result = requestUsersList(bossname);
            WriteDatatoClient(clientSocket, result);
        }
        else
        if (lines[0].equals("037RQ")){ // request free list
            bossname = lines[1];
            String friendname = lines[2];
            Socket friendSocket = server.getSocketFromUser(bossname, friendname);
            WriteDatatoClient(friendSocket, "037RQ##" + username);
            System.out.println("037RQ##" + username + ", " + friendname + ", " + friendSocket.getPort());
        }
        else
        if (lines[0].equals("037RT")){ // request free list
            String rep = lines[4];
            String friendname = lines[3];
            bossname = lines[1];
            mascotname = lines[2];
            Socket friendSocket = server.getSocketFromUser(bossname, friendname);
            Room room = server.getRoomFromUser(friendname);
            if (room != null){
                if (rep.equals("YES") && room.count < 4){
                    joinRoom(username, mascotname, bossname, room.keyname);    
                    String result = server.getListRoom(bossname);
                    server.sendDataToRoomList(bossname, clientSocket, result);
                }
                else 
                    WriteDatatoClient(clientSocket, "037RT##FULL");
            }
        }
    }
    
    public String requestUsersList(String bossname){
        String result = "036RT##";
        List<String> users = server.getFreeUsersListBoss(bossname);
        if (users == null)
            return result;
        for (int index = 0; index < users.size(); index++)
            if (users.get(index) != null)
                result += users.get(index) + "##";
        return result;
    }
    
    public void stopBoss(String username){
        Room room = server.getRoomFromUser(username);
        if (room != null){
            room.removeUser(username);
            room.sendData("034RT##"+username);
            System.out.println("034RT##"+username);
        }
        
    }
    
    public void bonusItem(String username, String bossname){
        if (bossname.equals("Jim")){
            connect.updateItems(username, "COLOR", 2);
            connect.updateItems(username, "TIMES", 1);
            connect.updateItems(username, "HINT", 2);
        } else if (bossname.equals("Toto")){
            connect.updateItems(username, "COLOR", 2);
            connect.updateItems(username, "TIMES", 2);
            connect.updateItems(username, "HINT", 2);
        }
        
    }
    
    public void requestFinalPointBoss(String username, String bossname){
        Room room = server.getRoomFromUser(username);
        
        if (room.HP > 0 && room.bossHP <= 0){
            room.sendData("027RT##WIN");
        }
        else
            room.sendData("027RT##LOSE");
        
        room.updateFinalRound(username);
        if (room.isResult()){
            List<String> users = room.username;
            for (int index = 0; index < users.size(); index++){
                bonusItem(users.get(index), bossname);
                server.setUserStatusBoss(users.get(index), bossname, server.NONE);
            }
            
            server.removeRoomFromUser(username);
            
        }
    }
    
    public String requestPointBoss(String username, String bossname, int attacks, int defence){
        String result = "026RT##";
        Random random = new Random();
        int bossAT = random.nextInt(20) + random.nextInt(60) + 40;
        int bossDefence = random.nextInt(50) + random.nextInt(50) + 70; 
        Room room = server.getRoomFromUser(username);
        room.updatePointRound(attacks, defence, bossAT, bossDefence, username);
        if (room.isUpdate())
            {
                
                result += room.HP + "##" + room.bossHP + "##";
                if (room.attack > bossDefence)
                    room.bossHP -= (room.attack - bossDefence);
                if (room.bossAttack > room.defence)
                    room.HP -= (room.bossAttack - room.defence);
          
                result += room.attack + "##" + room.bossAttack + "##" + room.defence + "##" + bossDefence + "##" + room.HP + "##" + room.bossHP + "##";
                
                room.attack = 0;
                room.defence = 0;
                room.bossAttack = 0;
                room.sendData(result);
                System.out.println(result);
            }    
        return result;
    }
    
    public void joinRoom(String username, String mascotname, String bossname, String keyname){
        String result = "025RT##";
        int HP = getHP(username, mascotname);
        int attack = getAttack(username, mascotname);
        Room keyRoom = server.getRoomFromKey(keyname);
        if (keyRoom == null || keyRoom.getUser(username) != -1){
            WriteDatatoClient(clientSocket, result + "FAIL");
            return;
        }
        Room room = server.joinRoom(clientSocket, username, mascotname, keyname, HP, attack);
        
        
        System.out.println("count = " + room.count);
        int indexKey = room.getUser(keyname);
        int level = connect.getLevel(room.username.get(indexKey), room.mascotname.get(indexKey));
        String avartar = connect.getAvatar(keyname);
        result += room.username.get(indexKey) + "," + level + "," + room.mascotname.get(indexKey) + "," + avartar + "##";
        
        
        for (int index = 0; index < room.username.size(); index++)
            if (index != indexKey){
                System.out.println("index = " + index);
                level = connect.getLevel(room.username.get(index), room.mascotname.get(index));
                avartar = connect.getAvatar(room.username.get(index));
                result += room.username.get(index) + "," + level + "," + room.mascotname.get(index) + "," + avartar + "##";
            }
        room.sendData(result);
        System.out.println(result + "count = " + room.count);
    }
    
    public String createRoom(String username, String mascotname, String bossname){
        String result = "024RT##SUCCESS";
        int HP = getHP(username, mascotname);
        int attack = getAttack(username, mascotname);
        int bossHP = connect.getBossHP(bossname);
        int bossAT = connect.getBossAT(bossname);
        server.createRoom(clientSocket, username, mascotname, bossname, HP, attack,bossHP, bossAT);
        return result;
    }
    
    public void requestItems(String username, String mascotname, String code){
        Battle battle = server.getBattle(clientSocket);
        Socket rivalSocket = battle.getRivalSocket(clientSocket);
        if (code.equals("IT1")){
            battle.increaseHP(clientSocket, 20);
            WriteDatatoClient(rivalSocket, "016RT##" + code);
        } else if (code.equals("IT2")){
            WriteDatatoClient(rivalSocket, "016RT##" + code);
        } else if (code.equals("IT3"))
            connect.updateEXP(username, mascotname, 3);
        else {
            connect.updateBonusItems(username, code, 1);
        }
        
        
    }
    
    public void requestMascotInfo(String username, String mascotname){
        int HPindex = connect.getHPindex(username, mascotname);
        int AttackIndex = connect.getAttackindex(username, mascotname);
        int HP = getHP(username, mascotname);
        int Attack = getAttack(username, mascotname);
        int level = connect.getLevel(username, mascotname);
        int strengthen = connect.getStrengthen(username, mascotname);
        String result = "015RT##" + level + "##" + strengthen + "##" + HP + "##" + Attack + "##" + HPindex + "##" + AttackIndex;
        WriteDatatoClient(clientSocket, result);
    }
    
    public void requestUserInfo(String username){
        String result = connect.requestUserInfo(username);
        print.println(result);
    }
    
    public void updateInfo(String username, String password, String email, String avatar){
        boolean isSuccess = connect.updateUsers(username, password, email, avatar);
        if (isSuccess){
             print.println("010RT##SUCCESS");
        } else 
            print.println("010RT##FAIL");
    }
    
    public void rematch(Battle battle){
        String result = null;
        String matrix = createMatrix();
        int HP1 = getHP(battle.username1, battle.mascotname1);
        int HP2 = getHP(battle.username2, battle.mascotname2);
        int attack1 = getAttack(battle.username1, battle.mascotname1);
        int attack2 = getAttack(battle.username2, battle.mascotname2);
        battle.updateBattle(HP1, attack1, HP2, attack2);
        
        if (battle.client1 == clientSocket){
            battle.isRematch1 = true;
            if (battle.isRematch2){
                WriteDatatoClient(battle.client1, "012RT##YES##" + HP1 + "##" + HP2 + "##" + matrix);
                WriteDatatoClient(battle.client2, "012RT##YES##" + HP2 + "##" + HP1 + "##" + matrix);
                connect.updateBattles(battle.username1);
                connect.updateBattles(battle.username2);
                battle.isRematch1 = false;
                battle.isRematch2 = false;
            }
            else
                WriteDatatoClient(battle.client2, "012RQ");
        } 
        else if (battle.client2 == clientSocket){
            battle.isRematch2 = true;
            if (battle.isRematch1){
                WriteDatatoClient(battle.client1, "012RT##YES##" + HP1 + "##" + HP2 + "##" + matrix);
                WriteDatatoClient(battle.client2, "012RT##YES##" + HP2 + "##" + HP1 + "##" + matrix);
                connect.updateBattles(battle.username1);
                connect.updateBattles(battle.username2);
                battle.isRematch1 = false;
                battle.isRematch2 = false;
            }
       else
           WriteDatatoClient(battle.client1, "012RQ");
        }
    }
    
    public void topRank(){
        String result = connect.getTopRank();
        print.println(result);
    }
    
    private void stopBattle(){
        // send Win/Lose from client
        Battle battle = server.getBattle(clientSocket);
        if (battle == null)
            return;
        if (battle.username1 == null || battle.username2 == null){
            server.removeBattle(username);
            return;
        }
            
        if (isFinal){
            server.removeBattle(username);
            return;
        }
        
        int myRank = connect.getRank(battle.username2);
        int yourRank = connect.getRank(battle.username1);
        int myBattle = connect.getBattle(battle.username2);
        int yourBattle = connect.getBattle(battle.username1);
        String result2 = "006RT##" + myRank + "##" + myBattle + "##" + yourRank + "##" + yourBattle + "##" + "WIN##DISCONNECT" ;
        String result1 = "006RT##" + yourRank + "##" + yourBattle + "##" + myRank + "##" + myBattle + "##" + "WIN##DISCONNECT";
        
        if (battle.client2 == clientSocket){
            PrintStream os = null;
            try {
                int level1 = connect.getLevel(battle.username1, battle.mascotname1);
                connect.updateEXP(battle.username1, battle.mascotname1, 3);
                connect.updateWins(battle.username1);
                if (level1 < connect.getLevel(battle.username1, battle.mascotname1)){
                    String result = "014RT##"+ connect.getLevel(battle.username1, battle.mascotname1);
                    WriteDatatoClient(battle.client1, result);
                }
                os = new PrintStream(battle.client1.getOutputStream());
                os.println(result1);
            } catch (IOException ex) {
                Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                os.close();
            }
            server.removeBattle(battle.username1);
        }
        
        if (battle.client1 == clientSocket){
            PrintStream os = null;
            
            try {
                connect.updateEXP(battle.username2, battle.mascotname2, 3);
                connect.updateWins(battle.username2);
                int level2 = connect.getLevel(battle.username2, battle.mascotname2);
                os = new PrintStream(battle.client2.getOutputStream());
                os.println(result2);
                
                if (level2 < connect.getLevel(battle.username2, battle.mascotname2)){
                    String result = "014RT##" + connect.getLevel(battle.username2, battle.mascotname2);
                    WriteDatatoClient(battle.client2, result);
                }
                
            } catch (IOException ex) {
                Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                os.close();
            }
            server.removeBattle(battle.username2);
        }
    }
    
    public void closeClient(){
        
        // update status client
        {
            connect.updateStatus(username, 0);
            //server.removeRoomFromUser(username);
            server.removeRoomFromKey(username);
            Room room = server.getRoomFromUser(username);
            if (room != null)
                room.removeUser(username);
            if (bossname != null)
                server.removeUserListBoss(username, bossname);
            server.removeClient(username);
            // stop Battle
            stopBattle();
            stopBoss(username);
            // close socket and break thread
            isOnline = false;
        }
        try {
            clientSocket.close();
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
    
    public void register(String[] lines){
        String result = connect.insertData(lines[1], lines[2], lines[3], lines[4]);
        System.out.println(result);
        print.println(result);
    }
    
    private int getHP(String username, String mascotname){
        int HPindex = connect.getHPindex(username, mascotname);
        int level = connect.getLevel(username, mascotname);
        int HP = connect.getHP(username, mascotname);
        return HP + HPindex * level;
    }
    
    private int getAttack(String username, String mascotname){
        int attackindex = connect.getAttackindex(username, mascotname);
        int strengthen = connect.getStrengthen(username, mascotname);
        int attack = connect.getAttack(username, mascotname);
        return attack + attackindex * strengthen;
    }
    
    public void requestBattle(String username, String mascotname){
        String result1 = null;
        String result2 = null;
        
        server.removeBattle(username);

        int HP = getHP(username, mascotname);
        int attack = getAttack(username, mascotname);
        int level = connect.getLevel(username, mascotname);
        
        String matrix = createMatrix();
        
        Battle battle = server.searchBattle(clientSocket, username, mascotname, HP, attack, level);
        if (battle.count == 2 && battle.client1 != null && battle.client2 != null){
            String avatar1 = connect.getAvatar(battle.username1);
            String avatar2 = connect.getAvatar(battle.username2);
            if (username.equalsIgnoreCase(battle.username1)){
                result1 = "003RT##" + battle.username2 + "##" + battle.mascotname2 + "##" + connect.getRank(battle.username2) + "##" + connect.getBattle(battle.username2) + "##" + battle.HP1 + "##" + battle.HP2 + "##" + avatar2 + "##" + matrix;
                result2 = "003RT##" + battle.username1 + "##" + battle.mascotname1 + "##" + connect.getRank(battle.username1) + "##" + connect.getBattle(battle.username1) + "##" + battle.HP2 + "##" + battle.HP1 + "##" + avatar1 + "##" + matrix;

            }
            else {
                result2 = "003RT##" + battle.username1 + "##" + battle.mascotname1 + "##" + connect.getRank(battle.username1) + "##" + connect.getBattle(battle.username1) + "##" + battle.HP2 + "##" + battle.HP1 + "##" + avatar1 + "##" + matrix;
                result1 = "003RT##" + battle.username2 + "##" + battle.mascotname2 + "##" + connect.getRank(battle.username2) + "##" + connect.getBattle(battle.username2) + "##" + battle.HP1 + "##" + battle.HP2 + "##" + avatar2 + "##" + matrix;
            }
            
            try{
                     connect.updateBattles(battle.username1);
                     connect.updateBattles(battle.username2);   
                     PrintStream os1 = new PrintStream(battle.client1.getOutputStream());
                     PrintStream os2 = new PrintStream(battle.client2.getOutputStream());
                     System.out.println(result1);
                     System.out.println(result2);
                     os1.println(result1);
                     os2.println(result2);
                     } catch (IOException e) {
                    System.out.println(e);
                }
        }
            
    }
    
    public void requestPointRound(String username, String mascotname, int attack, int defence){
        Battle battle = server.requestPointRound(username, mascotname, attack, defence);
        if (battle != null)
            if (battle.isUpdateClient1 && battle.isUpdateClient2)
            {
                int remainHP1 = battle.HP1;
                int remainHP2 = battle.HP2;
                if (battle.attack2 > battle.defence1)
                    battle.HP1 -= (battle.attack2 - battle.defence1);
                if (battle.attack1 > battle.defence2)
                    battle.HP2 -= (battle.attack1 - battle.defence2);
                if (battle.HP1 < 0 && battle.HP2 < 0){
                    if ((battle.defence1 + remainHP1) > (battle.defence2 + remainHP2))
                        winner = 1;
                    else 
                        winner = 2;
                }
                
                String result1 = "005RT##" + battle.attack1 + "##" + battle.attack2 + "##" + battle.defence2 + "##" + battle.HP1 + "##" + battle.HP2;
                String result2 = "005RT##" + battle.attack2 + "##" + battle.attack1 + "##" + battle.defence1 + "##" + battle.HP2 + "##" + battle.HP1;
                battle.isUpdateClient1 = false;
                battle.isUpdateClient2 = false;

                try{
                    PrintStream os1 = new PrintStream(battle.client1.getOutputStream());
                    PrintStream os2 = new PrintStream(battle.client2.getOutputStream());
                    os1.println(result1);
                    os2.println(result2);
                    System.out.println(result1);
                    System.out.println(result2);
                } catch (IOException e) {
                    System.out.println(e);
                }

            }    
    }
    
    public void requestNewRound(String username){
        Battle battle = server.requestNewRound(username);
        if (battle != null)
            if (battle.isUpdateClient1 && battle.isUpdateClient2)
            {
                String matrix = createMatrix();
                battle.isUpdateClient1 = false;
                battle.isUpdateClient2 = false;
                try{
                    PrintStream os1 = new PrintStream(battle.client1.getOutputStream());
                    PrintStream os2 = new PrintStream(battle.client2.getOutputStream());
                    os1.println("005.1RT##" + matrix);
                    os2.println("005.1RT##" + matrix);
                } catch (IOException e) {
                    System.out.println(e);
                }

            }
    }
    
    public void requestResult(String username){
        Battle battle = server.requestResultMatch(username);
        battle.setReceiveResult(username, true);
        if (battle.isReceiveResult1 && battle.isReceiveResult2){
            int myRank;
            int yourRank;
            int myBattle;
            int yourBattle;
            int level1;
            int level2;
            String result1 = null;
            String result2 = null;
            
            level1 = connect.getLevel(battle.username1, battle.mascotname1);
            level2 = connect.getLevel(battle.username2, battle.mascotname2);
            if (battle.HP1 <= battle.HP2)
            {
                connect.updateWins(battle.username2);
                connect.updateEXP(battle.username2, battle.mascotname2, 3);
                connect.updateEXP(battle.username1, battle.mascotname1, 1);
                
                myRank = connect.getRank(battle.username2);
                yourRank = connect.getRank(battle.username1);
                myBattle = connect.getBattle(battle.username2);
                yourBattle = connect.getBattle(battle.username1);
                if (winner == 1){
                    result2 = "006RT##" + myRank + "##" + myBattle + "##" + yourRank + "##" + yourBattle + "##" + "LOSE##CONNECT";
                    result1 = "006RT##" + yourRank + "##" + yourBattle + "##" + myRank + "##" + myBattle + "##" + "WIN##CONNECT";
                } else{
                    result2 = "006RT##" + myRank + "##" + myBattle + "##" + yourRank + "##" + yourBattle + "##" + "WIN##CONNECT";
                    result1 = "006RT##" + yourRank + "##" + yourBattle + "##" + myRank + "##" + myBattle + "##" + "LOSE##CONNECT";
                } 

            }
            else {
                connect.updateWins(battle.username1);
                connect.updateEXP(battle.username1, battle.mascotname1, 3);
                connect.updateEXP(battle.username2, battle.mascotname2, 1);

                myRank = connect.getRank(battle.username1);
                yourRank = connect.getRank(battle.username2);
                myBattle = connect.getBattle(battle.username1);
                yourBattle = connect.getBattle(battle.username2);

                if (winner == 2){
                    result1 = "006RT##" + myRank + "##" + myBattle + "##" + yourRank + "##" + yourBattle + "##" + "LOSE##CONNECT";
                    result2 = "006RT##" + yourRank + "##" + yourBattle + "##" + myRank + "##" + myBattle + "##" + "WIN##CONNECT";
                } else {
                    result1 = "006RT##" + myRank + "##" + myBattle + "##" + yourRank + "##" + yourBattle + "##" + "WIN##CONNECT";
                    result2 = "006RT##" + yourRank + "##" + yourBattle + "##" + myRank + "##" + myBattle + "##" + "LOSE##CONNECT";           
                }
            }
            
            if (level1 < connect.getLevel(battle.username1, battle.mascotname1)){
                String result = "014RT##" + (level1 + 1);
                WriteDatatoClient(battle.client1, result);
            }
            
            if (level2 < connect.getLevel(battle.username2, battle.mascotname2)){
                String result = "014RT##" + (level2 + 1);
                WriteDatatoClient(battle.client2, result);
            }
            
            if (winner != 0)
                winner = 0;
            
            battle.isReceiveResult1 = false;
            battle.isReceiveResult2 = false;
            
            try {
                    PrintStream os1 = new PrintStream(battle.client1.getOutputStream());
                    PrintStream os2 = new PrintStream(battle.client2.getOutputStream());
                    os1.println(result1);
                    os2.println(result2);
                    System.out.println(result1 + " " + result2);
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandle.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        
    }
    
    public String createMatrix(){
        int i, j, k;
        int CardNo = 9;
        int[][] CardMatrix = new int[10][10];
        int GameHeight = 8, GameWidth = 8;
        int CardCount[] = new int[CardNo];
        String str = "";
        for (k = 0; k < CardNo; k++)
        {
        	CardCount[k] = 8;           // Thiet lap co 4 the linh vat cho moi linh vat
        }
        Random ran = new Random();
        for (i = 0; i <= GameHeight + 1; i++)
        {
            for (j = 0; j <= GameWidth + 1; j++)
            {
                CardMatrix[i][j] = -1;
            }
        }
        for (i = 1; i <= GameHeight; i++)
        {
            for (j = 1; j <= GameWidth; j++)
            {
            	do
                {
                    k = ran.nextInt(CardNo);
                } 
                while (CardCount[k] == 0);
                CardMatrix[i][j] = k;         
                CardCount[k]--;               
                str += k + ",";
            }
        }  
        return str;
    } 
}

class Connect {
    Connection conn = null;
    Statement st = null;
        
    public Connect(){
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            //Class.forName(com.mysql.jdbc.Driver.class.getName());
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mascotwar?autoReconnect=true", "root", "oneofthem0107");
            //conn = DriverManager.getConnection("jdbc:mysql://localhost:8889/mascotwar", "root", "root");//
            System.out.println(conn.getWarnings());
            if(conn.isClosed()){
                System.out.println("Error get connection!");
                return;
            }
            
            try {
                st = conn.createStatement();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                }

        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void closeDB() throws SQLException {
        st.close();
        conn.close();
    }
    
    public ResultSet getData(String sqlString) {
        ResultSet rs = null;
        try {
                rs = st.executeQuery(sqlString);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return null;
             } 
        return rs;
    } 
    
    private List<String> getMascotName(String username){
        ResultSet rs = null;
        List<String>  results = new ArrayList<String>();
        int count = 0;
        try {
                rs = st.executeQuery("SELECT mascotname FROM mascotsusers WHERE username ='" + username +"'");
                while (rs.next()){
                    results.add(rs.getString(1));
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return null;
             } 
        return results;
    }
    
    public String getTopRank(){
        String str = "011RT##";
        String sql =   "SELECT  username, wins, battles " +
                "FROM    users " +
                "GROUP   BY username " +
                "ORDER   BY wins DESC " +
                "LIMIT   10";
         try {
                ResultSet rs = getData(sql);
                while (rs.next()){
                    String username = rs.getString(1);
                    String wins = rs.getString(2);
                    String battles = rs.getString(3);
                    str += username + "," + wins + "," + battles + "##";
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return str;
    }
    
        public String insertData(String username, String password, String email, String img){
         try {
                ResultSet rs = getData("SELECT username FROM users WHERE username ='" + username + "'");
                if (rs.next())
                    return "002RT##FAIL##Username exist";
                ResultSet rse = getData("SELECT email FROM users WHERE email ='" + email + "'");
                if (rse.next())
                    return "002RT##FAIL##Email exist";
                
                int val = st.executeUpdate("INSERT INTO users(username,password,email,img) VALUES ('" + username + "','" + password + "','" + email+ "','" + img + "')");
                if (val == 1){
                   int num = (int) Math.random()*2;
                   if (num == 1)
                      val = st.executeUpdate("INSERT INTO mascotsusers(username, mascotname) VALUES ('" + username + "','" + "krookodile" + "')" );
                   else 
                      val = st.executeUpdate("INSERT INTO mascotsusers(username, mascotname) VALUES ('" + username + "','" + "kyurem" + "')" ); 
                   return "002RT##SUCCESS";
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             }  
        
        return "002RT##FAIL##Please check connection";
    }
    
    public String usingMascotItem(String username, String itemname, int value){
        try {
            int val1 = st.executeUpdate("UPDATE itemsusers SET value = value - " + value + " WHERE username = '" + username + "' AND itemname ='" + itemname + "'");
            int val2 = st.executeUpdate("UPDATE mascotsusers SET strengthen = strengthen + " + value + " WHERE username = '" + username + "' AND mascotname ='" + itemname + "'");
            if (val1 == 1 && val2 == 1)
                return "018RT##SUCCESS##:)";
        } catch (SQLException ex) {
            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "018RT##FAIL##Please, Check infomation send to server";
    }
    
    public boolean updateItems(String username, String itemname, int value) {
        
        float money = getRemainPrice(username, itemname, value);
        if (money < 0)
            return false;
        
        try {
                int val = st.executeUpdate("UPDATE itemsusers SET value = value + " + value + " WHERE username = '" + username + "' AND itemname ='" + itemname + "'");
                if (val==1){
                    st.executeUpdate("UPDATE users SET money = " + money + " WHERE username = '" + username + "'");
                    return true;
                }
                else {
                    val = st.executeUpdate("INSERT INTO itemsusers(username, itemname, value) VALUES ('" + username + "','" + itemname + "','" + value + "')" );
                    if (val==1){
                        st.executeUpdate("UPDATE users SET money = " + money + " WHERE username = '" + username + "'");
                        return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }
    
    public boolean updateBonusItems(String username, String itemname, int value) {
        
        try {
                int val = st.executeUpdate("UPDATE itemsusers SET value = value + " + value + " WHERE username = '" + username + "' AND itemname ='" + itemname + "'");
                if (val==1)
                    return true;
                else {
                    val = st.executeUpdate("INSERT INTO itemsusers(username, itemname, value) VALUES ('" + username + "','" + itemname + "','" + value + "')" );
                    if (val==1)
                    return true;
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }
    
    public String checkLogin(String username, String password) {
        int status = getStatus(username);
        String mascotName = "";
        List<String> mascots = getMascotName(username);
        mascotName = mascots.remove(0);
        while (!mascots.isEmpty()){
            mascotName += "," + mascots.remove(0);
        }
        System.out.println(username + ": " + status);
        if (status == 1)
            return "001RT##FAIL##User logined from other device";
        try {
                ResultSet rs = getData("SELECT battles, img, wins, money FROM users WHERE username ='" + username + "' AND password ='"+ password + "'");
                if (rs.next()){
                    String battles = rs.getString(1);
                    String img = rs.getString(2);
                    int rank = rs.getInt(3);
                    float money = rs.getFloat(4);
                    rank = getRank(rank);
                    updateStatus(username, 1);
                    System.out.println(username + ": " + getStatus(username));
                    return "001RT##SUCCESS##" + rank + "##" + battles + "##" + img + "##" + money+ "##" + mascotName;
                } 
                else
                    return "001RT##FAIL##Account invalid";
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return "001RT##FAIL##Account invalid";
    }
    
    public boolean checkUser(String username, String password) {
        try {
                ResultSet rs = getData("SELECT * FROM users WHERE username ='" + username + "' AND password ='"+ password + "'");
                if (rs.next()){
                    return true;
                } 
                else
                    return false;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return false;
    }
    
    public boolean checkPrice(String username, String itemname, int num){
        float money = getMoney(username);
        float price = getPrice(itemname);
        if (money >= price * num)
            return true;
        return false;
    }
    
    public float getRemainPrice(String username, String itemname, int num){
        float money = getMoney(username);
        float price = getPrice(itemname);
        return (money - price * num); 
    }
    
    public float getMoney(String username){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT money FROM users WHERE username ='" + username +"'");
                if (rs.next()){
                    return rs.getFloat(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             }
        return 0;
    }
    
    public float getAvartar(String username){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT money FROM users WHERE username ='" + username +"'");
                if (rs.next()){
                    return rs.getFloat(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             }
        return 0;
    }
    public float getPrice(String itemname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT price FROM items WHERE itemname ='" + itemname +"'");
                if (rs.next()){
                    return rs.getFloat(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             }
        return 0;
    }
    
    public int getHP(String username, String mascotname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT HP FROM mascots WHERE mascotname ='" + mascotname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
    public int getAttack(String username, String mascotname){
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT attack FROM mascots WHERE mascotname ='" + mascotname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return 1;
    }
    
    public String requestUserInfo(String username){
        String result = "009RT##FAILT";
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT email, wins, battles FROM users WHERE username ='" + username +"'");
                if (rs.next()){
                    String email = rs.getString(1) ;
                    int wins = rs.getInt(2);
                    String battles = rs.getString(3);
                    int rank = getRank(wins);
                    result = "009RT##" + email + "##" + rank + "##" + wins + "##" + battles;
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return result;
    }
    
    public String requestMyItemsList(String username){
        String result = "019RT##";
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT itemname, value FROM itemsusers WHERE username ='" + username +"'");
                while (rs.next()){
                    String itemname = rs.getString(1) ;
                    int value = rs.getInt(2);
                    result += itemname + "," + value + "##";
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return result;
    }
    
    public String requestBossList(){
        String result = "021RT##";
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT bossname, HP, AT, bonus FROM boss");
                while (rs.next()){
                    String bossname = rs.getString(1) ;
                    String HP = rs.getString(2);
                    String AT = rs.getString(3);
                    String bonus = rs.getString(4);
                    result += bossname + ";" + HP + ";" + AT + ";" + bonus + "##";
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return result;
    }
    
    public String requestItemsList(){
        String result = "023RT##";
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT itemname, price FROM items");
                while (rs.next()){
                    String itemname = rs.getString(1) ;
                    String value = rs.getString(2);
                    result += itemname + "," + value + "##";
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return result;
    }
    
    public String requestItemsInfo(String username){
        String result = "017RT##";
        ResultSet rs;
        try {
                rs = st.executeQuery("SELECT DISTINCT mascotname, strengthen, kind FROM mascotsusers NATURAL JOIN mascots WHERE username ='" + username +"'");
                while (rs.next()){
                    String mascotname = rs.getString(1) ;
                    int strengthen = rs.getInt(2);
                    String kind = rs.getString(3);
                    result += mascotname + "," + kind + "," + strengthen + "##";
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return result;
    }
    
    
    public boolean updateBattles(String username) {
        try {
                int val = st.executeUpdate("UPDATE users SET battles = battles + 1 WHERE username = '" + username + "'");
                if(val==1)
                    return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }

    public boolean updateStatus(String username, int status){
        try {
                int val = st.executeUpdate("UPDATE users SET status = '" + status + "' WHERE username = '" + username + "'");
                if(val==1)
                    return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return false;
    }
    
    public void updateStatus(){
        try {
                st.executeUpdate("UPDATE users SET status = 0");
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
    }
    
    public boolean updateWins(String username) {
        try {
                int val = st.executeUpdate("UPDATE users SET wins = wins + 1 WHERE username = '" + username + "'");
                if (val==1)
                    return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }
    
    public boolean updateUsers(String username, String password, String email, String img) {
        try {
                int val = st.executeUpdate("UPDATE users SET password = '" + password + "',email = '" + email + "',img = '" + img + "'WHERE username = '" + username + "'");
                if(val==1)
                    return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }
    
    public boolean updateEXP(String username, String mascotname, int score) {
        try {
                int val = st.executeUpdate("UPDATE mascotsusers SET EXP = EXP + " + score + " WHERE username = '" + username + "' AND mascotname ='" + mascotname + "'");
                if (val==1)
                    return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        
        return false;
    }
    
    public int getBossHP(String bossname){
        try {
                ResultSet rs = st.executeQuery("SELECT HP FROM boss WHERE bossname ='" + bossname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
    public int getBossAT(String bossname){
        try {
                ResultSet rs = st.executeQuery("SELECT AT FROM boss WHERE bossname ='" + bossname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
    public int getHPindex(String username, String mascotname){
        try {
                ResultSet rs = st.executeQuery("SELECT HPindex FROM mascots WHERE mascotname ='" + mascotname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
    public int getAttackindex(String username, String mascotname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT attackindex FROM mascots WHERE mascotname ='" + mascotname +"'");
                if (rs.next()){
                    return rs.getInt(1);
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
    public String getKindMascot(String mascotname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT kind FROM mascots WHERE mascotname ='" + mascotname +"'");
                if (rs.next()){
                   String kind = rs.getString(1);
                   if (rs != null)
                       rs.close();
                   return kind;
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return null;
    }
    
    public int getLevel(int EXP){
        if (EXP <= 5) return 1;
        else if (EXP <= 13) return 2;
        else if (EXP <= 30) return 3;
        else if (EXP <= 49) return 4;
        else if (EXP <= 79) return 5;
        else if (EXP <= 150) return 6;
        else if (EXP <= 251) return 7;
        else if (EXP <= 500) return 8;
        else if (EXP <= 1000) return 9;
        else if (EXP <= 2000) return 10;
        else return 11;
    }
        
    public int getLevel(String username, String mascotname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT EXP FROM mascotsusers WHERE mascotname ='" + mascotname + "' AND username ='"+ username + "'");
                if (rs.next()){
                    return getLevel(rs.getInt(1));
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    
       public int getStrenglen(int items){
        if (items <= 1) return 1;
        else if (items <= 2) return 2;
        else if (items <= 3) return 3;
        else if (items <= 4) return 4;
        else if (items <= 5) return 5;
        else if (items <= 7) return 6;
        else if (items <= 9) return 7;
        else if (items <= 11) return 8;
        else if (items <= 13) return 9;
        else if (items <= 15) return 10;
        else return 11;
    }
        
    public int getStrengthen(String username, String mascotname){
        ResultSet rs = null;
        try {
                rs = st.executeQuery("SELECT strengthen FROM mascotsusers WHERE mascotname ='" + mascotname + "' AND username ='"+ username + "'");
                if (rs.next()){
                    return getLevel(rs.getInt(1));
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                
             } 
        return 1;
    }
    

    public int getStatus(String username){
        try {
                ResultSet rs = getData("SELECT status FROM users WHERE username = '" + username + "'");
                if(rs.next())
                    return rs.getInt(1);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return 0;
    }
    
    public String getAvatar(String username){
        try {
                ResultSet rs = getData("SELECT img FROM  users  WHERE username = '" + username + "'");
                if(rs.next())
                    return rs.getString(1);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return null;
    }
    
    public int getBattle(String username) {
        try {
                ResultSet rs = getData("SELECT battles FROM users WHERE username ='" + username + "'");
                if (rs.next()){
                    int battles = rs.getInt(1);
                    return battles;
                } 
                else return 0;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return 0;
    }
    
    public int getRank(String username) {
        int rank = 0;
        try {
                ResultSet rs = getData("SELECT wins FROM users WHERE username ='" + username + "'");
                if (rs.next()){
                    rank = getRank(rs.getInt(1));
                } 
                else rank = 0;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return rank;
    }
       
    public int getRank(int wins){
        String sql = "SELECT COUNT(*) FROM users WHERE (wins > " + wins + ")"; 
         try {
                ResultSet rs = getData(sql);
                if (rs.next()){
                    return rs.getInt(1) + 1;
                } 
            } catch (SQLException e) {
                System.err.println(e.getMessage());
             } 
        return 0;
    }
    
    private void closeConnect() {
        try {
                conn.close();
        } catch (SQLException e) {
              System.err.println(e.getMessage());
          } 
    }
    
    private boolean isClosed() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    private Statement createStatement() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

}










