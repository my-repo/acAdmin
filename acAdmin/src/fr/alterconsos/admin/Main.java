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
		try {
			config().save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void beforeInit() throws Exception {
		Event.register(ReqConfig.class);
		Event.register(UpdConfigUrl.class);
		Event.register(ReqConfigDirs.class);
		Event.register(UpdConfigDir.class);
		Event.register(UpdConfigNewDir.class);
		Event.register(SetCurrentDump.class);
		Event.register(SetCurrentZip.class);
		Event.register(GetFromZip.class);
		
//		Event.register(Personne.class);
//		Event.register(Personnes.class,
//				new TypeToken<ArrayList<Personne>>() {
//				}.getType());
		
		Config cfg = config();
		FS fs = new FS(cfg.dir);
		if (fs.dir().length != cfg.dir.length){
			cfg.dir = fs.dir();
			cfg.save();
		}
		System.out.println("Config :\n" + new Gson().toJson(config()));
	}

	public Config config() throws Exception {
		try {
			return (Config) Bridge.AConfig.get(Config.class);
		} catch (Exception e) {
			throw e;
		}
	}

	public static class Config extends Bridge.AConfig {
		int callId = 0;
		String[] dir = { "D:", "temp", "dump1", "titi" };
		String pwdP = "";
		String pwdT = "1234";
		String pwdD = "1234";
		String urlP = "https://alterconsos.appspot.com/";
		String urlT = "http://192.168.0.1/";
		String urlD = "http://192.168.0.1:8080/";
		
	}

	public static class UpdConfigDir implements IEvent {
		int callId;
		String[] dir;

		@Override
		public void process() {	
			try {
				Config cfg = main.config();
				FS fs = new FS(dir);
				cfg.dir = fs.dir();
				cfg.callId = callId;
				cfg.save();
				Bridge.send(cfg);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class UpdConfigNewDir implements IEvent {
		int callId;
		String newdir;

		@Override
		public void process() {	
			try {
				Config cfg = main.config();
				FS fs = new FS(cfg.dir);
				fs.newDir(newdir);
				cfg.dir = fs.dir();
				cfg.callId = callId;
				cfg.save();
				Bridge.send(cfg);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class UpdConfigUrl implements IEvent {
		int callId;
		String url;
		String pwd;
		String ptd;

		@Override
		public void process() {
			try {
				Config cfg = main.config();
				if ("P".equals(ptd)) {
					cfg.urlP = url;
					cfg.pwdP = pwd;
				} else if ("T".equals(ptd)) {
					cfg.urlT = url;
					cfg.pwdT = pwd;
				} else if ("D".equals(ptd)) {
					cfg.urlD = url;
					cfg.pwdD = pwd;
				}
				cfg.callId = callId;
				cfg.save();
				Bridge.send(cfg);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class ReqConfig implements IEvent {
		int callId;

		@Override
		public void process() {
			// Bridge.sendEx(callId, new Exception("Pour tester une erreur de chargement de la configuration"));
			try {
				Config cfg = main.config();
				cfg.callId = callId;
				FS fs = new FS(cfg.dir);
				String[] dx = fs.dir();
				if (dx.length != cfg.dir.length) {
					cfg.dir = dx;
					cfg.save();
				}
				Bridge.send(cfg);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class Dirs {
		int callId;
		String[] dirs;
	}
	
	public static class ReqConfigDirs implements IEvent {
		int callId;

		@Override
		public void process() {
			try {
				Config cfg = main.config();
				FS fs = new FS(cfg.dir);
				Dirs d = new Dirs();
				d.dirs = fs.subdirs();
				d.callId = callId;
				Bridge.send(d);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class Dump {
		int callId;
		String[] lines;
	}
	
	public static class SetCurrentDump implements IEvent {
		int callId;
		String path;
		String dump;
		
		@Override
		public void process() {
			try {
				FS fs = new FS(path + dump);
				Dump d = new Dump();
				d.lines = fs.listZips();
				d.callId = callId;
				Bridge.send(d);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class Line {
		int callId;
		String[] cols;
	}
	
	public static class SetCurrentZip implements IEvent {
		int callId;
		String parent;
		String zip;
		
		@Override
		public void process() {
			try {
				Zip zip = Zip.getZip(parent + "/" + this.zip);
				Line l = new Line();
				l.cols = zip.cols();
				l.callId = callId;
				Bridge.send(l);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	public static class ZipText {
		int callId;
		String text;
	}
	
	public static class GetFromZip implements IEvent {
		int callId;
		String col;
		
		@Override
		public void process() {
			try {
				Zip zip = Zip.getZip(null);
				ZipText l = new ZipText();
				l.text = zip.get(col);
				l.callId = callId;
				Bridge.send(l);
			} catch (Exception e) {
				Bridge.sendEx(callId, e);
			}
		}
	}

	/*******************************************************/
	
//	public static class Personne implements IEvent {
//		String nom;
//		int age;
//
//		public void process() {
//			String s = nom + " - " + age;
//			Bridge.err(s);
//			Bridge.send(this);
//		}
//	}
//
//	public static class Personnes implements IEvent {
//		ArrayList<Personne> list;
//
//		@Override
//		public void process() {
//			StringBuffer sb = new StringBuffer();
//			for (int i = 0; i < list.size(); i++) {
//				Personne p = list.get(i);
//				String s = i + " / " + p.nom + " - " + p.age;
//				sb.append(s + "\n");
//			}
//			Bridge.log(sb.toString());
//			Bridge.send(this);
//		}
//	}

	/*******************************************************/

	public static void main(String[] args) {
		try {
			int port = 8887;
			if (args.length > 0) {
				try {
					port = Integer.parseInt(args[0]);
				} catch (Exception e) {
				}
			}
			main.beforeInit();
			Bridge.start(main, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
