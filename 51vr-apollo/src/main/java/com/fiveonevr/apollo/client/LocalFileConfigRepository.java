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
    private volatile ConfigRegistry configRegistry;

    public LocalFileConfigRepository(String namespace) {
        this(namespace, null);



    }

    public LocalFileConfigRepository(String namespace, ConfigRepository upstreamRepository) {
        this.namespace = namespace;
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        this.setLocalCacheDir(findLocalCacheDir(), false);

        this.setUpstreamRepository(upstreamRepository);
        this.trySync();




    }

    //创建缓存目录，若immediately=true，立刻同步
    void setLocalCacheDir(File baseDir, boolean syncImmediately) {
        this.baseDir = baseDir;
        this.checkLocalConfigCacheDir(this.baseDir);
        if(syncImmediately) {
            this.trySync();
        }
    }


    private void checkLocalConfigCacheDir(File baseDir) {
        if(baseDir.exists()) {
            return;
        }
        try {
            Files.createDirectory(baseDir.toPath());
        }catch (IOException ex) {
            logger.warn("Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.",
                    baseDir.getAbsolutePath(), ExceptionUtils.getDetailMessage(ex));
        }
    }


    //获取缓存File对象,比如c:\opt\001\XXX
    private File findLocalCacheDir() {
        try {
            String defaultCacheDir = configUtil.getDefaultLocalCacheDir();
            Path path = Paths.get(defaultCacheDir);
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }
            if(Files.exists(path) &&  Files.isWritable(path)) {
                return new File(defaultCacheDir, CONFIG_DIR);
            }
        }catch (Throwable ex) {
            //ignore
        }
        return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
    }




    @Override
    protected void sync() {
        boolean syncFromUpstreamSuccess = trySyncFromUpstream();
        if(syncFromUpstreamSuccess) {
            return;//已经同步过了
        }
        Throwable exception = null;
        try {
            this.properties  = this.loadFromLocalCacheFile(this.baseDir, this.namespace);
            this.sourceType = ConfigSourceType.LOCAL;
        }catch (Throwable ex) {
            logger.error("local repository  sync error: ", ExceptionUtils.getDetailMessage(ex));
            exception = ex;
        }finally {
            //todo
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
        this.upstreamRepository = upstreamRepository;
        trySyncFromUpstream();
        this.upstreamRepository.addChangeListener(this);
    }

    //尝试与upstream同步，同步失败返回false，同步成功，将properties与source本地缓存
    private boolean trySyncFromUpstream() {
        if(upstreamRepository == null) {
            return false;
        }
        try {
            updateFileProperties(upstreamRepository.getProperty(), upstreamRepository.getSourceType());
            return true;
        }catch (Throwable ex) {
            logger.warn("Sync config from upstream repository {} failed, reason: {}", upstreamRepository.getClass(), ExceptionUtils.getDetailMessage(ex));
        }
        return false;
    }


    private synchronized  void updateFileProperties(Properties newProperties, ConfigSourceType sourceType) {
        this.sourceType = sourceType;
        if(newProperties.equals(this.properties)) {
            return;
        }
        this.properties = newProperties;

    }

    //通过本地缓存文件返回Properties
    private Properties loadFromLocalCacheFile(File basedir, String namespace) {
        Preconditions.checkNotNull(baseDir, "Basedir cannot be null");
        File file = assembleLocalCacheFile(baseDir, namespace);
        Properties properties = null;
        if(file.isFile() && file.canRead()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);
                logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());

            }catch (IOException ex) {
                throw new ApolloConfigException(String
                        .format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
            }finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore
                }
            }
        }else {
            throw new ApolloConfigException(
                    String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
        }
        return properties;
    }



    //将缓存的properties写入文件
    void persistLocalCacheFile(File baseDir, String namespace) {
        if(baseDir == null) {
            return;
        }
        File file = assembleLocalCacheFile(baseDir, namespace);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            this.properties.store(out, "Persisted by DefaultConfig");


        }catch (IOException ex) {
            logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(),
                    ExceptionUtils.getDetailMessage(ex));
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
        String fileName = String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
                .join(configUtil.getAppId(), configUtil.getCluster(), namespace));
        return new File(baseDir, fileName);
    }

}
