package su.knst.moneysaver.utils.logger;

import java.time.Instant;
import java.util.Date;

public class SimpleConsoleLogger extends AbstractLogger{
    protected String prefix;

    public SimpleConsoleLogger(String prefix){
        this.prefix = prefix;
    }

    @Override
    public void info(String text) {
        System.out.println("[" + time() + "] " + prefix + " [INFO] " + text);
    }

    @Override
    public void warn(String text) {
        System.out.println("[" + time() + "] " + prefix + " [WARN] " + text);
    }

    @Override
    public void error(String text) {
        System.err.println("[" + time() + "] " + prefix + " [ERROR] " + text);
    }

    protected String time(){
        return String.valueOf(Date.from(Instant.now()));
    }

    @Override
    public AbstractLogger subFace(String prefix) {
        return new SimpleConsoleLogger(this.prefix + " " + prefix);
    }
}
