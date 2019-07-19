package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.fiveonevr.apollo.client.enums.PropertyChangeType;
import com.fiveonevr.apollo.client.model.ConfigChange;
import com.fiveonevr.apollo.client.model.ConfigChangeEvent;
import com.fiveonevr.apollo.client.utils.AbstractConfig;
import com.fiveonevr.apollo.client.utils.ApolloThreadFactory;
import com.fiveonevr.apollo.client.utils.ClassLoaderUtil;
import com.fiveonevr.apollo.client.utils.ExceptionUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/17.
 */

public class DefaultConfig extends AbstractConfig implements RepositoryChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConfig.class);
    private static final ExecutorService m_executorService;
    private final String m_namespace;
    private final Properties m_resourceProperties;
    private final AtomicReference<Properties> m_configProperties;
    private final ConfigRepository m_configRepository;
    private final RateLimiter m_warnLogRateLimiter;
    private final List<ConfigChangeListener> m_listeners = Lists.newCopyOnWriteArrayList();
    private final Map<ConfigChangeListener, Set<String>> m_interestedKeys = Maps.newConcurrentMap();
    private volatile ConfigSourceType m_sourceType = ConfigSourceType.NONE;


    static { m_executorService = Executors.newCachedThreadPool(ApolloThreadFactory.create("Config", true));
    }

    public DefaultConfig(String namespace, ConfigRepository configRepository) {
        m_namespace = namespace;
        m_resourceProperties = loadFromResource(m_namespace);
        m_configRepository = configRepository;
        m_configProperties = new AtomicReference<>();
        m_warnLogRateLimiter = RateLimiter.create(0.017); // 1 warning log output per minute
        initialize();
    }

    //将本地缓存进行更新，将自己注册到localfileconfigRepository
    private void initialize() {
        try {
            updateConfig(m_configRepository.getProperty(), m_configRepository.getSourceType());
        } catch (Throwable ex) {
            logger.warn("Init Apollo Local Config failed - namespace: {}, reason: {}.", m_namespace, ExceptionUtils.getDetailMessage(ex));
        } finally {
            //register the change listener no matter config repository is working or not
            //so that whenever config repository is recovered, config could get changed
            m_configRepository.addChangeListener(this);
        }
    }

    //系统变量优先级最高，然后是缓存，然后是环境变量，最后是配置文件
    @Override
    public String getProperty(String key, String defaultValue) {
        //查询环境变量-DCompany=51vr
        String value = System.getProperty(key);
        //查询缓存
        if (value == null && m_configProperties.get() != null) {
            value = m_configProperties.get().getProperty(key);
        }

        //这个是环境变量，比如$PATH
        if (value == null) {
            value = System.getenv(key);
        }

        //从代码META-INF/config/xx.properties获取
        if (value == null && m_resourceProperties != null) {
            value = (String) m_resourceProperties.get(key);
        }

        if (value == null && m_configProperties.get() == null && m_warnLogRateLimiter.tryAcquire()) {
            logger.warn("Could not load config for namespace {} from Apollo, please check whether the configs are released in Apollo! Return default value now!", m_namespace);
        }
        //value为null 返回默认的吧
        return value == null ? defaultValue : value;
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue) {
        return null;
    }

    @Override
    public Long getLongProperty(String key, Long defaultValue) {
        return null;
    }

    @Override
    public Short getShortProperty(String key, Short defaultValue) {
        return null;
    }

    @Override
    public Float getFloatProperty(String key, Float defaultValue) {
        return null;
    }

    @Override
    public Double getDoubleProperty(String key, Double defaultValue) {
        return null;
    }

    @Override
    public Byte getByteProperty(String key, Byte defaultValue) {
        return null;
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        return null;
    }

    @Override
    public String[] getArrayProperty(String key, String delimiter, String[] defaultValue) {
        return new String[0];
    }

    @Override
    public Date getDateProperty(String key, Date defaultValue) {
        return null;
    }

    @Override
    public Date getDateProperty(String key, String format, Date defaultValue) {
        return null;
    }

    @Override
    public Date getDateProperty(String key, String format, Locale locale, Date defaultValue) {
        return null;
    }

    @Override
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        return null;
    }

    @Override
    public long getDurationProperty(String key, long defaultValue) {
        return 0;
    }

    @Override
    public Set<String> getPropertyNames() {
        Properties properties = m_configProperties.get();
        if (properties == null) {
            return Collections.emptySet();
        }
        return stringPropertyNames(properties);
    }

    @Override
    public <T> T getProperty(String key, Function<String, T> function, T defaultValue) {
        return null;
    }

    @Override
    public ConfigSourceType getSourceType() {
        return m_sourceType;
    }

    private Set<String> stringPropertyNames(Properties properties) {
        //jdk9以下版本Properties#enumerateStringProperties方法存在性能问题，keys() + get(k) 重复迭代, jdk9之后改为entrySet遍历.
        Map<String, String> h = new HashMap<>();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k instanceof String && v instanceof String) {
                h.put((String) k, (String) v);
            }
        }
        return h.keySet();
    }

    //LocalFileConfigRepository发生变动
    @Override
    public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
        //如果一致，直接返回
        if (newProperties.equals(m_configProperties.get())) {
            return;
        }
        ConfigSourceType sourceType = m_configRepository.getSourceType();
        Properties newConfigProperties = new Properties();
        newConfigProperties.putAll(newProperties);
        //ConfigChange是具体的某个property的change，更新并且计算
        Map<String, ConfigChange> actualChanges = updateAndCalcConfigChanges(newConfigProperties, sourceType);
        //check double checked result
        if (actualChanges.isEmpty()) {
            return;
        }
        //通知他所对应的listeners
        this.fireConfigChange(new ConfigChangeEvent(m_namespace, actualChanges));

    }

    private void updateConfig(Properties newConfigProperties, ConfigSourceType sourceType) {
        m_configProperties.set(newConfigProperties);
        m_sourceType = sourceType;
    }



    private Map<String, ConfigChange> updateAndCalcConfigChanges(Properties newConfigProperties, ConfigSourceType sourceType) {
        //获取本次与上次的Config
        List<ConfigChange> configChanges = calcPropertyChanges(m_namespace, m_configProperties.get(), newConfigProperties);

        ImmutableMap.Builder<String, ConfigChange> actualChanges = new ImmutableMap.Builder<>();

        /** === Double check since DefaultConfig has multiple config sources ==== **/

        //1. use getProperty to update configChanges's old value
        for (ConfigChange change : configChanges) {
            change.setOldValue(this.getProperty(change.getPropertyName(), change.getOldValue()));
        }

        //2. update m_configProperties
        updateConfig(newConfigProperties, sourceType);
        clearConfigCache();

        //3. use getProperty to update configChange's new value and calc the final changes
        for (ConfigChange change : configChanges) {
            change.setNewValue(this.getProperty(change.getPropertyName(), change.getNewValue()));
            switch (change.getChangeType()) {
                case ADDED:
                    if (Objects.equals(change.getOldValue(), change.getNewValue())) {
                        break;
                    }
                    if (change.getOldValue() != null) {
                        change.setChangeType(PropertyChangeType.MODIFIED);
                    }
                    actualChanges.put(change.getPropertyName(), change);
                    break;
                case MODIFIED:
                    if (!Objects.equals(change.getOldValue(), change.getNewValue())) {
                        actualChanges.put(change.getPropertyName(), change);
                    }
                    break;
                case DELETED:
                    if (Objects.equals(change.getOldValue(), change.getNewValue())) {
                        break;
                    }
                    if (change.getNewValue() != null) {
                        change.setChangeType(PropertyChangeType.MODIFIED);
                    }
                    actualChanges.put(change.getPropertyName(), change);
                    break;
                default:
                    break;
            }
        }
        return actualChanges.build();
    }




    private Properties loadFromResource(String namespace) {
        String name = String.format("META-INF/config/%s.properties", namespace);
        InputStream in = ClassLoaderUtil.getLoader().getResourceAsStream(name);
        Properties properties = null;
        if (in != null) {
            properties = new Properties();

            try {
                properties.load(in);
            } catch (IOException ex) {
                logger.error("Load resource config for namespace {} failed", namespace, ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        return properties;
    }




}
