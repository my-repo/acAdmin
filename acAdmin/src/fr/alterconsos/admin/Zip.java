package fr.alterconsos.admin;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class Zip {

	private static Zip c = null;
	
	public static Zip getZip(String path){
		if (c != null && c.path.equals(path) || path == null)
			return c;
		c = new Zip(path);
		return c;
	}
	
	private String path = null;
	
	private String[] cols = new String[0];
	
	private Zip(String path){
		this.path = path;
		ArrayList<String> lst = new ArrayList<String>();
		try {
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(path)));
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				lst.add(ze.getName());
			}
			zis.close();
			cols = lst.toArray(new String[lst.size()]);
		} catch (Exception e) {	}
	}
	
	public String get(String col){
		boolean found = false;
		for(String s : cols)
			if (s.equals(col)){
				found = true;
				break;
			}
		if (!found)
			return "";
		try {
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(path)));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String name = ze.getName();
				if (!name.equals(col))
					continue;
				bos.reset();
				int l = 0;
				while ((l = zis.read(buf, 0, 4096)) > 0)
					bos.write(buf, 0, l);
				byte[] content = bos.toByteArray();
				zis.close();
				if (!name.endsWith("json"))
					return Base64.encode(content);
				else
					return new String(content, "UTF-8");
			}
			zis.close();
			return "";
		} catch (Exception e) {
			return "";
		}
	}
	
	public String[] cols(){
		return cols;
	}
	
}
