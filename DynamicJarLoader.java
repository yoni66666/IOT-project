package il.co.ilrd.multiprotocolserver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * DynamicJarLoader is a class that is used to load and execute Java code from JAR files at runtime.
 * It allows for adding new functionality to a Java application without having to restart the application (ex: Plugins)
 */
public class DynamicJarLoader {
    private String interfaceName;
    private String jarPath;

    public DynamicJarLoader(String interfaceName) throws MalformedURLException {
        this.interfaceName = interfaceName;

    }

    /* load the classes from jar file that implement the given interface name */
    public List<Class<?>> load(String jarPath) throws IOException, ClassNotFoundException {
        this.jarPath = jarPath;
        List<Class<?>> classes = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarPath);
             URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(jarPath).toURI().toURL()})) {
            Iterator<JarEntry> entries = jarFile.stream().iterator();

            // traverse over files in jar
            while (entries.hasNext()) {
                JarEntry entry = entries.next();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                String className = entry.getName().split("\\.")[0].replace('/', '.');
                Class<?> currClass = Class.forName(className, false, classLoader);
                for (Class<?> currInterface : currClass.getInterfaces()) {
                    if (currInterface.getSimpleName().equals(interfaceName)) {
                        currClass = Class.forName(className, true, classLoader);
                        classes.add(currClass);
                        break;
                    }
                }
            }
        }

        return classes;
    }
}
