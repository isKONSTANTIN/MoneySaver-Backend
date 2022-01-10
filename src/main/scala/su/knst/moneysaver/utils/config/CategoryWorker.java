package su.knst.moneysaver.utils.config;

public class CategoryWorker {
    protected ConfigWorker configWorker;
    protected String category;

    public CategoryWorker(ConfigWorker configWorker, String category) {
        this.configWorker = configWorker;
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ConfigWorker getConfigWorker() {
        return configWorker;
    }

    public void saveString(String key, String value){
        configWorker.saveString(category, key, value);
    }

    public String saveStringIfNull(String key, String value){
        return configWorker.saveStringIfNull(category, key, value);
    }

    public String loadString(String key, String defaultValue){
        return configWorker.loadString(category, key, defaultValue);
    }

    public String loadString(String key){
        return configWorker.loadString(category, key);
    }

    public void saveAsJSON(String key, Object value){
        configWorker.saveAsJSON(category, key, value);
    }

    public <T> T loadJSON(String key, Class<T> tClass) {
        return configWorker.loadJSON(category, key, tClass);
    }

    public void saveBinaryObject(String key, BinaryObject object){
        configWorker.saveBinaryObject(category, key, object);
    }

    public <T extends BinaryObject> T loadBinaryObject(String key, Class<T> tClass) throws IllegalAccessException, InstantiationException {
        return configWorker.loadBinaryObject(category, key, tClass);
    }
}
