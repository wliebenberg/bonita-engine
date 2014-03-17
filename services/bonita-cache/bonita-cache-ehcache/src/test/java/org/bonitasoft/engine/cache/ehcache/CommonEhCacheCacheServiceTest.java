package org.bonitasoft.engine.cache.ehcache;

import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.bonitasoft.engine.cache.CacheConfigurations;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CommonEhCacheCacheServiceTest {

    @Mock
    private CacheConfigurations cacheConfigurations;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private TechnicalLoggerService logger;

    @Mock
    private ReadSessionAccessor sessionAccessor;

    @Mock
    private Cache cache;

    private CommonEhCacheCacheService cacheService;

    @Before
    public void setup() {
        cacheService = new EhCacheCacheService(logger, sessionAccessor, cacheConfigurations) {

            @Override
            public synchronized void start() throws SBonitaException {
                cacheManager = CommonEhCacheCacheServiceTest.this.cacheManager;
            }
        };
    }

    @Test
    public void should_getKeys_return_empty_list_when_cache_manager_is_null() throws Exception {
        List<Object> keys = cacheService.getKeys("unknownCache");

        assertTrue(keys.isEmpty());
    }

    @Test
    public void should_getKeys_return_empty_list_when_cache_manager_have_no_cache() throws Exception {
        cacheService.start();

        List<Object> keys = cacheService.getKeys("unknownCache");

        assertTrue(keys.isEmpty());
    }

    // cache is final can't change methods... use powermock...
    // @Test
    // public void should_getKeys_return_cache_getKeys() throws Exception {
    // List<String> values = Arrays.asList("a", "b");
    // when(cacheManager.getCache("knownCache")).thenReturn(cache);
    // cacheService.start();
    // when(cache.getKeys()).thenReturn(values);
    //
    // List<Object> keys = cacheService.getKeys("knownCache");
    //
    // assertEquals(values, keys);
    // }

}
