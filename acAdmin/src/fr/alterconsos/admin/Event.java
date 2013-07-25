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

	public static void register(Class<?> clazz) {
		String type = clazz.getSimpleName();
		types.put(type, clazz);
	}

	public static void register(Class<?> clazz, Type typeToken) {
		String type = clazz.getSimpleName();
		types.put(type, clazz);
		ctypes.put(type, typeToken);
	}

	@SuppressWarnings("rawtypes")
	public static class EventDeserializer implements JsonDeserializer<Event> {

		@SuppressWarnings("unchecked")
		public Event deserialize(JsonElement jsonElement, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException {
			JsonObject event = (JsonObject) jsonElement;
			String type = event.get("type").getAsString();
			int callId = event.get("callId").getAsInt();
			JsonElement eltData = event.get("data");
			Class<?> clazz = types.get(type);
			if (clazz == null)
				return null;
			if (eltData != null && eltData.isJsonObject()) {
				return new Event(type, callId, (IEvent) context.deserialize(
						eltData, clazz));
			}
			if (eltData != null && eltData.isJsonArray()) {
				Type t = ctypes.get(type);
				if (t != null)
					return new Event(type, callId,
							(ArrayList) context.deserialize(eltData, t));
			}
			return null;
		}
	}

	public static String serial(String data) {
		return gson.toJson(data, String.class);
	}

	public static String serial(Object data) {
		if (data == null)
			return "";
		String type = data.getClass().getSimpleName();
		try {
			Class<?> clazz = types.get(type);
			if (clazz == null)
				return gson.toJson(data);
			Field f = clazz.getDeclaredField("list");
			return gson.toJson(f.get(data));
		} catch (Exception e) {
			return gson.toJson(data);
		}
	}

	public static Event<?> get(String json) throws Exception {
		return gson.fromJson(json, Event.class);
	}
	
	public static IEvent get(Event<?> event) throws Exception {
		Class<?> clazz = types.get(event.type);
		IEvent obj = (IEvent) (clazz.newInstance());
		Field f = clazz.getDeclaredField("list");
		f.set(obj, event.col);
		return obj;
	}

	public static Object process(String json) throws Exception {
		Event<?> event = gson.fromJson(json, Event.class);
		if (event == null)
			return null;
		if (event.data != null)
			return event.data.process();
		else {
			Class<?> clazz = types.get(event.type);
			IEvent obj = (IEvent) (clazz.newInstance());
			Field f = clazz.getDeclaredField("list");
			f.set(obj, event.col);
			return obj.process();
		}
	}

	public String type;
	public int callId;
	public IEvent data;
	public ArrayList<?> col;

	public Event(String type, int callId, IEvent data) {
		this.type = type;
		this.callId = callId;
		this.data = data;
	}

	public Event(String type, int callId, ArrayList<?> data) {
		this.type = type;
		this.callId = callId;
		this.col = data;
	}

	// public static void main(String[] args){
	// try {
	// Event.register("Personne", Main.Personne.class);
	// Event.register("Personnes", Main.Personnes.class, new
	// TypeToken<Collection<Main.Personne>>(){}.getType());
	// String x1 = "{\"nom\"=\"Sportes\", \"age\"=63}";
	// String x2 = "{\"nom\"=\"Colin\", \"age\"=62}";
	// String x = "{\"type\"=\"Personne\", \"data\"=" + x1 + "}";
	// String y = "{\"type\"=\"Personnes\", \"data\"=[" + x1 + "," + x2 + "]}";
	// Event.process(x);
	// Event.process(y);
	// } catch (Exception e){
	// e.printStackTrace();
	// }
	// }

}
