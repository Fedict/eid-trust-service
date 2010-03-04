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

import java.io.IOException;
import java.util.List;

import javax.ejb.Local;
import javax.faces.model.SelectItem;

import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.TreeNode;

import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;

@Local
public interface TrustDomain {

	/*
	 * Factory
	 */
	void trustDomainListFactory();

	void constraintsPolicyFactory();

	void constraintsKeyUsageFactory();

	List<SelectItem> keyUsageTypeFactory();

	void constraintsEndEntityFactory();

	/*
	 * Lifecycle.
	 */
	void destroyCallback();

	/*
	 * Accessors
	 */
	String getName();

	void setName(String name);

	List<String> getSourceTrustPoints();

	void setSourceTrustPoints(List<String> sourceTrustPoints);

	List<String> getSelectedTrustPoints();

	void setSelectedTrustPoints(List<String> selectedTrustPoints);

	String getCertificatePolicy();

	void setCertificatePolicy(String certificatePolicy);

	String getKeyUsage();

	void setKeyUsage(String keyUsage);

	boolean isAllowed();

	void setAllowed(boolean allowed);

	String getDn();

	void setDn(String dn);

	DNConstraintEntity getDnConstraint();

	boolean isQc();

	void setQc(boolean qc);

	QCStatementsConstraintEntity getQcConstraint();

	/*
	 * Actions
	 */
	String modify();

	void select();

	String remove();

	String add();

	String save();

	String setDefault();

	String selectTrustPoints();

	void initSelect();

	String saveSelect();

	String back();

	String removeConstraintPolicy();

	String addConstraintPolicy();

	String removeConstraintKeyUsage();

	String saveConstraintKeyUsage();

	String addConstraintKeyUsage();

	String addConstraintDn();

	String saveConstraintDn();

	String removeConstraintDn();

	String removeConstraintEndEntity();

	String addConstraintEndEntity();

	String addConstraintQc();

	String saveConstraintQc();

	String removeConstraintQc();

	/*
	 * Trust points tree
	 */
	TreeNode<TrustPointEntity> getTreeNode();

	/*
	 * Richfaces component callbacks
	 */
	void processNodeSelection(NodeSelectedEvent event);

	void uploadListener(UploadEvent event) throws IOException;

}
