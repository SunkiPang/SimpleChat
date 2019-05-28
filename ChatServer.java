import java.net.*;
import java.io.*;
import java.util.*;


/*




*/




public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10006);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;

	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){

				//System.out.println(line);
				if(line.equals("/quit"))
					break;
				if(check_msg(line)){
					if(line.indexOf("/to ") == 0){
						sendmsg(line);
					}else if(line.equals("/userlist")){
						send_userlist();
					}else
						broadcast(id + " : " + line);
				}
				else{
					sendmsg("bad word!!");
				}

			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg
	public void send_userlist(){
		broadcast("--------user list--------");

		Iterator <String> iter = hm.keySet().iterator();
		while(iter.hasNext()){
			String keys = (String)iter.next();
			//System.out.println(keys);
			broadcast(keys);
		}
	}//send_userlist


	public void broadcast(String msg){
		synchronized(hm){
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			PrintWriter obj = (PrintWriter)hm.get(id);
			while(iter.hasNext()){

				//System.out.println(iter.next());
				PrintWriter pw = (PrintWriter)iter.next();
				if(obj == pw){
					continue;
				}

				pw.println(msg);
				pw.flush();
			}
		}
	} // broadcast

	public boolean check_msg(String msg){


		String[] bad_String = {"bad1","bad2","bad3","bad4","bad5"};
		int flag = 1;
		for(int i = 0; i < bad_String.length; i++){
			if(msg.indexOf(bad_String[i]) != -1){
				flag = 0;
				PrintWriter pw = (PrintWriter)hm.get(id);
				pw.println(bad_String[i] + " is bad word!!");
				pw.flush();
			}
		}
		if(flag == 1)
			return true;
		else
			return false;
	}




}
