package fr.alterconsos.admin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.Gson;

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
		Event.register(PauseDump.class);
		Event.register(RepriseDump.class);
		Event.register(AbandonDump.class);
		Event.register(NewRest.class);
		Event.register(OnOff.class);

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
		String ptd;
		int encours; // 1: svg partielle, 2: svg, 3: rest, 0:fermeture
		int phase = 0; // 1: initiale 2:lignes 
		boolean pause = false;
		String path;
		String nom;
		int nbc = 0;
		int nbt = 0;
		String err;
		int size = 0;
		int cells = 0;
		int nodes = 0;
		long totalSize = 0;
	}

	public static JsonFile<Config> fconfig;
	
	public static Config config() throws Exception{
		if (fconfig == null)
			fconfig = new JsonFile<Config>("~.acAdmin.json", Config.class);
		return fconfig.get();
	}
	
	public class CloseRun {
		String ptd;
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
			} else 
				return runD;
		}

		public String url(String ptd){
			if ("P".equals(ptd)) {
				return urlP;
			} else if ("T".equals(ptd)) {
				return urlT;
			} else 
				return urlD;
		}

		public String pwd(String ptd){
			if ("P".equals(ptd)) {
				return pwdP;
			} else if ("T".equals(ptd)) {
				return pwdT;
			} else 
				return pwdD;
		}

		public void run(Run arg){
			if ("P".equals(arg.ptd)) {
				runP = arg;
			} else if ("T".equals(arg.ptd)) {
				runT = arg;
			} else
				runD = arg;
		}

		public synchronized Config closeRun(Run arg)  throws Exception{
			if (arg == null)
				return this;
			Run run = run(arg.ptd);
			if (run == null)
				return this;
			run.encours = 0;
			Bridge.send(run);		
			if ("P".equals(arg.ptd)) {
				runP = null;;
			} else if ("T".equals(arg.ptd)) {
				runT = null;
			} else 
				runD = null;
			fconfig.set(this);
			return this;						
		}
		
		public synchronized Config updRun(Run arg)  throws Exception{
			if (arg == null)
				return this;
			Run run = run(arg.ptd);
			if (run == null)
				run = new Run();
			run.ptd = arg.ptd;
			run.encours = arg.encours;
			run.path = arg.path;
			run.nom = arg.nom;
			run.nbc = arg.nbc;
			run.nbt = arg.nbt;
			run.cells = arg.cells;
			run.nodes = arg.nodes;
			run.phase = arg.phase;
			run.pause = arg.pause;
			run.totalSize = arg.totalSize;
			run.err = arg.err;
			run.size = arg.size;
			run(run);
			fconfig.set(this);
			Bridge.send(run);
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
	
	public static class NewRest implements IEvent {
		String path;
		String ptd;
		String nom;
		
		@Override
		public Object process() throws Exception {
			Config cfg = config();
			FS fs = new FS(cfg.dir);
			String path = fs.path();
			if (!path.equals(cfg.dir))
				cfg.chgDir(path);
			Run run = cfg.run(ptd);
			if (run != null)
				throw new Bridge.AppEx("Sauvegarde / restauration en cours pour le serveur " + ptd);
			run = new Run();
			run.path = path;
			run.encours = 3;
			run.nom = nom;
			run.nbc = 0;
			run.nbt = 0;
			run.phase = 0;
			run.ptd = ptd;
			run.err = null;
			run.totalSize = 0;
			cfg.updRun(run);
			new Process(ptd, run, null, cfg.pwd(ptd), cfg.url(ptd));
			return null;
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
				throw new Bridge.AppEx("Sauvegarde / restauration en cours pour le serveur " + ptd);
			String nom = ptd + sdf1.format(new Date()) + (filtre != null ? "P" : "C");
			fs.newDir(nom);
			if (filtre != null)
				new JsonFile<Filtre>(path + nom + "/filtre.json", Filtre.class).set(filtre);
			run = new Run();
			run.path = path;
			run.encours = filtre != null ? 1 : 2;
			run.nom = nom;
			run.nbc = 0;
			run.nbt = 0;
			run.phase = 0;
			run.ptd = ptd;
			run.err = null;
			run.totalSize = 0;
			cfg.updRun(run);
			new Process(ptd, run, filtre, cfg.pwd(ptd), cfg.url(ptd));
			return null;
		}
	}

	public static class PauseDump implements IEvent {
		String ptd;
		
		@Override
		public Object process() throws Exception {
			Process p = Process.get(ptd);
			if (p != null)
				p.stopIt();
			else {
				Config cfg = config();
				Run run = cfg.run(ptd);
				if (run != null) {
					run.pause = true;
					try {
						Main.config().updRun(run);
					} catch (Exception e1) {
						Bridge.ex(e1);
					}
				}
			}
			return null;
		}
	}

	public static class RepriseDump implements IEvent {
		String ptd;
		
		@Override
		public Object process() throws Exception {
			Config cfg = config();
			Run run = cfg.run(ptd);
			if (run == null)
				throw new Bridge.AppEx("Reprise de sauvegarde / restauration impossible (pas en cours) pour le serveur " + ptd);
			Process p = Process.get(ptd);
			if (p != null)
				throw new Bridge.AppEx("Reprise de sauvegarde / restauration impossible (déjà en exécution) pour le serveur " + ptd);
			Filtre filtre = null;
			if (run.nom.endsWith("P"))
				filtre = new JsonFile<Filtre>(run.path + run.nom + "/filtre.json", Filtre.class).get();
			run.pause = false;
			cfg.updRun(run);
			new Process(ptd, run, filtre, cfg.pwd(ptd), cfg.url(ptd));
			return null;
		}
	}

	public static class AbandonDump implements IEvent {
		String ptd;
		
		@Override
		public Object process() throws Exception {
			Config cfg = config();
			Run run = cfg.run(ptd);
			if (run == null)
				throw new Bridge.AppEx("Abandon de sauvegarde / restauration impossible (pas encours) pour le serveur " + ptd);
			Process p = Process.get(ptd);
			if (p != null)
				throw new Bridge.AppEx("Abandon de sauvegarde / restauration impossible (en exécution) pour le serveur " + ptd);
			if (run.encours != 3) {
				File af = new File(run.path + run.nom);
				File nf = new File(run.path + run.nom.substring(0, run.nom.length() -1) + "A");
				af.renameTo(nf);
			}
			cfg.closeRun(run);
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

	public static class OnOff implements IEvent {
		boolean onoff;
		String ptd;

		@Override
		public Object process() throws Exception {
			Config cfg = config();
			String ret = Process.onOff(onoff, cfg.pwd(ptd), cfg.url(ptd));
			Bridge.log(ret, ret.startsWith("$"));
			return null;
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
