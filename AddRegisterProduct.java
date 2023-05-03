package il.co.ilrd.multiprotocolserver;

public class AddRegisterProduct implements FactoryInitializer{
    @Override
    public void addToFactory() {
        System.out.println("it is AddRegisterProduct");
        SingletonPlugAndPlayFactory.getInstance().add("registerProduct", (Void) -> new RegisterProduct());
    }
}
