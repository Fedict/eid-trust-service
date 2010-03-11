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

package be.fedict.trust.service.bean;

import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.LocalizationService;
import be.fedict.trust.service.dao.LocalizationDAO;
import be.fedict.trust.service.entity.LocalizationKeyEntity;
import be.fedict.trust.service.entity.LocalizationTextEntity;

/**
 * Localization Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class LocalizationServiceBean implements LocalizationService {

	private static final Log LOG = LogFactory
			.getLog(LocalizationServiceBean.class);

	@EJB
	private LocalizationDAO localizationDAO;

	/**
	 * {@inheritDoc}
	 */
	public String findText(String key, Locale locale) {

		LOG.debug("find text: " + key + " language=" + locale.getLanguage());
		LocalizationKeyEntity localizationKey = this.localizationDAO
				.findLocalization(key);
		if (null != localizationKey) {
			for (LocalizationTextEntity text : localizationKey.getTexts()) {
				if (text.getLanguage().equals(locale.getLanguage())) {
					return text.getText();
				}
			}
		}
		return null;
	}
}
