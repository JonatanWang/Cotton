package cotton.systemsupport;
public interface StatisticsProvider {
    public StatisticsData[] getStatisticsForSubSystem(String name);
    public StatisticsData getStatistics(String[] name);
    public StatisticsProvider getProvider();
    public StatType getStatType();
}
