package fr.alterconsos.admin;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class Event<T> {
	private static Hashtable<String, Class<?>> types = new Hashtable<String, Class<?>>();

	private static Hashtable<String, Type> ctypes = new Hashtable<String, Type>();

	private static Gson gson;
	
	static {
		GsonBuilder g = new GsonBuilder();
		g.registerTypeAdapter(Event.class, new EventDeserializer());
		gson = g.create();
	}

	public static void register(String type, Class<?> clazz){
		types.put(type,  clazz);
	}

	public static void register(String type, Class<?> clazz, Type colType){
		types.put(type,  clazz);
		ctypes.put(type,  colType);
	}

	@SuppressWarnings("rawtypes")
	public static class EventDeserializer implements JsonDeserializer<Event> {
		
		@SuppressWarnings("unchecked")
		public Event deserialize(JsonElement jsonElement, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			JsonObject event = (JsonObject)jsonElement;
			String type = event.get("type").getAsString();
			JsonElement eltData = event.get("data");
			Class<?> clazz = types.get(type);
			if (clazz == null)
				return null;
			if (eltData != null && eltData.isJsonObject()) {			
				return new Event(type, (IEvent)context.deserialize(eltData, clazz));
			}
			if (eltData != null && eltData.isJsonArray()) {
				Type t = ctypes.get(type);
				if (t != null) {
					return new Event(type, (ArrayList)context.deserialize(eltData, t));
				}
			}
			return null;
		}
		
	}

	public static String serial(String data){
		return gson.toJson(data, String.class);
	}
	
	public static String serial(Object data){
		if (data == null)
			return null;
		String type = data.getClass().getSimpleName();
		Class<?> clazz = types.get(type);
		if (clazz == null)
			return null;
		String x;
		if (ctypes.get(type) == null) {
			x = gson.toJson(data);
		} else {
			try {
				Field f = clazz.getDeclaredField("list");
				x = gson.toJson(f.get(data));
			} catch (Exception e) {
				return null;
			}
		}
		StringBuffer sb = new StringBuffer();
		sb.append("{\"type\":\"").append(type).append("\", \"data\":").append(x).append("}");
		return sb.toString();
	}

	public static void process(String json){
		Event<?> event = gson.fromJson(json, Event.class);
		if (event == null)
			return;
		if (event.data != null)
			event.data.process();
		else {
			Class<?> clazz = types.get(event.type);
			try {
				IEvent obj = (IEvent)(clazz.newInstance());
				Field f = clazz.getDeclaredField("list");
				f.set(obj, event.col);
				obj.process();
			} catch (Exception e) {}
		}
	}
	
	public String type;
	public IEvent data;
	public ArrayList<?> col;
	public Event(String type, IEvent data){
		this.type = type;
		this.data = data;
	}
	
	public Event(String type, ArrayList<?> data){
		this.type = type;
		this.col = data;
	}

//	public static void main(String[] args){
//		try {
//			Event.register("Personne", Main.Personne.class);
//			Event.register("Personnes", Main.Personnes.class, new TypeToken<Collection<Main.Personne>>(){}.getType());
//			String x1 = "{\"nom\"=\"Sportes\", \"age\"=63}";
//			String x2 = "{\"nom\"=\"Colin\", \"age\"=62}";
//			String x = "{\"type\"=\"Personne\", \"data\"=" + x1 + "}";
//			String y = "{\"type\"=\"Personnes\", \"data\"=[" + x1 + "," + x2 + "]}";
//			Event.process(x);
//			Event.process(y);
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//	}

}

