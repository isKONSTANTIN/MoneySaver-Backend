package su.knst.moneysaver.utils.logger;

public abstract class AbstractLogger {
    public abstract void info(String text);
    public abstract void warn(String text);
    public abstract void error(String text);

    public abstract AbstractLogger subFace(String prefix);
}
