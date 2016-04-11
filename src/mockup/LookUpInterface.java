public Interface LookUpInterface {
        public Boolean registrateService(String serviceName, ServiceFactory serviceFactory);
        public ServiceMetaData getService(String serviceName);
        public Enumeration<String> getServiceEnumeration();
        public Boolean removeServiceEntry(String service);

}
