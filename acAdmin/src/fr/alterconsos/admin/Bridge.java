package fr.alterconsos.admin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

public class Bridge extends WebSocketServer {
	private static WebSocket theConn = null;
	private static IMain theMain;
	private static Bridge theBridge;
	
	public Bridge( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
	}

	public Bridge( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		theConn = conn;
		theMain.onStart();
		// System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		theMain.onEnd();
		System.out.println("Fin de session");
		Config.get().save();
		System.exit(0);
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		conn.send("reçu : [" + message + "]");
		if ("w".equals(message)) {
			try { Thread.sleep(2000); } catch (InterruptedException e) { }
			conn.send("plus tard !!!");
		}
		Event.process(message);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if(conn != null && ex != null) {
			ex.printStackTrace();
		}
	}
	
	public static void send(IEvent data){
		String s = Event.serial(data);
		if (s != null)
			theConn.send(s);
	}

	public static void err(String text){
		log(text, true);
	}
	
	public static void log(String text){
		log(text, false);	}

	public static void log(String text, boolean err){
		String s = Event.serial(text);
		if (s != null && s.length() != 0)
			theConn.send("{\"type\":\"" + (err ? "err" : "log") + "\", \"data\":" + s + "}");
	}

	public static class Config {
		public static Config config = null;
		
		String dir = "D:/acAdmin";
		String[] lines = new String[0];
		String pwd = "23452345";
		
		public static Config get(){
			if (config != null)
				return config;
			try {
				File f = new File(System.getProperty("user.home") + "\\.acAdmin.json");
				int l = f.exists() && f.isFile() ? (int)f.length() : 0;
				if (l != 0) {
					BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
					byte[] buf = new byte[l];
					is.read(buf);
					is.close();
					config = new Gson().fromJson(new String(buf, "UTF-8"), Config.class);
					return config;
				}
			} catch (Exception e){ }
			config = new Config();
			config.save();
			return config;
		}
		
		public void save() {
			try {
				File f = new File(System.getProperty("user.home") + "\\.acAdmin.json");
				Gson gson = new Gson();
				String content = gson.toJson(this);
				byte[] buf = content.getBytes("UTF-8");
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
				os.write(buf);
				os.close();
			} catch (Exception e){ }
		}
	}
	
	public static void start(IMain main) throws InterruptedException , IOException {
		theMain = main;
		WebSocketImpl.DEBUG = false;
		int port = 8887; // 843 flash policy port
		theBridge = new Bridge(port);
		theBridge.start();
		String url = "file:///" + new java.io.File(".").getCanonicalPath().replace('\\', '/') + "/war/test.html";
		String x = System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe  --app=";
		// String x = "rundll32 url.dll,FileProtocolHandler ";
        Runtime.getRuntime().exec(x + url);     
		System.out.println("Début de session, port: " + theBridge.getPort());
		System.out.println("Config :\n" + new Gson().toJson(Config.get()));
	}

}
