package su.knst.moneysaver.utils.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;

@Singleton
public class MainConfig {
    public CategoryWorker database;
    public CategoryWorker webPush;
    public CategoryWorker server;

    protected static ConfigWorker main;

    public MainConfig(){
        main = new ConfigWorker(new File("config/main.conf"));

        database = new CategoryWorker(main, "database");
        webPush = new CategoryWorker(main, "webPush");
        server = new CategoryWorker(main, "server");
    }
}
