package fr.alterconsos.admin;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Main implements IMain {
	public static final Main main = new Main();

	public void onStart() {
		System.out.println("Browser connecté");
	}

	public void onEnd() {
		System.out.println("Browser déconnecté");
		config().save();
	}

	public void beforeInit() {

	}

	public static class Personne implements IEvent {
		String nom;
		int age;

		public void process() {
			String s = nom + " - " + age;
			Bridge.err(s);
			Bridge.send(this);
		}
	}

	public Config config() {
		return (Config) Bridge.AConfig.get(Config.class);
	}

	public static class Config extends Bridge.AConfig {
		String dir = "D:/acAdmin";
		String pwdP = "";
		String pwdT = "1234";
		String pwdD = "1234";
		String urlP = "https://alterconsos.appspot.com/";
		String urlT = "http://192.168.0.1/";
		String urlD = "http://192.168.0.1:8080/";
	}

	public static class Personnes implements IEvent {
		ArrayList<Personne> list;

		@Override
		public void process() {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < list.size(); i++) {
				Personne p = list.get(i);
				String s = i + " / " + p.nom + " - " + p.age;
				sb.append(s + "\n");
			}
			Bridge.log(sb.toString());
			Bridge.send(this);
		}
	}

	public static void main(String[] args) {
		try {
			int port = 8887;
			if (args.length > 0) {
				try {
					port = Integer.parseInt(args[0]);
				} catch (Exception e) {
				}
			}
			Event.register(Personne.class);
			Event.register(Personnes.class,
					new TypeToken<ArrayList<Personne>>() {
					}.getType());
			System.out.println("Config :\n" + new Gson().toJson(main.config()));
			main.beforeInit();
			Bridge.start(main, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
