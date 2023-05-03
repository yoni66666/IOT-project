package il.co.ilrd.multiprotocolserver;

import il.co.ilrd.filetracker.FolderWatcher;
import il.co.ilrd.observer.Callback;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class JarObserver {
    private DynamicJarLoader loader;
    private Callback<WatchEvent<?>> callback;

    public JarObserver(){
        try {
            loader = new DynamicJarLoader("FactoryInitializer");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribe(FolderWatcher watcher) {
        callback = new Callback<>(this::notifyLoadJarFile, null);
        watcher.register(callback);
    }

    public void addToFactoryTheDefaultRegisters () throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //must add all file inside the folder, because mayby a user add a jar file and then the service fall down
        String path = SingletonPlugAndPlayFactory.getInstance().getJarPath()+"/JarExample.jar";
        addToFactoryNewCommand(path);
    }

    private void addToFactoryNewCommand(String path) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Class<?>> classesList = loader.load(path);
        for (Class<?> c : classesList) {
            FactoryInitializer factoryInitializer = (FactoryInitializer) c.getDeclaredConstructor().newInstance();
            factoryInitializer.addToFactory();
        }
    }

    private void notifyLoadJarFile(WatchEvent<?> event)  {
        if (event.kind() == ENTRY_CREATE){
            System.out.println("notifyUpdateFile got notify");

            Path createdPath = (Path) event.context();
            System.out.println(createdPath);

            String path = SingletonPlugAndPlayFactory.getInstance().getJarPath()+"/"+createdPath;

            File file = createdPath.toFile();
            String fileName = file.getName();
            if (fileName.endsWith(".jar")) {
                System.out.println(fileName + " is a .jar file.");
                try {
                    addToFactoryNewCommand(path);
                }catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println(fileName + " is not a .jar file.");
            }
        }
    }
}
