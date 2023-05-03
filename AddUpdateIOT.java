package il.co.ilrd.multiprotocolserver;

public class AddUpdateIOT implements FactoryInitializer {
    @Override
    public void addToFactory() {
        System.out.println("it is updateIOT");
        SingletonPlugAndPlayFactory.getInstance().add("updateIOT", (Void) -> new UpdateIOT());
    }
}
