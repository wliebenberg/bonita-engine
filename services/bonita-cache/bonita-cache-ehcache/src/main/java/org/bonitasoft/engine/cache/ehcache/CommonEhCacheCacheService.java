package org.bonitasoft.engine.cache.ehcache;

import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.bonitasoft.engine.cache.CacheConfigurations;
import org.bonitasoft.engine.cache.CacheException;
import org.bonitasoft.engine.cache.CommonCacheService;
import org.bonitasoft.engine.commons.LogUtil;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;

public abstract class CommonEhCacheCacheService implements CommonCacheService {

    protected CacheManager cacheManager;

    protected final TechnicalLoggerService logger;

    protected final ReadSessionAccessor sessionAccessor;

    protected final Map<String, CacheConfiguration> cacheConfigurations;

    private final URL configFile;

    public CommonEhCacheCacheService(final TechnicalLoggerService logger, final ReadSessionAccessor sessionAccessor,
            final CacheConfigurations cacheConfigurations) {
        this(logger, sessionAccessor, cacheConfigurations, null);
    }

    public CommonEhCacheCacheService(final TechnicalLoggerService logger, final ReadSessionAccessor sessionAccessor,
            final CacheConfigurations cacheConfigurations, final URL configFile) {
        this.logger = logger;
        this.sessionAccessor = sessionAccessor;
        this.configFile = configFile;
        final List<org.bonitasoft.engine.cache.CacheConfiguration> configurations = cacheConfigurations.getConfigurations();
        this.cacheConfigurations = new HashMap<String, CacheConfiguration>(configurations.size());
        for (final org.bonitasoft.engine.cache.CacheConfiguration cacheConfig : configurations) {
            this.cacheConfigurations.put(cacheConfig.getName(), getEhCacheConfiguration(cacheConfig));
        }
    }

    protected CacheConfiguration getEhCacheConfiguration(final org.bonitasoft.engine.cache.CacheConfiguration cacheConfig) {
        final CacheConfiguration ehCacheConfig = new CacheConfiguration();
        ehCacheConfig.setMaxElementsInMemory(cacheConfig.getMaxElementsInMemory());
        ehCacheConfig.setMaxElementsOnDisk(cacheConfig.getMaxElementsOnDisk());
        ehCacheConfig.setOverflowToDisk(!cacheConfig.isInMemoryOnly());
        ehCacheConfig.setTimeToLiveSeconds(cacheConfig.getTimeToLiveSeconds());
        ehCacheConfig.setEternal(cacheConfig.isEternal());
        return ehCacheConfig;
    }

    protected synchronized Cache createCache(final String cacheName, final String internalCacheName) throws CacheException {
        if (cacheManager == null) {
            throw new CacheException("The cache is not stated, call start() on the cache service");
        }
        Cache cache = cacheManager.getCache(internalCacheName);
        if (cache == null) {
            final CacheConfiguration cacheConfiguration = cacheConfigurations.get(cacheName);
            if (cacheConfiguration != null) {
                final CacheConfiguration newCacheConfig = cacheConfiguration.clone();
                newCacheConfig.setName(internalCacheName);
                cache = new Cache(newCacheConfig);
                cacheManager.addCache(cache);
            } else {
                throw new CacheException("No configuration found for the cache " + cacheName);
            }
        }
        return cache;
    }

