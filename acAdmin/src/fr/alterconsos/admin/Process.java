package fr.alterconsos.admin;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

import com.google.gson.Gson;

public class Process extends Thread {
	private static Process procP;
	private static Process procT;
	private static Process procD;

	public static String toSHA1(String convertme) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			return toHexString(md.digest(convertme.getBytes("UTF-8")));
		} catch (Exception ex) {
			return null;
		}
	}

	public static String toHexString(byte[] buf) {
		char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
				'e', 'f' };
		StringBuffer strBuf = new StringBuffer(buf.length * 2);
		for (int i = 0; i < buf.length; i++) {
			strBuf.append(hexChar[(buf[i] & 0xf0) >>> 4]);
			strBuf.append(hexChar[buf[i] & 0x0f]);
		}
		return strBuf.toString();
	}

	public static Process get(String ptd) {
		if ("P".equals(ptd))
			return procP;
		if ("T".equals(ptd))
			return procT;
		return procD;
	}

	String ptd;
	boolean stop = false;
	Main.Run run;
	Main.Filtre filtre;
	String pwd;
	String url;
	String[] lignes;
	String header;
	long size;
	
	public Process(String ptd, Main.Run run, Main.Filtre filtre, String pwd, String url)
			throws Exception {
		if (get(ptd) != null)
			throw new Bridge.AppEx("Process " + ptd + " déjà en exécution");
		this.ptd = ptd;
		this.run = run;
		this.pwd = toSHA1(pwd + "00");
		this.url = url;
		if (!this.url.endsWith("/"))
			this.url += "/";
		if ("P".equals(ptd))
			procP = this;
		if ("T".equals(ptd))
			procT = this;
		if ("D".equals(ptd))
			procD = this;
		this.filtre = filtre;
		run.pause = false;
		run.err = null;
		Main.config().updRun(run);
		this.start();
	}

	public void stopIt() throws Exception {
		stop = true;
	}

	public void run() {
		try {
			if (run.encours != 3)
				sauvegarde();
			else
				restauration();
			if (!stop)
				Main.config().closeRun(run);
			else {
				run.pause = true;
				try {
					Main.config().updRun(run);
				} catch (Exception e1) {
					Bridge.ex(e1);
				}
			}			
			int vol = (int) (run.totalSize / 1000000);
			int volk = (int) (run.totalSize / 1000);
			String x = run.totalSize == 0 ? "0o" : 
				(vol == 0 ? (volk == 0 ? run.totalSize + "o" : (volk + "Ko") ) : vol + "Mo");
			String msg = (run.encours == 1 || run.encours == 2 ? "Sauvegarde " : "Restauration ") + ptd 
					+ (stop ? " mise en pause. " : " terminée avec succès. ") 
					+ run.nbc + "/" + run.nbt + " ligne(s). Volume total : " + x;
			if (run.encours == 3)
				msg += ". Nombre de cellules : " + run.cells + ". Nombre d'items : " + run.nodes + ".";
			Bridge.log(msg);
		} catch (Exception e) {
			run.err = Bridge.exc(e);
			run.pause = true;
			try {
				Main.config().updRun(run);
			} catch (Exception e1) {
				Bridge.ex(e1);
			}
		}
		if ("P".equals(ptd))
			procP = null;
		if ("T".equals(ptd))
			procT = null;
		if ("D".equals(ptd))
			procD = null;
	}

	public void sauvegarde() throws Exception {
		if (stop)
			return;
		if (run.phase == 0) {
			lignes = listLines();
			if (lignes.length == 0)
				return;
			run.totalSize = 0;
			run.nbt = lignes.length;
			run.nbc = 0;
			run.phase = 1;
			Main.config().updRun(run);
		} else {
			lignes = new JsonFile<String[]>(run.path + run.nom + "/lignes.json", String[].class)
					.get();
		}
		while (run.nbc < run.nbt) {
			if (stop)
				return;
			String ligne = lignes[run.nbc];
			dump(ligne);
			run.totalSize += this.size;
			run.nbc++;
			Main.config().updRun(run);
		}
	}

	public void restauration() throws Exception {
		if (stop)
			return;
		lignes = new JsonFile<String[]>(run.path + run.nom + "/lignes.json", String[].class)
				.get();
		if (run.phase == 0) {
			if (lignes.length == 0)
				return;
			run.totalSize = 0;
			run.nbt = lignes.length;
			run.nbc = 0;
			run.phase = 1;
			Main.config().updRun(run);
		}
		while (run.nbc < run.nbt) {
			if (stop)
				return;
			String ligne = lignes[run.nbc];
			Status status = load(ligne);
			run.totalSize += status.bytes;
			run.cells += status.cells;
			run.nodes += status.nodes;
			run.nbc++;
			Main.config().updRun(run);
		}
	}

	private String[] listLines() throws Exception {
		byte[] respb = post(setArgs("linesS", filtre != null ? filtre.lignes : null, 
				null, filtre != null ? filtre.version : 0, null), null, false);
		String resp = new String(respb, "UTF-8");
		if (!resp.startsWith("$")) {
			new JsonFile<String[]>(run.path + run.nom + "/lignes.json", null).set(resp);
			return new Gson().fromJson(resp, String[].class);
		} else {
			throw new Bridge.AppEx(resp);
		}
	}

	private byte[] setArgs(String op, String l, String c, long v, String t)
			throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"at\":-1,\"ad\":0,\"ap\":\"").append(pwd).append("\",\"op\":\"").append(op)
				.append("\"");
		if (l != null && !"".equals(l))
			sb.append(",\"l\":\"").append(l).append("\"");
		if (c != null && !"".equals(c))
			sb.append(",\"l\":\"").append(c).append("\"");
		if (t != null && !"".equals(t))
			sb.append(",\"l\":\"").append(t).append("\"");
		if (v != 0)
			sb.append(",\"v\":\"").append("" + v).append("\"");
		sb.append("}\n");
		return sb.toString().getBytes("UTF-8");
	}

	private byte[] post(byte[] args, byte[] file, boolean hasHeader) throws Exception {
		URLConnection connection;
		try {
			connection = new URL(url + "dumpload").openConnection();
		} catch (Exception e) {
			throw new Bridge.AppEx("Connexion au serveur impossible");
		}
		connection.setDoOutput(true);
		OutputStream os = connection.getOutputStream();
		os.write(args);
		if (file != null)
			os.write(file);
		os.close();
		InputStream is = connection.getInputStream();
		byte[] buf = new byte[4096];
		ByteArrayOutputStream os2 = new ByteArrayOutputStream(16192);
		if (hasHeader) {
			StringBuffer sb = new StringBuffer();
			int b;
			while ( (b = is.read()) > 0)
				sb.append((char)b);
			this.header = sb.toString();
			if (this.header.startsWith("$"))
				throw new Bridge.AppEx(this.header);
		}
		int l = 0;
		while ((l = is.read(buf)) > 0)
			os2.write(buf, 0, l);
		is.close();
		byte[] res = os2.toByteArray();
		os2.close();
		return res;
	}

	private void dump(String ligne) throws Exception {
		byte[] resp = post(setArgs("dumpS", ligne, 
				filtre != null ? filtre.colonnes : null, 
				filtre != null ? filtre.version : 0, 
				filtre != null ? filtre.types : null), null, true);
		this.size = resp.length - this.header.length();
		File zf = new File(run.path + run.nom + "/.temp.zip");
		FileOutputStream zos = new FileOutputStream(zf);
		zos.write(resp);
		zos.close();
		zf.renameTo(new File(run.path + run.nom + "/" + ligne + "_" + this.header + ".zip"));
	}

	private Status load(String ligne) throws Exception {
		byte[] file = getBytes(run.path, run.nom, ligne);
		byte[] resp = post(setArgs("load", ligne, null, 0, null), file, false);
		String text = new String(resp, "UTF-8");
		if (text.startsWith("$"))
			throw new Bridge.AppEx(text);
		Status status = new Gson().fromJson(text, Status.class);
		return status;
	}
	
	private static class Status {
		int bytes;
		int cells;
		int nodes;
	}

	private byte[] getBytes(String path, String nom, String ligne) throws IOException {
		File d = new File(path + "/" + nom);
		for(File f : d.listFiles()){
			if (f.isFile()){
				String n = f.getName();
				if (n.startsWith(ligne + "_") && n.endsWith(".zip")){
					int l = (int) f.length();
					if (l != 0) {
						BufferedInputStream is = new BufferedInputStream(
								new FileInputStream(f));
						byte[] buf = new byte[l];
						is.read(buf);
						is.close();
						return buf;
					} else
						return new byte[0];
				}
			}
		}
		return new byte[0];
	}

}
