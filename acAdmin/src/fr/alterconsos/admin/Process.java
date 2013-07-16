package fr.alterconsos.admin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
		if ("D".equals(ptd))
			return procD;
		return null;
	}

	String ptd;
	boolean stop = false;
	Main.Run run;
	Main.Filtre filtre;
	String pwd;
	String url;
	String[] lignes;
	String header;
	int size;

	public Process(String ptd, Main.Run run, Main.Filtre filtre, String pwd, String url)
			throws Exception {
		if (get(ptd) != null)
			throw new Exception("Process " + ptd + " déjà en exécution");
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
		this.start();
	}

	public static class ProcessInfo {
		String ptd;
		String message;
		int type;
		int size;
		int index;
		long totalSize;
	}

	public void save() throws Exception {
		if (stop)
			throw new Exception("Exécution mise en pause sur demande");
		Main.Config cfg = Main.config();
		cfg.updRun(ptd, run);
	}

	public void stopIt() {
		stop = true;
	}

	public void run() {
		try {
			if (run.encours.startsWith("S"))
				sauvegarde();
			else
				restauration();
		} catch (Exception e) {
			ProcessInfo pi = new ProcessInfo();
			pi.type = -1;
			pi.message = Bridge.exc(e);
			pi.ptd = ptd;
			Bridge.send(pi);
		}
	}

	public void close() {
		ProcessInfo pi = new ProcessInfo();
		pi.type = 9;
		pi.message = "Sauvegarde terminée avec succès";
		pi.ptd = ptd;
		Bridge.send(pi);
	}

	public void sauvegarde() throws Exception {
		if (run.phase == 0) {
			lignes = listLines();
			run.totalSize = 0;
			run.nbt = lignes.length;
			run.nbc = 0;
			run.phase = run.nbt == 0 ? 9 : 1;
			save();
			ProcessInfo pi = new ProcessInfo();
			pi.type = 1;
			pi.size = lignes.length;
			pi.index = 0;
			pi.totalSize = run.totalSize;
			pi.message = lignes.length + " lignes à sauvegarder";
			pi.ptd = ptd;
			Bridge.send(pi);
		} else {
			lignes = new JsonFile<String[]>(run.path + run.nom + "/lignes.json", String[].class)
					.get();
		}
		while (run.nbc < run.nbt) {
			String ligne = lignes[run.nbc];
			dump(ligne);
			run.totalSize += this.size;
			int size = (this.size / 1000);
			if (size == 0 && this.size != 0)
				size = 1;
			save();
			ProcessInfo pi = new ProcessInfo();
			pi.type = 2;
			pi.totalSize = run.totalSize;
			pi.size = size;
			pi.index = run.nbc + 1;
			pi.message = "Ligne : " + ligne + " (" + size + "Ko)";
			pi.ptd = ptd;
			Bridge.send(pi);
			run.nbc++;
		}
		ProcessInfo pi = new ProcessInfo();
		pi.type = 9;
		pi.message = "Sauvegarde terminée avec succès";
		pi.ptd = ptd;
		Bridge.send(pi);
		return;
	}

	private String[] listLines() throws Exception {
		byte[] respb = post(setArgs("linesS", filtre.lignes, null, filtre.version, null), 0);
		String resp = new String(respb, "UTF-8");
		if (!resp.startsWith("$")) {
			new JsonFile<String[]>(run.path + run.nom + "/lignes.json", null).set(resp);
			return new Gson().fromJson(resp, String[].class);
		} else {
			throw new Exception(resp + "\n");
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

	private byte[] post(byte[] args, int header) throws Exception {
		URLConnection connection = new URL(url + "dumpload").openConnection();
		connection.setDoOutput(true);
		OutputStream os = connection.getOutputStream();
		os.write(args);
		os.close();
		InputStream is = connection.getInputStream();
		byte[] buf = new byte[4096];
		ByteArrayOutputStream os2 = new ByteArrayOutputStream(16192);
		if (header != 0) {
			byte[] pfx = new byte[header];
			is.read(pfx);
			this.header = new String(pfx, "UTF-8");
			if (this.header.startsWith("$"))
				throw new Exception(this.header);
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
		byte[] resp = post(setArgs("dumpS", ligne, filtre.colonnes, filtre.version, filtre.types),
				17);
		this.size = resp.length - 17;
		File zf = new File(run.path + run.nom + "/.temp.zip");
		FileOutputStream zos = new FileOutputStream(zf);
		zos.write(resp);
		zos.close();
		zf.renameTo(new File(run.path + run.nom + "/" + ligne + "_" + this.header + ".zip"));
	}

	public void restauration() throws Exception {

	}

}
