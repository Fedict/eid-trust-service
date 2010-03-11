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

package be.fedict.trust.service;

import java.util.Locale;

import javax.ejb.Local;

import be.fedict.trust.service.entity.LocalizationTextEntity;

/**
 * Localization service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface LocalizationService {

	/**
	 * Return the {@link LocalizationTextEntity} for the specified key and
	 * {@link Locale}. Returns <code>null</code> if not found.
	 * 
	 * @param key
	 * @param locale
	 */
	String findText(String key, Locale locale);
}
