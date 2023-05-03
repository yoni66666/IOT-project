package il.co.ilrd.multiprotocolserver;

public class AddRegisterIOT implements FactoryInitializer {
    @Override
    public void addToFactory() {
        System.out.println("it is registerIOT");
        SingletonPlugAndPlayFactory.getInstance().add("registerIOT", (Void) -> new RegisterIOT());
    }
}
