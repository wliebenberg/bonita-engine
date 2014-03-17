/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.cache.ehcache;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bonitasoft.engine.cache.CacheConfigurations;
import org.bonitasoft.engine.cache.CacheException;
import org.bonitasoft.engine.cache.CacheService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.sessionaccessor.TenantIdNotSetException;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Hongwen Zang
 */
public class EhCacheCacheService extends CommonEhCacheCacheService implements CacheService {

    public EhCacheCacheService(final TechnicalLoggerService logger, final ReadSessionAccessor sessionAccessor, final CacheConfigurations cacheConfigurations,
            final URL configFile) {
        super(logger, sessionAccessor, cacheConfigurations, configFile);
    }

    public EhCacheCacheService(final TechnicalLoggerService logger, final ReadSessionAccessor sessionAccessor, final CacheConfigurations cacheConfigurations) {
        super(logger, sessionAccessor, cacheConfigurations);
    }

    /**
     * @param cacheName
     * @return
     * @throws CacheException
     */
    @Override
    protected String getKeyFromCacheName(final String cacheName) throws CacheException {
        try {
            return String.valueOf(sessionAccessor.getTenantId()) + '_' + cacheName;
        } catch (final TenantIdNotSetException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public List<String> getCachesNames() {
        if (cacheManager == null) {
            return Collections.emptyList();
        }
        final String[] cacheNames = cacheManager.getCacheNames();
        final ArrayList<String> cacheNamesList = new ArrayList<String>(cacheNames.length);
        String prefix;
        try {
            prefix = String.valueOf(sessionAccessor.getTenantId()) + '_';
            for (final String cacheName : cacheNames) {
                if (cacheName.startsWith(prefix)) {
                    cacheNamesList.add(getCacheNameFromKey(cacheName));
                }
            }
        } catch (final TenantIdNotSetException e) {
            throw new RuntimeException(e);
        }
        return cacheNamesList;
    }

    /**
     * @param cacheName
     * @return
     */
    private String getCacheNameFromKey(final String cacheNameKey) {
        return cacheNameKey.substring(cacheNameKey.indexOf('_') + 1);
    }

}
