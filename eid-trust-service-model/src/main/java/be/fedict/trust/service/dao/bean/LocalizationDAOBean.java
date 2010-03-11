/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.trust.service.dao.bean;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.dao.LocalizationDAO;
import be.fedict.trust.service.entity.LocalizationKeyEntity;
import be.fedict.trust.service.entity.LocalizationTextEntity;

/**
 * Localization DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class LocalizationDAOBean implements LocalizationDAO {

	private static final Log LOG = LogFactory.getLog(LocalizationDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public LocalizationKeyEntity addLocalization(String key,
			Map<Locale, String> textEntries) {

		LOG.debug("add localization: " + key);
		LocalizationKeyEntity localizationKey = new LocalizationKeyEntity(key);
		this.entityManager.persist(localizationKey);
		this.entityManager.flush();

		Set<LocalizationTextEntity> texts = new HashSet<LocalizationTextEntity>();
		for (Entry<Locale, String> textEntry : textEntries.entrySet()) {
			LocalizationTextEntity text = new LocalizationTextEntity(
					localizationKey, textEntry.getKey().getLanguage(),
					textEntry.getValue());
			this.entityManager.persist(text);
			texts.add(text);
		}
		this.entityManager.flush();
		localizationKey.setTexts(texts);

		return localizationKey;
	}

	/**
	 * {@inheritDoc}
	 */
	public LocalizationKeyEntity findLocalization(String key) {

		LOG.debug("find localization: " + key);
		return this.entityManager.find(LocalizationKeyEntity.class, key);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeLocalization(String key) {

		LOG.debug("remove localization: " + key);
		LocalizationKeyEntity localizationKey = findLocalization(key);
		if (null != localizationKey) {
			this.entityManager.remove(localizationKey);
		}
	}

}
