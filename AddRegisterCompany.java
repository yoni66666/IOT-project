package il.co.ilrd.multiprotocolserver;

public class AddRegisterCompany implements FactoryInitializer{
    @Override
    public void addToFactory() {
        System.out.println("it is AddRegisterCompany");
        SingletonPlugAndPlayFactory.getInstance().add("registerCompany", (Void) -> new RegisterCompany());
    }
}
