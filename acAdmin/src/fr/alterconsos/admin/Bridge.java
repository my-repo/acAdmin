package fr.alterconsos.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Bridge extends WebSocketServer {
	private static WebSocket theConn = null;
	private static IMain theMain;
	private static Bridge theBridge;

	public static class AppEx extends Exception {
		private static final long serialVersionUID = 1L;

		public AppEx(String message){
			super(message);
		}
	}
	
	public static String exc(Exception e){
		if (e == null)
			return "Exception";
		if (e.getClass() == AppEx.class)
			return e.getMessage();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(bos));
		try {
			return bos.toString("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			return e.toString();
		}
	}
	
	public Bridge(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public Bridge(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		theConn = conn;
		theMain.onStart();
		// System.out.println(
		// conn.getRemoteSocketAddress().getAddress().getHostAddress() +
		// " entered the room!" );
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		theMain.onEnd();
		System.out.println("Fin de session");
		System.exit(0);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// conn.send("reçu : [" + message + "]");
		// Event.process(message);
		
		Event<?> event;
		try {
			event = Event.get(message);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		if (event == null)
			return;
		Object obj;
		try {
			if (event.data != null)
				obj = event.data.process();
			else
				obj = Event.get(event).process(); // List
			if (obj == null)
				sendAck(event.callId);
			else
				send(event.callId, obj);
		} catch (Exception e){
			sendEx(event.callId, e);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null && ex != null) {
			ex.printStackTrace();
		}
	}

	public static class ErrMsg {
		int callId;
		String message;
	}
	
	public static void sendEx(int callId, Exception e){
		ErrMsg m = new ErrMsg();
		m.callId = callId;
		m.message = Bridge.exc(e);
		theConn.send(Event.serial(m));
	}

	public static class Ack {
		int callId;
		String message;
	}

	public static void sendAck(int callId){
		Ack m = new Ack();
		m.callId = callId;
		theConn.send(Event.serial(m));
	}

	public static void send(int callId, Object data) {
		String s = Event.serial(data);
		String m = "{\"callId\":" + callId + ", \"data\":" + s + "}";
		theConn.send(m);
	}

	public static void send(Object data) {
		if (data != null) {
			String s = Event.serial(data);
			String m = "{\"type\":\"" + data.getClass().getSimpleName() + "\", \"data\":" + s + "}";
			theConn.send(m);
		}
	}

	public static void ex(Exception e) {
		e.printStackTrace();
		log(exc(e), true);
	}

	public static void err(String text) {
		log(text, true);
	}

	public static void log(String text) {
		log(text, false);
	}

	public static void log(String text, boolean err) {
		String s = Event.serial(text);
		if (s != null && s.length() != 0)
			theConn.send("{\"type\":\"" + (err ? "err" : "log")
					+ "\", \"data\":" + s + "}");
	}

	public static void start(IMain main, int port) throws InterruptedException,
			IOException {
		theMain = main;
		WebSocketImpl.DEBUG = false;
		theBridge = new Bridge(port);
		theBridge.start();
		String url = "file:///"
				+ new java.io.File(".").getCanonicalPath().replace('\\', '/')
				+ "/war/app.html?" + port;
		String x = System.getProperty("user.home")
				+ "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe  --app=";
		// String x = "rundll32 url.dll,FileProtocolHandler ";
		Runtime.getRuntime().exec(x + url);
		System.out.println("Début de session, port: " + theBridge.getPort());
	}

}
