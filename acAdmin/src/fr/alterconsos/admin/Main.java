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

		// Event.register(Personnes.class, new TypeToken<ArrayList<Personne>>() {}.getType());

		Config cfg = config();
		FS fs = new FS(cfg.dir);
		String path = fs.path();
		if (!path.equals(cfg.dir)) {
			cfg.dir = path;
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

	public static class Run {
		String encours;
		String path;
		int nblignes;
		String exception;
	}

	public static class Config extends Bridge.AConfig {
		String dir = "D:/temp";
		String pwdP = "";
		String pwdT = "1234";
		String pwdD = "1234";
		String urlP = "https://alterconsos.appspot.com/";
		String urlT = "http://192.168.0.1/";
		String urlD = "http://192.168.0.1:8080/";
		Run runP;
		Run runT;
		Run runD;
	}

	public static class UpdConfigDir implements IEvent {
		String dir;

		@Override
		public Object process() throws Exception {
			Config cfg = main.config();
			FS fs = new FS(dir);
			cfg.dir = fs.path();
			cfg.save();
			return cfg;
		}
	}

	public static class UpdConfigNewDir implements IEvent {
		String newdir;

		@Override
		public Object process() throws Exception {
			Config cfg = main.config();
			FS fs = new FS(cfg.dir);
			fs.newDir(newdir);
			cfg.dir = fs.path();
			cfg.save();
			return cfg;
		}
	}

	public static class UpdConfigUrl implements IEvent {
		String url;
		String pwd;
		String ptd;

		@Override
		public Object process() throws Exception {
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
			cfg.save();
			return cfg;
		}
	}

	public static class ReqConfig implements IEvent {

		@Override
		public Object process() throws Exception {
			Config cfg = main.config();
			FS fs = new FS(cfg.dir);
			String path = fs.path();
			if (!path.equals(cfg.dir)) {
				cfg.dir = path;
				cfg.save();
			}
			return cfg;
		}
	}

	public static class Dirs {
		int callId;
		String[] dirs;
	}

	public static class ReqConfigDirs implements IEvent {
		int callId;

		@Override
		public Object process() throws Exception {
			Config cfg = main.config();
			FS fs = new FS(cfg.dir);
			Dirs d = new Dirs();
			d.dirs = fs.subdirs();
			d.callId = callId;
			return d;
		}
	}

	public static class Dump {
		String[] lines;
	}

	public static class SetCurrentDump implements IEvent {
		String path;
		String dump;

		@Override
		public Object process() throws Exception {
			FS fs = new FS(path + dump);
			Dump d = new Dump();
			d.lines = fs.listZips();
			return d;
		}
	}

	public static class Line {
		int callId;
		String[] cols;
	}

	public static class SetCurrentZip implements IEvent {
		String parent;
		String zip;

		@Override
		public Object process() throws Exception {
			Zip zip = Zip.getZip(parent + "/" + this.zip);
			Line l = new Line();
			l.cols = zip.cols();
			return l;
		}
	}

	public static class ZipText {
		String text;
	}

	public static class GetFromZip implements IEvent {
		String col;

		@Override
		public Object process() throws Exception {
			Zip zip = Zip.getZip(null);
			ZipText l = new ZipText();
			l.text = zip.get(col);
			return l;
		}
	}

	/*******************************************************/

	// public static class Personne {
	// String nom;
	// int age;
	// }
	//
	// public static class Personnes implements IEvent {
	// ArrayList<Personne> list;
	//
	// @Override
	// public Object process() throws Exception {
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < list.size(); i++) {
	// Personne p = list.get(i);
	// String s = i + " / " + p.nom + " - " + p.age;
	// sb.append(s + "\n");
	// }
	// Bridge.log(sb.toString());
	// return this;
	// }
	// }

	/*******************************************************/

	public static void main(String[] args) {
		try {
			int port = 8887;
			main.beforeInit();
			Bridge.start(main, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
