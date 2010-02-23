/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.trust.admin.portal;

import java.util.List;

import javax.ejb.Local;

import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;

import be.fedict.trust.service.entity.TrustPointEntity;

@Local
public interface TrustDomain {

	/*
	 * Factory
	 */
	void trustDomainListFactory();

	/*
	 * Lifecycle.
	 */
	void destroyCallback();

	/*
	 * Accessors
	 */
	List<String> getSourceTrustPoints();

	void setSourceTrustPoints(List<String> sourceTrustPoints);

	List<String> getSelectedTrustPoints();

	void setSelectedTrustPoints(List<String> selectedTrustPoints);

	/*
	 * Actions
	 */
	String modify();

	String save();

	String setDefault();

	String selectTrustPoints();

	void initSelect();

	String saveSelect();

	String back();

	/*
	 * Trust points tree
	 */
	TreeNode<TrustPointEntity> getTreeNode();

	/*
	 * Richfaces component callbacks
	 */
	void processNodeSelection(NodeSelectedEvent event);

}
