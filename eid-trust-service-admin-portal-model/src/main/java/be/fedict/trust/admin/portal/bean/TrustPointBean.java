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

	@In(value = "selectedTrustPoint")
	private TrustPointEntity selectedTrustPoint;

	private TreeNode<TrustPointEntity> rootNode = null;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		log.debug("#destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeNode<TrustPointEntity> getTreeNode() {
		if (rootNode == null) {
			loadTree();
		}
		return rootNode;
	}

	private void loadTree() {
		rootNode = new TreeNodeImpl<TrustPointEntity>();
		addNodes(null, rootNode);
	}

	private void addNodes(String path, TreeNode<TrustPointEntity> node) {

		List<TrustPointEntity> trustPoints = trustDomainService
				.listTrustPoints(selectedTrustDomain);

		for (TrustPointEntity trustPoint : trustPoints) {
			TreeNodeImpl<TrustPointEntity> nodeImpl = new TreeNodeImpl<TrustPointEntity>();
			nodeImpl.setData(trustPoint);
			node.addChild(trustPoint.getName(), nodeImpl);
		}
	}

	// /**
	// * {@inheritDoc}
	// */
	// @SuppressWarnings("unchecked")
	// public TreeModel getTreeModel() {
	//
	// log.debug("get tree model for domain: " + selectedTrustDomain);
	// String domainName = selectedTrustDomain.getName();
	// List<TrustPointEntity> trustPoints = trustDomainService
	// .listTrustPoints(selectedTrustDomain);
	// TreeNode rootNode = new TreeNodeBase(ROOT_NODE_TYPE, domainName, false);
	// TreeModel treeModel = new TreeModelBase(rootNode);
	//
	// HashMap<String, TreeNode> nodes = new HashMap<String, TreeNode>();
	//
	// for (TrustPointEntity trustPoint : trustPoints) {
	// String nodeDescription = trustPoint.getCertificateAuthority()
	// .getCertificate().getSubjectX500Principal().toString();
	// log.debug("adding node: " + nodeDescription);
	// TreeNode newNode = new TrustPointTreeNode(DEFAULT_NODE_TYPE,
	// nodeDescription, true, trustPoint);
	// TreeNode parentNode = nodes.get(trustPoint
	// .getCertificateAuthority().getCertificate()
	// .getIssuerX500Principal().toString());
	// if (null != parentNode) {
	// log.debug("to parent node: " + parentNode.getDescription());
	// parentNode.getChildren().add(newNode);
	// parentNode.setLeaf(false);
	// } else {
	// log.debug("to root node: " + rootNode.getDescription());
	// rootNode.getChildren().add(newNode);
	// rootNode.setLeaf(false);
	// }
	// // be careful for self-signed certs here
	// nodes.put(nodeDescription, newNode);
	// }
	// return treeModel;
	// }

	/**
	 * {@inheritDoc}
	 */
	public String view() {

		log.debug("view");
		// TrustPointTreeNode selectedNode = (TrustPointTreeNode) FacesContext
		// .getCurrentInstance().getExternalContext().getRequestMap().get(
		// "node");
		// TrustPointEntity selectedTrustPoint = selectedNode.getTrustPoint();
		// log.debug("view: " + selectedTrustPoint);
		// FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
		// .put("selectedTrustPoint", selectedTrustPoint);
		// return "view";
		return "view";
	}
}
