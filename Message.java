package il.co.ilrd.multiprotocolserver;

/*
    Name: Johnathan Tali
    Reviewer: Tali
    Exercise: Multi Protocol Server
*/

import java.io.Serializable;

public interface Message<K,T> extends Serializable {
    public K getKey();
    public T getMessage();

}
