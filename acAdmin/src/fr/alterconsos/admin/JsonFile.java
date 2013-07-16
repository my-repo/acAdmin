package fr.alterconsos.admin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.google.gson.Gson;

public class JsonFile<T> {

	private File file;
	private Gson gson = new Gson();
	private T content;
	private String scontent;
	
	@SuppressWarnings("unchecked")
	public JsonFile(String path, Class<?> clazz) throws Exception {
		if (path == null || path.length() < 2)
			throw new Exception("Path incorrect");
		String p = path;
		if (p.startsWith("~")) {
			p = System.getProperty("user.home") + "\\" + p.substring(1);
		}
		file = new File(p);
		int l = file.exists() && file.isFile() ? (int) file.length() : 0;
		if (l != 0) {
			BufferedInputStream is = new BufferedInputStream(
					new FileInputStream(file));
			byte[] buf = new byte[l];
			is.read(buf);
			is.close();
			scontent = new String(buf, "UTF-8");
		} else
			scontent = clazz == null ? "" : (clazz.isArray() ? "[]" : "{}");
		if (clazz != null)
			content = (T) gson.fromJson(scontent, clazz);
	}
	
	public T get(){
		return content;
	}

	public void set(String content) throws Exception{
		this.scontent = content;
		save();
	}

	public void set(T content) throws Exception{
		this.content = content;
		this.scontent = gson.toJson(content);
		save();
	}

	private void save() throws Exception{
		byte[] buf = scontent.getBytes("UTF-8");
		BufferedOutputStream os = new BufferedOutputStream(
				new FileOutputStream(file));
		os.write(buf);
		os.close();		
	}
}
