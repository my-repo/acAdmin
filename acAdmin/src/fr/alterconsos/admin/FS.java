package fr.alterconsos.admin;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class FS {
	
	public static String[] path2Strings(String path){
		String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
		return p.split("/");
	}
	
	private static FileFilter filter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};

	private static FileFilter filterZip = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if (pathname.isFile()){
				String n = pathname.getName();
				if (!n.endsWith(".zip") || n.length() < 23)
					return false;
				int i = n.indexOf("_");
				if (i == -1 || i + 22 != n.length())
					return false;
				return true;
			}
			return false;
		}
	};

	private File[] dir;
	private File[] subdirs;
	
	public int dirSize(){
		return dir.length;
	}

	public int subdirsSize(){
		return subdirs.length;
	}

	public String[] dir(){
		String[] d = new String[dir.length];
		for(int i = 0; i < dir.length; i++)
			if (i > 0)
				d[i] = dir[i].getName();
			else {
				String s = dir[i].getPath();
				d[i] = s.substring(0, s.length() - 1);				
			}
		return d;
	}
	
	public String path(){
		String[] d = dir();
		StringBuffer sb = new StringBuffer();
		for(String s : d)
			sb.append(s).append("/");
		return sb.toString();
	}
	
	public String[] subdirs(){
		String[] d = new String[subdirs.length];
		for(int i = 0; i < subdirs.length; i++) {
			if (dir.length != 0)
				d[i] = subdirs[i].getName();
			else {
				String s = subdirs[i].getPath();
				d[i] = s.substring(0, s.length() - 1);
			}
		}
		return d;
	}

	public FS(String path){
		this(path2Strings(path));
	}
	
	public FS(String[] dir){
		if (dir == null || dir.length == 0) {
			this.dir = new File[0];
			this.subdirs = getRoots();
			return;
		}
		ArrayList<File> lstd = new ArrayList<File>();
		File p = null;
		int i = 0;
		boolean ok = true;
		while(i < dir.length && ok) {
			String s = dir[i];
			if (i == 0 && !(dir[0].endsWith("/") || dir[0].endsWith("\\")))
				s += "/";
			File d = new File(p, s);
			if (d.isDirectory()) {
				p = d;
				lstd.add(d);
				i++;
			} else
				ok = false;
		}
		if (lstd.size() == 0) {
			this.dir = new File[0];
			this.subdirs = getRoots();
			return;			
		}
		this.dir = lstd.toArray(new File[lstd.size()]);
		this.subdirs = p.listFiles(filter);
	}
	
	public String[] listZips(){
		if (this.dirSize() == 0)
			return new String[0];
		File owner = this.dir[this.dirSize() - 1];
		File[] lf = owner.listFiles(filterZip);
		String[] r = new String[lf.length];
		for(int i = 0; i < lf.length; i++) {
			String s = lf[i].getName();
			r[i] = s.substring(0, s.length() - 4);
		}
		return r;
	}
	
	private static File[] getRoots() {
		File[] roots = File.listRoots();
		ArrayList<File> lst = new ArrayList<File>();
		for (File f : roots)
			if (f.isDirectory())
				lst.add(f);
		return lst.toArray(new File[lst.size()]);
	}
		
	public void newDir(String name) throws Exception{
		if (name == null || name.length() == 0)
			throw new Exception("Nom absent");
		File d = new File(dir[dir.length - 1], name);
		if (d.isFile())
			throw new Exception("Un fichier avec ce nom existe déjà");
		if (!d.exists() && !d.mkdir())
			throw new Exception("Création refusée");
		File[] nd = new File[dir.length + 1];
		for(int i = 0; i < dir.length; i++)
			nd[i] = dir[i];
		nd[dir.length] = d;
		this.dir = nd;
		this.subdirs = d.listFiles(filter);
	}
	
	/*
	private FS print(){
		System.out.print(">>");
		for(String s : dir())
			System.out.print(s + "/");
		System.out.println("<<");
		for(String s : subdirs())
			System.out.println(" " + s);
		return this;
	}
	
	public static void main(String[] args) {
		FS fs1 = new FS(null).print();
		String[] d2 = {"D:"};
		FS fs2 = new FS(d2).print();
		String[] d3 = {"D:", "temp", "dump1"};
		FS fs3 = new FS(d3).print();
		String[] d4 = {"D:", "temp", "dump1", "toto"};
		FS fs4 = new FS(d4).print();
		fs4.newDir("titi");
		fs4.print();
		String[] d5 = {"K:", "temp", "dump1"};
		FS fs5 = new FS(d5).print();
		
	}
	*/
}
