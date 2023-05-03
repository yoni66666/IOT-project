package il.co.ilrd.multiprotocolserver;

import java.util.HashMap;
import java.util.function.Function;

public class IotFactory<K, T, D> {
    private HashMap<K, Function<D, ? extends T>> factoryMap = new HashMap<>();

    public void add(K key, Function<D, ? extends  T> func){
        factoryMap.put(key, func);
    }

    public T create(K key, D data){
        return factoryMap.get(key).apply(data);
    }

    public T create(K key){
        return factoryMap.get(key).apply(null);
    }
}
