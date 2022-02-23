package su.knst.moneysaver.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ConfigWorker {
    protected Gson gson = new GsonBuilder().setPrettyPrinting().create();
    protected File file;
    protected HashMap<String, Map<String, String>> cache = new HashMap<>();

    public ConfigWorker(File config){
        this.file = config;

        try {
            loadFromFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveString(String category, String key, String value) {
        if (!value.equals(getCategory(category).put(key, value)))
            try {
                saveToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public String saveStringIfNull(String category, String key, String value){
        Map<String, String> categoryMap = getCategory(category);
        String saved = categoryMap.get(key);

        if (saved == null){
            categoryMap.put(key, value);
            try {
                saveToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            saved = value;
        }

        return saved;
    }

    public String loadString(String category, String key, String defaultValue){
        return getCategory(category).getOrDefault(key, defaultValue);
    }

    protected void saveToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(gson.toJson(cache));

        writer.flush();
        writer.close();
    }

    protected void loadFromFile() throws Exception {
        if (!file.exists()){
            new File(file.getAbsoluteFile().getParent()).mkdirs();
            file.createNewFile();
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buffer = new char[128];
        int readed;
        StringBuilder builder = new StringBuilder();

        while ((readed = reader.read(buffer)) != -1)
            builder.append(buffer, 0, readed);

        HashMap<String, Map<String, String>> newCache = gson.fromJson(builder.toString(), cache.getClass());

        if (newCache != null)
            cache = newCache;
    }

    protected Map<String, String> getCategory(String category){
        Map<String, String> map;

        if (!cache.containsKey(category)){
            map = new HashMap<>();
            cache.put(category, map);
        }else {
            map = cache.get(category);
        }

        return map;
    }

    public String loadString(String category, String key){
        return loadString(category, key, null);
    }

    public void saveAsJSON(String category, String key, Object value){
        saveString(category, key, gson.toJson(value));
    }

    public <T> T loadJSON(String category, String key, Class<T> tClass) {
        String loaded = loadString(category, key);
        if (loaded == null)
            return null;

        return gson.fromJson(loaded, tClass);
    }

    public void saveBinaryObject(String category, String key, BinaryObject object){
        ByteBuffer byteBuffer = object.save();
        String base64 = Base64.getEncoder().encodeToString(byteBuffer.array());
        saveString(category, key, base64);
    }

    public <T extends BinaryObject> T loadBinaryObject(String category, String key, Class<T> tClass) throws IllegalAccessException, InstantiationException {
        String loadedBase64 = loadString(category, key);
        if (loadedBase64 == null)
            return null;

        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(loadedBase64));
        }catch (Exception e){
            e.printStackTrace();
        }

        if (byteBuffer == null)
            return null;

        T object = tClass.newInstance();
        try {
            object.load(byteBuffer);
        }catch (Exception e){
            object = null;
            e.printStackTrace();
        }

        return object;
    }
}
