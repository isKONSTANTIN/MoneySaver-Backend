package su.knst.moneysaver.utils.config;

import com.google.inject.Singleton;

import java.io.File;

@Singleton
public class InitConfig {
    public CategoryWorker admin;

    protected static ConfigWorker init;

    public InitConfig(){
        init = new ConfigWorker(new File("config/init.conf"));

        admin = new CategoryWorker(init, "admin");
    }
}
