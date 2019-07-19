package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ConfigConsts;
import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.utils.ClassLoaderUtil;
import com.fiveonevr.apollo.client.utils.ConfigUtil;
import com.fiveonevr.apollo.client.utils.ExceptionUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class LocalFileConfigRepository extends AbstractConfigRepository implements RepositoryChangeListener{

    private static final Logger logger = LoggerFactory.getLogger(LocalFileConfigRepository.class);
    private volatile ConfigSourceType sourceType = ConfigSourceType.LOCAL;
    private final static String CONFIG_DIR = "/config-cache";
    private final String namespace;
    private File baseDir;
    private volatile Properties properties;
    private volatile ConfigRepository upstreamRepository;
    private ConfigUtil configUtil;


    public LocalFileConfigRepository(String namespace) {
        this(namespace, null);

    }

    public LocalFileConfigRepository(String namespace, ConfigRepository upstreamRepository) {
        this.namespace = namespace;
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        //目录不存在，生成目录
        this.setLocalCacheDir(findLocalCacheDir(), false);
        //将本地properties与sorucetype进行同步，与upstream进行同步，并且写入本地缓存
        this.setUpstreamRepository(upstreamRepository);
        this.trySync();
    }

    //创建缓存目录，若immediately=true，立刻同步
    void setLocalCacheDir(File baseDir, boolean syncImmediately) {
        //C:\opt\data\001\config-cache，basedir设置文件
        this.baseDir = baseDir;
        //如果目录不存在，就生成目录
        this.checkLocalConfigCacheDir(this.baseDir);
        if(syncImmediately) {
            this.trySync();
        }
    }

    //如果目录不存在，就生成目录
    private void checkLocalConfigCacheDir(File baseDir) {
        if(baseDir.exists()) {
            return;
        }
        try {
            Files.createDirectory(baseDir.toPath());
        }catch (IOException ex) {
            logger.warn("Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.", baseDir.getAbsolutePath(), ExceptionUtils.getDetailMessage(ex));
        }
    }

    //获取缓存文件路径
    private File findLocalCacheDir() {
        try {
            String defaultCacheDir = configUtil.getDefaultLocalCacheDir();
            //C:\opt\data\001
            Path path = Paths.get(defaultCacheDir);
            //如果目录没有被创建，可能是sdk第一次执行
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }
            //如果file存在并且可写
            if(Files.exists(path) &&  Files.isWritable(path)) {
                return new File(defaultCacheDir, CONFIG_DIR);
            }
        }catch (Throwable ex) {
            //ignore
        }
        return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
    }

    //这个方法有意思了，先进行upstream获取，获取失败，走本地缓存
    @Override
    protected void sync() {
        //upstream为null 或者 同步upstream的时候发生异常
        boolean syncFromUpstreamSuccess = trySyncFromUpstream();
        if(syncFromUpstreamSuccess) {
            return;//已经同步过了
        }
        Throwable exception = null;
        try {
            //读取本地cache
            this.properties  = this.loadFromLocalCacheFile(this.baseDir, this.namespace);
            this.sourceType = ConfigSourceType.LOCAL;
        }catch (Throwable ex) {
            logger.error("local repository  sync error: ", ExceptionUtils.getDetailMessage(ex));
            exception = ex;
        }finally {
            //ignore
        }
        if(this.properties == null) {
            this.sourceType = ConfigSourceType.NONE;
            throw new ApolloConfigException("Load config from local config failed!", exception);
        }
    }

    @Override
    public Properties getProperty() {
        if (this.properties == null) {
            sync();
        }
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamRepository) {
        if(upstreamRepository == null) {
            return;
        }
        if(this.upstreamRepository != null) {
            this.upstreamRepository.removeChangeListener(this);
        }
        //本地缓存upstream设置为 reomoteConfigRepository传入对象
        this.upstreamRepository = upstreamRepository;
        trySyncFromUpstream();
        this.upstreamRepository.addChangeListener(this);
    }


    //与upstream同步
    private boolean trySyncFromUpstream() {
        if(upstreamRepository == null) {
            return false;
        }
        try {
            //同步本地properties以及sourcetype
            updateFileProperties(upstreamRepository.getProperty(), upstreamRepository.getSourceType());
            return true;
        }catch (Throwable ex) {
            logger.warn("Sync config from upstream repository {} failed, reason: {}", upstreamRepository.getClass(), ExceptionUtils.getDetailMessage(ex));
        }
        return false;
    }


    //同步方法块，将sourceType与properties进行同步
    private synchronized  void updateFileProperties(Properties newProperties, ConfigSourceType sourceType) {
        this.sourceType = sourceType;
        //Properties equals
        if(newProperties.equals(this.properties)) {
            return;
        }
        this.properties = newProperties;
        this.persistLocalCacheFile(baseDir, namespace);
    }

    //通过本地缓存文件返回Properties
    private Properties loadFromLocalCacheFile(File basedir, String namespace) {
        //如果为NULL会抛出空指针
        Preconditions.checkNotNull(baseDir, "Basedir cannot be null");
        //获取缓存文件
        File file = assembleLocalCacheFile(baseDir, namespace);
        Properties properties = null;
        //是file并且可读
        if(file.isFile() && file.canRead()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);
                logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());

            }catch (IOException ex) {
                throw new ApolloConfigException(String.format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
            }finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore
                }
            }
        }else {//如果无法读取抛出异常
            throw new ApolloConfigException(String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
        }
        return properties;
    }



    //将缓存的properties写入文件
    void persistLocalCacheFile(File baseDir, String namespace) {
        if(baseDir == null) {
            return;
        }
        //需要存储的文件
        File file = assembleLocalCacheFile(baseDir, namespace);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            this.properties.store(out, "Persisted by DefaultConfig");
        }catch (IOException ex) {
            logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(), ExceptionUtils.getDetailMessage(ex));
        }finally {
            if(out != null) {
                try {
                    out.close();
                }catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    @Override
    public ConfigSourceType getSourceType() {
        return null;
    }

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        if (newProperties.equals(properties)) {
            return;
        }
        Properties newFileProperties = new Properties();
        newFileProperties.putAll(newProperties);
        updateFileProperties(newFileProperties, this.upstreamRepository.getSourceType());
        this.fireRepositoryChange(namespace, newProperties);
    }

    //获取缓存文件名称
    File assembleLocalCacheFile(File baseDir, String namespace) {
        //001+default+application
        String fileName = String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join(configUtil.getAppId(), configUtil.getCluster(), namespace));
        return new File(baseDir, fileName);
    }
}
