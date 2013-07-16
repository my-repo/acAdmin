package fr.alterconsos.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Main implements IMain {
	public static final Main main = new Main();

	public static final TimeZone timezone = TimeZone.getTimeZone("Europe/Paris");

	public static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss",
			Locale.FRANCE);

	static {
		sdf1.setTimeZone(timezone);
	}
	
	public void onStart() {
		System.out.println("Browser connecté");
	}

	public void onEnd() {
		System.out.println("Browser déconnecté");
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
		Event.register(ReqFiltre.class);
		Event.register(Filtre.class);
		Event.register(NewDump.class);

		// Event.register(Personnes.class, new TypeToken<ArrayList<Personne>>() {}.getType());

		Config cfg = config();
		FS fs = new FS(cfg.dir);
		String path = fs.path();
		if (!path.equals(cfg.dir)) {
			cfg.dir = path;
			fconfig.set(cfg);
		}
		System.out.println("Config :\n" + new Gson().toJson(config()));
	}

	public static class Run {
		String encours;
		int phase = 0;
		String path;
		String nom;
		int nbc = 0;
		int nbt = 0;
		String exception;
		long totalSize = 0;
	}

	public static JsonFile<Config> fconfig;
	
	public static Config config() throws Exception{
		if (fconfig == null)
			fconfig = new JsonFile<Config>("~.acAdmin.json", Config.class);
		return fconfig.get();
	}
	
	public class Config {
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
		
		public Run run(String ptd){
			if ("P".equals(ptd)) {
				return runP;
			} else if ("T".equals(ptd)) {
				return runT;
			} else if ("D".equals(ptd)) {
				return runD;
			}
			return null;
		}

		public String url(String ptd){
			if ("P".equals(ptd)) {
				return urlP;
			} else if ("T".equals(ptd)) {
				return urlT;
			} else if ("D".equals(ptd)) {
				return urlD;
			}
			return null;
		}

		public String pwd(String ptd){
			if ("P".equals(ptd)) {
				return pwdP;
			} else if ("T".equals(ptd)) {
				return pwdT;
			} else if ("D".equals(ptd)) {
				return pwdD;
			}
			return null;
		}

		public void run(String ptd, Run arg){
			if ("P".equals(ptd)) {
				runP = arg;
			} else if ("T".equals(ptd)) {
				runT = arg;
			} else if ("D".equals(ptd)) {
				runD = arg;
			}
		}
		
		public synchronized Config updRun(String ptd, Run arg)  throws Exception{
			if (arg == null)
				
				return this;
			Run run = run(ptd);
			if (run == null)
				run = new Run();
			if (arg.encours != null)
				run.encours = arg.encours;
			if (arg.path != null)
				run.path = arg.path;
			if (arg.nom != null)
				run.nom = arg.nom;
			if (arg.nbc != -1)
				run.nbc = arg.nbc;
			if (arg.nbt != -1)
				run.nbt = arg.nbt;
			if (arg.phase != -1)
				run.phase = arg.phase;
			if (arg.totalSize != -1)
				run.totalSize = arg.totalSize;
			if (arg.exception != null)
				run.exception = arg.exception;
			run(ptd, run);
			fconfig.set(this);
			return this;			
		}
		
		public synchronized Config chgDir(String dir) throws Exception{
			this.dir = dir;
			fconfig.set(this);
			return this;
		}
		
		public synchronized Config chgSrv(String ptd, String url, String pwd)  throws Exception{
			if ("P".equals(ptd)) {
				this.urlP = url;
				this.pwdP = pwd;
			} else if ("T".equals(ptd)) {
				this.urlT = url;
				this.pwdT = pwd;
			} else if ("D".equals(ptd)) {
				this.urlD = url;
				this.pwdD = pwd;
			}
			fconfig.set(this);
			return this;
		}
	}

	public static class UpdConfigDir implements IEvent {
		String dir;

		@Override
		public Object process() throws Exception {
			return config().chgDir(new FS(dir).path());
		}
	}

	public static class UpdConfigNewDir implements IEvent {
		String newdir;

		@Override
		public Object process() throws Exception {
			Config cfg = config();
			return cfg.chgDir(new FS(cfg.dir).newDir(newdir).path());
		}
	}

	public static class UpdConfigUrl implements IEvent {
		String url;
		String pwd;
		String ptd;

		@Override
		public Object process() throws Exception {
			return config().chgSrv(ptd, url, pwd);
		}
	}

	public static class ReqConfig implements IEvent {

		@Override
		public Object process() throws Exception {
			Config cfg = config();
			String path = new FS(cfg.dir).path();
			if (!path.equals(cfg.dir))
				cfg.chgDir(path);
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
			Dirs d = new Dirs();
			d.dirs = new FS(config().dir).subdirs();
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
			Dump d = new Dump();
			d.lines = new FS(path + dump).listZips();
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
			Line l = new Line();
			l.cols = Zip.getZip(parent + "/" + this.zip).cols();
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
			ZipText l = new ZipText();
			l.text = Zip.getZip(null).get(col);
			return l;
		}
	}
	
	
	public static class NewDump implements IEvent {
		String path;
		String ptd;
		Filtre filtre;
		
		@Override
		public Object process() throws Exception {
			if (filtre != null && filtre.isVide())
				filtre = null;
			Config cfg = config();
			FS fs = new FS(cfg.dir);
			String path = fs.path();
			if (!path.equals(cfg.dir))
				cfg.chgDir(path);
			Run run = cfg.run(ptd);
			if (run != null)
				throw new Exception("Sauvegarde / restauration en cours pour le serveur " + ptd);
			String nom = ptd + sdf1.format(new Date()) + (filtre != null ? "P" : "C");
			fs.newDir(nom);
			if (filtre != null)
				new JsonFile<Filtre>(path + nom + "/filtre.json", Filtre.class).set(filtre);
			run = new Run();
			run.path = path;
			run.encours = "S" + (filtre != null ? "P" : "C") + "0";
			run.nom = nom;
			run.nbc = 0;
			run.nbt = 0;
			run.phase = 0;
			cfg.updRun(ptd, run);
			new Process(ptd, run, filtre, cfg.pwd(ptd), cfg.url(ptd));
			return null;
		}
	}
	
	public static class Filtre {
		long version;
		String lignes;
		String colonnes;
		String types;
		
		public boolean isVide(){
			return (version == 0 && 
					(lignes == null || "".equals(lignes)) &&
					(colonnes == null || "".equals(colonnes)) &&
					(types == null || "".equals(types)));
		}
	}

	public static class ReqFiltre implements IEvent {
		String path;

		@Override
		public Object process() throws Exception {
			return new JsonFile<Filtre>(path + "/filtre.json", Filtre.class).get();
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
