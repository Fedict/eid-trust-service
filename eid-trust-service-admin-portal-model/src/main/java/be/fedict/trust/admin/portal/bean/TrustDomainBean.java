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

import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import be.fedict.trust.admin.portal.TrustDomain;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;

@Stateful
@Name("trustDomain")
@LocalBinding(jndiBinding = "fedict/eid/trust/admin/portal/TrustDomainBean")
public class TrustDomainBean implements TrustDomain {

	public static final String SELECTED_TRUST_DOMAIN = "selectedTrustDomain";
	private static final String TRUST_DOMAIN_LIST_NAME = "trustDomainList";

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@DataModel(TRUST_DOMAIN_LIST_NAME)
	private List<TrustDomainEntity> trustDomainList;

	@DataModelSelection(TRUST_DOMAIN_LIST_NAME)
	@Out(value = SELECTED_TRUST_DOMAIN, required = false, scope = ScopeType.CONVERSATION)
	@In(required = false)
	private TrustDomainEntity selectedTrustDomain;

	@Out(value = TrustPointBean.SELECTED_TRUST_POINT, required = false, scope = ScopeType.SESSION)
	private TrustPointEntity selectedTrustPoint;

	private TreeNode<TrustPointEntity> rootNode = null;

	private List<String> sourceTrustPoints;
	private List<String> selectedTrustPoints;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
		this.sourceTrustPoints = null;
		this.selectedTrustPoints = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(TRUST_DOMAIN_LIST_NAME)
	public void trustDomainListFactory() {

		this.log.debug("trust domain list factory");
		this.trustDomainList = this.trustDomainService.listTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String modify() {

		this.log.debug("modify: #0", this.selectedTrustDomain.getName());
		return "modify";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String back() {

		return "back";
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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void processNodeSelection(NodeSelectedEvent event) {

		HtmlTree tree = (HtmlTree) event.getComponent();
		TreeNode<TrustPointEntity> currentNode = tree.getModelTreeNode(tree
				.getRowKey());
		this.selectedTrustPoint = (TrustPointEntity) currentNode.getData();
		this.log.debug("view: " + selectedTrustPoint.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String save() {

		this.log.debug("save trust domain: #0 ", this.selectedTrustDomain
				.getName());
		try {
			this.trustDomainService.save(selectedTrustDomain);
		} catch (InvalidCronExpressionException e) {
			this.facesMessages.addToControlFromResourceBundle("cron",
					StatusMessage.Severity.ERROR, "errorCronExpressionInvalid");
			return null;
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String setDefault() {

		this.log.debug("set default trust domain: #0", this.selectedTrustDomain
				.getName());
		this.trustDomainService.setDefault(this.selectedTrustDomain);
		trustDomainListFactory();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String selectTrustPoints() {

		this.log.debug("select trust points for trust domain: #0",
				this.selectedTrustDomain.getName());
		return "select";
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSelect() {

		this.log.debug("#init select");
		if (null != this.selectedTrustDomain) {
			this.selectedTrustPoints = new LinkedList<String>();
			for (TrustPointEntity trustPoint : this.trustDomainService
					.listTrustPoints(this.selectedTrustDomain)) {
				this.selectedTrustPoints.add(trustPoint.getName());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSourceTrustPoints() {

		List<TrustPointEntity> trustPoints = this.trustDomainService
				.listTrustPoints();
		this.sourceTrustPoints = new LinkedList<String>();
		for (TrustPointEntity trustPoint : trustPoints) {
			if (null != this.selectedTrustPoints
					&& !this.selectedTrustPoints.contains(trustPoint.getName()))
				this.sourceTrustPoints.add(trustPoint.getName());
		}
		return this.sourceTrustPoints;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSourceTrustPoints(List<String> sourceTrustPoints) {

		this.sourceTrustPoints = sourceTrustPoints;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSelectedTrustPoints() {

		return this.selectedTrustPoints;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelectedTrustPoints(List<String> selectedTrustPoints) {

		this.selectedTrustPoints = selectedTrustPoints;
	}

	/**
	 * {@inheritDoc}
	 */
	public String saveSelect() {

		try {
			this.trustDomainService.setTrustPoints(this.selectedTrustDomain,
					this.selectedTrustPoints);
			loadTree();
		} catch (TrustDomainNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorTrustDomainNotFound");
			return null;
		}
		return "success";
	}
}
