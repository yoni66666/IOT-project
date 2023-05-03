package il.co.ilrd.multiprotocolserver;

import il.co.ilrd.factory.Factory;
import il.co.ilrd.filetracker.FolderWatcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public class SingletonPlugAndPlayFactory {
    private FolderWatcher watcher;
    private JarObserver observer;
    private Factory<String, IOTCommand, Void> factory;

    private static final String JAR_PATH = "/home/jonathan/git/jonathan.shapiro/fs/projects/src";

    public static SingletonPlugAndPlayFactory getInstance() {
        return SingletonHolder.instance;
    }

    private SingletonPlugAndPlayFactory(String jarsFolderPath) {
        watcher = new FolderWatcher(JAR_PATH);
        observer = new JarObserver();
        factory = new Factory<>();
    }

    public String getJarPath(){
        return JAR_PATH;
    }

    public void initObserver(){
        try {
            observer.addToFactoryTheDefaultRegisters();
        } catch (IOException  | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SingletonHolder {
        private static final SingletonPlugAndPlayFactory instance = new SingletonPlugAndPlayFactory("");
    }

    public void play() {
        initObserver();
        observer.subscribe(watcher);
        watcher.start();
    }

    public IOTCommand create(String key) {
        return factory.create(key);
    }

    public void add(String key, Function<Void, ? extends IOTCommand> func) {
        factory.add(key,func);
    }
}
