package fr.alterconsos.admin;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.reflect.TypeToken;

public class Main implements IMain {
	public static final Main main = new Main();

	public void onStart(){
		System.out.println("Browser connected");
	}
	
	public void onEnd(){
		System.out.println("Browser disconnected");		
	}

	public void beforeInit(){
		
	}
	
	public static class Personne implements IEvent{
		String nom;
		int age;
		
		public void process(){
			String s = nom + " - " + age;
			Bridge.err(s);
			Bridge.send(this);
		}
	}

	public static class Personnes implements IEvent{
		ArrayList<Personne> list;
		public void process(){
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i < list.size(); i++){
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
			Event.register("Personne", Personne.class);
			Event.register("Personnes", Personnes.class, new TypeToken<Collection<Personne>>(){}.getType());
			Bridge.Config.get();
			main.beforeInit();
			Bridge.start(main);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