    @Override
    public void store(final String cacheName, final Serializable key, final Object value) throws CacheException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "store"));
        }
        if (cacheManager == null) {
            throw new CacheException("The cache is not started, call start() on the cache service");
        }
        final String cacheNameKey = getKeyFromCacheName(cacheName);
        try {
            Cache cache = cacheManager.getCache(cacheNameKey);
            if (cache == null) {
                cache = createCache(cacheName, cacheNameKey);
            }
            if (value instanceof Serializable) {
                cache.put(new Element(key, (Serializable) value));
            } else {
                cache.put(new Element(key, value));
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "store"));
            }
        } catch (final IllegalStateException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "store", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' is not alive");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        } catch (final net.sf.ehcache.CacheException ce) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "store", ce));
            }
            throw new CacheException(ce);
        }
    }

    protected abstract String getKeyFromCacheName(String cacheName) throws CacheException;

    @Override
    public Object get(final String cacheName, final Object key) throws CacheException {
        if (cacheManager == null) {
            return null;
        }
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "get"));
        }
        final String cacheNameKey = getKeyFromCacheName(cacheName);
        try {
            final Cache cache = cacheManager.getCache(cacheNameKey);
            if (cache == null) {
                // the cache does not exist = the key was not stored
                return null;
            }
            final Element element = cache.get(key);
            if (element != null) {
                if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                    logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "get"));
                }
                return element.getObjectValue();
            } else {
                if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                    logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "get"));
                }
                return null;
            }
        } catch (final IllegalStateException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "get", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' is not alive");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        } catch (final Exception e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "get", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' does not exist");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        }
    }

    @Override
    public boolean clear(final String cacheName) throws CacheException {
        if (cacheManager == null) {
            return true;
        }
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "clear"));
        }
        final String cacheNameKey = getKeyFromCacheName(cacheName);
        try {
            final Cache cache = cacheManager.getCache(cacheNameKey);
            if (cache != null) {
                cache.removeAll();
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "clear"));
            }
            return cache == null;
        } catch (final IllegalStateException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "clear", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' is not alive");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        } catch (final Exception e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "clear", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' does not exist");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        }
    }

    @Override
    public int getCacheSize(final String cacheName) throws CacheException {
        if (cacheManager == null) {
            return 0;
        }
        final Cache cache = cacheManager.getCache(getKeyFromCacheName(cacheName));
        if (cache == null) {
            return 0;
        }
        return cache.getSize();
    }

    @Override
    public void clearAll() throws CacheException {
        if (cacheManager == null) {
            return;
        }
        try {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "clearAll"));
            }
            final List<String> cacheNames = getCachesNames();
            for (final String cacheName : cacheNames) {
                clear(cacheName);
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "clearAll"));
            }
        } catch (final net.sf.ehcache.CacheException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "clearAll", e));
            }
            throw new CacheException(e);
        }
    }

    @Override
    public boolean remove(final String cacheName, final Object key) throws CacheException {
        if (cacheManager == null) {
            return false;
        }

        final String cacheNameKey = getKeyFromCacheName(cacheName);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "remove"));
        }
        final Cache cache = cacheManager.getCache(cacheNameKey);

        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "remove"));
        }
        // key was not removed
        return cache != null && cache.remove(key);
    }

    @Override
    public List<Object> getKeys(final String cacheName) throws CacheException {
        if (cacheManager == null) {
            return Collections.emptyList();
        }
        final String cacheNameKey = getKeyFromCacheName(cacheName);
        try {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getKeys"));
            }
            final Cache cache = cacheManager.getCache(cacheNameKey);
            if (cache == null) {
                return Collections.emptyList();
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getKeys"));
            }
            return cache.getKeys();
        } catch (final IllegalStateException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getKeys", e));
            }
            final StringBuilder stringBuilder = new StringBuilder("The cache '");
            stringBuilder.append(cacheNameKey).append("' is not alive");
            final String msg = stringBuilder.toString();
            throw new CacheException(msg, e);
        } catch (final net.sf.ehcache.CacheException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getKeys", e));
            }
            throw new CacheException(e);
        }
    }

    public void destroy() throws SBonitaException, TimeoutException {
        stop();
    }

    @Override
    public synchronized void start() throws SBonitaException {
        this.cacheManager = configFile != null ? CacheManager.create(configFile) : CacheManager.create();
    }

    @Override
    public synchronized void stop() throws SBonitaException, TimeoutException {
        if (cacheManager != null) {
            this.cacheManager.shutdown();
            this.cacheManager = null;
        }
    }

    @Override
    public void pause() throws SBonitaException, TimeoutException {
        // nothing to do
    }

    @Override
    public void resume() throws SBonitaException {
        // nothing to do
    }

}
