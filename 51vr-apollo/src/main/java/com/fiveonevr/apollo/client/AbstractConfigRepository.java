package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.utils.ExceptionUtils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

public abstract class AbstractConfigRepository implements  ConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigRepository.class);
    //listeners使用copyOnWriteArrayList<RepositoryChangeListener>
    private List<RepositoryChangeListener> listeners = Lists.newCopyOnWriteArrayList();
    //抽象方法，各自实现
    protected abstract void sync();

    protected boolean trySync() {
        try {
            sync();
            return true;
        }catch (Throwable ex) {
            logger.warn("Sync config failed, will retry. Repository {}, reason: {}", this.getClass(), ExceptionUtils.getDetailMessage(ex));
            return false;
        }
    }


    @Override
    public void addChangeListener(RepositoryChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    @Override
    public void removeChangeListener(RepositoryChangeListener listener) {
        listeners.remove(listener);
    }

    //引爆监听器的change方法
    protected void fireRepositoryChange(String namespace, Properties newProperties) {
        for (RepositoryChangeListener listener : listeners) {
            try {
                listener.onRepositoryChange(namespace, newProperties);
            } catch (Throwable ex) {
                logger.error("Failed to invoke repository change listener {}", listener.getClass(), ex);
            }
        }
    }
}
