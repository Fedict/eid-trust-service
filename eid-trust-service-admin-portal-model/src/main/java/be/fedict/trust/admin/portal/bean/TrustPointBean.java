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

package be.fedict.trust.admin.portal.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import be.fedict.trust.admin.portal.TrustPoint;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

@Stateful
@Name("trustPoint")
@LocalBinding(jndiBinding = "fedict/eid/trust/admin/portal/TrustPointBean")
public class TrustPointBean implements TrustPoint {

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In(create = true)
	FacesMessages facesMessages;

	@In(value = TrustDomainBean.SELECTED_TRUST_DOMAIN)
	private TrustDomainEntity selectedTrustDomain;

	@In(value = "selectedTrustPoint", required = false)
	private TrustPointEntity selectedTrustPoint;

	private TreeNode<TrustPointEntity> rootNode = null;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeNode<TrustPointEntity> getTreeNode() {
		if (this.rootNode == null) {
			loadTree();
		}
		return this.rootNode;
	}

	private void loadTree() {
		this.rootNode = new TreeNodeImpl<TrustPointEntity>();
		addNodes(null, this.rootNode);
	}

	private void addNodes(String path, TreeNode<TrustPointEntity> node) {

		List<TrustPointEntity> trustPoints = this.trustDomainService
				.listTrustPoints(this.selectedTrustDomain);

		for (TrustPointEntity trustPoint : trustPoints) {
			TreeNodeImpl<TrustPointEntity> nodeImpl = new TreeNodeImpl<TrustPointEntity>();
			nodeImpl.setData(trustPoint);
			node.addChild(trustPoint.getName(), nodeImpl);
		}
	}

}
