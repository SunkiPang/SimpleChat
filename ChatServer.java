import java.net.*;
import java.io.*;
import java.util.*;


/*

github 주소
https://github.com/SunkiPang/SimpleChat

- 교수님 코멘트
현재 접속한 사용자 목록 보기 기능
채팅 문장으로 “/userlist” 를 보내면 현재 접속한 사용자들의 id 및 총 사용자 수를 보여준다. 메소드 : send_userlist()
자신이 보낸 채팅 문장은 자신에게는 나타나지 않도록 할 것
금지어 경고 기능
서버에 금지어 목록을 미리 등록함 (5개 이상)
채팅 문장에 금지어가 포함되어 있으면 다른 사용자에게 전송하지 않고, 해당 사용자에게만 적절한 경고 메시지를 보낸다.

<선기>
- userlist 기능은 hm에 저장된 key값을 broadcast하자.
- 자신에게 채팅문자가 나타나지 않는 기능은 broadcast 과정에서 if 문으로 검사하여 아이디가 같으면 continue 하는 것으로 하자.
- 금지어는 리스트를 만들어 검사하고 검사해서 금지어가 쓰여져 있다면 보내지 broadcast하지 않고 사용자에게만 경고 메시지를 보내자.

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
				//사용자가 입력한 글에 금지어가 있는지 확인
				if(check_msg(line)){ //check_msg에서 true가 나와야 아래의 문장들을 실행
					if(line.indexOf("/to ") == 0){
						sendmsg(line);
					}else if(line.equals("/userlist")){
						send_userlist();
					}else
						broadcast(id + " : " + line);
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

		/*
		userlist 보내주기
		hm에 저장된 id들을 받아서 출력하자
		1. PrintWriter를 생성해서 그 곳에 hm의 id를 받는다.
		2. while문을 돌려서 입력한 사용자에게 출력해준다.
		3. broadcast가 본인이 입력한 것은 되돌려주지 않기 때문에 pw을 이용해서 출력해준다.
		*/

		PrintWriter pw = (PrintWriter)hm.get(id);
		Iterator <String> iter = hm.keySet().iterator();
		pw.println("--------user list--------");
		while(iter.hasNext()){
			String keys = (String)iter.next();
			//System.out.println(keys);

			pw.println(keys);
			pw.flush();

			//broadcast(keys);
		}
	}//send_userlist


	public void broadcast(String msg){
		synchronized(hm){
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			PrintWriter obj = (PrintWriter)hm.get(id); //id만을 받을 obj.
			while(iter.hasNext()){
				//System.out.println(iter.next());
				PrintWriter pw = (PrintWriter)iter.next();
				//만약 현재 broadcast하려는 id가 보낸 사용자의 id와 같으면
				//continue를 통해 메시지가 보내지지 않게 함.
				if(obj == pw){
					continue;
				}

				pw.println(msg);
				pw.flush();
			}
		}
	} // broadcast

	public boolean check_msg(String msg){ //메시지에 금지단어가 섞여있는지 확인하는 메소드.

		/*
		1. bad_String 어레이에서 단어의 숫자만큼 반복.
		2. -1이 아닌 경우 문장에 단어가 포함되어 있다는 뜻.
		3. -1이 아닌 숫자가 나왔을 때 flag를 내려서 false를 리턴하게 만듦.
		4. 또한 그 사용자에게만 메시지를 보낸다.
		5. 금지어를 포함해서 PrintWriter로 그 사용자에게 보내줌.
		6. 그 후 run에서 if문을 만족시켜야 하기 때문에 flag의 여부에 따라 true와 false로 return값을 준다.
		*/

		String[] bad_String = {"bad1","bad2","bad3","bad4","bad5"}; // 금지어 리스트
		int flag = 1; //금지어의 포함여부를 보여줄 flag

		for(int i = 0; i < bad_String.length; i++){
			if(msg.indexOf(bad_String[i]) != -1){ //문장에 금지어가 포함됐을 때
				flag = 0;
				PrintWriter pw = (PrintWriter)hm.get(id); //사용자의 id로 PrintWriter 생성
				pw.println("Don't use " + bad_String[i]); //사용자에게 금지어가 포함 된 경고문 출력.
				pw.flush();
			}
		}
		if(flag == 1)
			return true;
		else
			return false;
	}




}
