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

package be.fedict.trust.admin.portal.bean;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FileUtils;
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
import org.richfaces.event.UploadEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.richfaces.model.UploadItem;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.TrustDomain;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.VirtualTrustDomainEntity;
import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.exception.VirtualTrustDomainNotFoundException;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "trustDomain")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "TrustDomainBean")
public class TrustDomainBean implements TrustDomain {

	public static final String SELECTED_TRUST_DOMAIN = "selectedTrustDomain";
	public static final String SELECTED_VIRTUAL_TRUST_DOMAIN = "selectedVirtualTrustDomain";
	private static final String TRUST_DOMAIN_LIST_NAME = "trustDomainList";
	private static final String VIRTUAL_TRUST_DOMAIN_LIST_NAME = "virtualTrustDomainList";

	private static final String CONSTRAINTS_POLICY_LIST = "constraintsPolicies";
	private static final String CONSTRAINTS_KEY_USAGE_LIST = "constraintsKeyUsage";
	private static final String KEY_USAGE_TYPE_LIST = "keyUsageTypes";
	private static final String CONSTRAINTS_END_ENTITY_LIST = "constraintsEndEntity";

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

	@SuppressWarnings("unused")
	@DataModel(VIRTUAL_TRUST_DOMAIN_LIST_NAME)
	private List<VirtualTrustDomainEntity> virtualTrustDomainList;

	@DataModelSelection(VIRTUAL_TRUST_DOMAIN_LIST_NAME)
	@Out(value = SELECTED_VIRTUAL_TRUST_DOMAIN, required = false, scope = ScopeType.CONVERSATION)
	@In(required = false)
	private VirtualTrustDomainEntity selectedVirtualTrustDomain;

	@Out(value = TrustPointBean.SELECTED_TRUST_POINT, required = false, scope = ScopeType.CONVERSATION)
	private TrustPointEntity selectedTrustPoint;

	private TreeNode<TrustPointEntity> rootNode = null;
	private TreeNode<TrustDomainEntity> rootNodeVirtual = null;

	private List<String> sourceTrustPoints;
	private List<String> selectedTrustPoints;

	private List<String> sourceTrustDomains;
	private List<String> selectedTrustDomains;

	@DataModel(CONSTRAINTS_POLICY_LIST)
	private List<PolicyConstraintEntity> policyConstraints;

	@DataModelSelection(CONSTRAINTS_POLICY_LIST)
	private PolicyConstraintEntity selectedPolicyConstraint;

	@DataModel(CONSTRAINTS_KEY_USAGE_LIST)
	private List<KeyUsageConstraintEntity> keyUsageConstraints;

	@DataModelSelection(CONSTRAINTS_KEY_USAGE_LIST)
	private KeyUsageConstraintEntity selectedKeyUsageConstraint;

	@DataModel(CONSTRAINTS_END_ENTITY_LIST)
	private List<EndEntityConstraintEntity> endEntityConstraints;

	@DataModelSelection(CONSTRAINTS_END_ENTITY_LIST)
	private EndEntityConstraintEntity selectedEndEntityConstraint;

	private String name;
	private String nameVirtual;
	private String certificatePolicy;
	private String keyUsage;
	private boolean allowed;
	private DNConstraintEntity dnConstraint;
	private String dn;
	private byte[] certificateBytes;
	private QCStatementsConstraintEntity qcConstraint;
	private boolean qc;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
		this.name = null;
		this.sourceTrustPoints = null;
		this.selectedTrustPoints = null;
		this.sourceTrustDomains = null;
		this.selectedTrustDomains = null;
		this.certificatePolicy = null;
		this.keyUsage = null;
		this.allowed = false;
		this.dnConstraint = null;
		this.dn = null;
		this.certificateBytes = null;
		this.qcConstraint = null;
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
	@Factory(VIRTUAL_TRUST_DOMAIN_LIST_NAME)
	public void virtualTrustDomainListFactory() {

		this.log.debug("virtual trust domain list factory");
		this.virtualTrustDomainList = this.trustDomainService
				.listVirtualTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String modify() {

		this.log.debug("modify trust domain: #0", this.selectedTrustDomain
				.getName());
		for (CertificateConstraintEntity certificateConstraint : this.selectedTrustDomain
				.getCertificateConstraints()) {
			if (certificateConstraint instanceof DNConstraintEntity) {
				this.dnConstraint = (DNConstraintEntity) certificateConstraint;
				this.dn = this.dnConstraint.getDn();
			} else if (certificateConstraint instanceof QCStatementsConstraintEntity) {
				this.qcConstraint = (QCStatementsConstraintEntity) certificateConstraint;
				this.qc = this.qcConstraint.getQcComplianceFilter();
			}
		}
		return "modify";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String modifyVirtual() {

		this.log.debug("modify virtual trust domain: #0",
				this.selectedVirtualTrustDomain.getName());
		return "modify_virtual";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String add() {

		this.log.debug("add trust domain #0", this.name);
		this.selectedTrustDomain = this.trustDomainService
				.addTrustDomain(this.name);
		return "modify";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String addVirtual() {

		this.log.debug("add virtual trust domain #0", this.nameVirtual);
		this.selectedVirtualTrustDomain = this.trustDomainService
				.addVirtualTrustDomain(this.nameVirtual);
		return "modify_virtual";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public void select() {

		this.log.debug("selected trust domain: #0", this.selectedTrustDomain
				.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public void selectVirtual() {

		this.log.debug("selected virtual trust domain: #0",
				this.selectedVirtualTrustDomain.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String remove() {

		this.log.debug("remove: #0", this.selectedTrustDomain.getName());
		this.trustDomainService.removeTrustDomain(this.selectedTrustDomain);
		trustDomainListFactory();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String removeVirtual() {

		this.log.debug("remove virtual: #0", this.selectedVirtualTrustDomain
				.getName());
		this.trustDomainService
				.removeVirtualTrustDomain(this.selectedVirtualTrustDomain);
		virtualTrustDomainListFactory();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String back() {

		trustDomainListFactory();
		virtualTrustDomainListFactory();
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
	public TreeNode<TrustDomainEntity> getTreeNodeVirtual() {
		if (this.rootNodeVirtual == null) {
			loadTreeVirtual();
		}
		return this.rootNodeVirtual;
	}

	private void loadTreeVirtual() {
		this.rootNodeVirtual = new TreeNodeImpl<TrustDomainEntity>();
		addNodesVirtual(null, this.rootNodeVirtual);
	}

	private void addNodesVirtual(String path, TreeNode<TrustDomainEntity> node) {

		for (TrustDomainEntity trustDomain : this.selectedVirtualTrustDomain
				.getTrustDomains()) {
			TreeNodeImpl<TrustDomainEntity> nodeImpl = new TreeNodeImpl<TrustDomainEntity>();
			nodeImpl.setData(trustDomain);
			node.addChild(trustDomain.getName(), nodeImpl);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Begin(join = true)
	public void processNodeSelection(NodeSelectedEvent event) {

		HtmlTree tree = (HtmlTree) event.getComponent();
		TreeNode<TrustPointEntity> currentNode = tree.getModelTreeNode(tree
				.getRowKey());
		this.selectedTrustPoint = (TrustPointEntity) currentNode.getData();
		this.log.debug("view trust point: #0", selectedTrustPoint.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Begin(join = true)
	public void processNodeSelectionVirtual(NodeSelectedEvent event) {

		HtmlTree tree = (HtmlTree) event.getComponent();
		TreeNode<TrustDomainEntity> currentNode = tree.getModelTreeNode(tree
				.getRowKey());
		this.selectedTrustDomain = (TrustDomainEntity) currentNode.getData();
		this.log.debug("view trust domain: #0", selectedTrustDomain.getName());
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
	public String selectTrustDomains() {

		this.log.debug("select trust domainsfor virtual trust domain: #0",
				this.selectedVirtualTrustDomain.getName());
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
	public void initSelectVirtual() {

		this.log.debug("#init select virtual");
		if (null != this.selectedVirtualTrustDomain) {
			this.selectedTrustDomains = new LinkedList<String>();
			for (TrustDomainEntity trustDomain : this.selectedVirtualTrustDomain
					.getTrustDomains()) {
				this.selectedTrustDomains.add(trustDomain.getName());
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

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSourceTrustDomains() {

		this.sourceTrustDomains = new LinkedList<String>();
		for (TrustDomainEntity trustDomain : this.trustDomainService
				.listTrustDomains()) {
			if (null != this.selectedTrustDomains
					&& !this.selectedTrustDomains.contains(trustDomain
							.getName()))
				this.sourceTrustDomains.add(trustDomain.getName());
		}
		return this.sourceTrustDomains;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSourceTrustDomains(List<String> sourceTrustDomains) {

		this.sourceTrustDomains = sourceTrustDomains;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getSelectedTrustDomains() {

		return this.selectedTrustDomains;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelectedTrustDomains(List<String> selectedTrustDomains) {

		this.selectedTrustDomains = selectedTrustDomains;
	}

	/**
	 * {@inheritDoc}
	 */
	public String saveSelectVirtual() {

		try {
			this.selectedVirtualTrustDomain = this.trustDomainService
					.setTrustDomains(this.selectedVirtualTrustDomain,
							this.selectedTrustDomains);
			loadTreeVirtual();
		} catch (VirtualTrustDomainNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorTrustDomainNotFound");
			return null;
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(CONSTRAINTS_POLICY_LIST)
	public void constraintsPolicyFactory() {

		this.log.debug("certificate policy constraints factory");
		this.policyConstraints = new LinkedList<PolicyConstraintEntity>();
		for (CertificateConstraintEntity certificateConstraint : this.selectedTrustDomain
				.getCertificateConstraints()) {
			if (certificateConstraint instanceof PolicyConstraintEntity) {
				this.policyConstraints
						.add((PolicyConstraintEntity) certificateConstraint);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String removeConstraintPolicy() {

		if (null != this.selectedPolicyConstraint) {
			this.log.debug("remove policy constraint: #0",
					this.selectedPolicyConstraint.getPolicy());
			this.trustDomainService
					.removeCertificatePolicy(this.selectedPolicyConstraint);
			this.selectedTrustDomain.getCertificateConstraints().remove(
					this.selectedPolicyConstraint);
			constraintsPolicyFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String addConstraintPolicy() {

		if (null != this.certificatePolicy) {
			PolicyConstraintEntity policyConstraint = this.trustDomainService
					.addCertificatePolicy(this.selectedTrustDomain,
							this.certificatePolicy);
			this.selectedTrustDomain.getCertificateConstraints().add(
					policyConstraint);
			constraintsPolicyFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(KEY_USAGE_TYPE_LIST)
	public List<SelectItem> keyUsageTypeFactory() {

		List<SelectItem> keyUsageTypes = new LinkedList<SelectItem>();
		for (KeyUsageType keyUsageType : KeyUsageType.values()) {
			keyUsageTypes.add(new SelectItem(keyUsageType.name(), keyUsageType
					.toString()));
		}
		return keyUsageTypes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(CONSTRAINTS_KEY_USAGE_LIST)
	public void constraintsKeyUsageFactory() {

		this.log.debug("key usage constraints factory");
		this.keyUsageConstraints = new LinkedList<KeyUsageConstraintEntity>();
		for (CertificateConstraintEntity certificateConstraint : this.selectedTrustDomain
				.getCertificateConstraints()) {
			if (certificateConstraint instanceof KeyUsageConstraintEntity) {
				this.keyUsageConstraints
						.add((KeyUsageConstraintEntity) certificateConstraint);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String addConstraintKeyUsage() {

		if (null != this.keyUsage) {
			KeyUsageConstraintEntity keyUsageConstraint = this.trustDomainService
					.addKeyUsageConstraint(this.selectedTrustDomain,
							KeyUsageType.valueOf(this.keyUsage), this.allowed);
			this.selectedTrustDomain.getCertificateConstraints().add(
					keyUsageConstraint);
			constraintsKeyUsageFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String removeConstraintKeyUsage() {

		if (null != this.selectedKeyUsageConstraint) {
			this.log.debug("remove key usage constraint: #0",
					this.selectedKeyUsageConstraint.getKeyUsage());
			this.trustDomainService
					.removeKeyUsageConstraint(this.selectedKeyUsageConstraint);
			this.selectedTrustDomain.getCertificateConstraints().remove(
					this.selectedKeyUsageConstraint);
			constraintsKeyUsageFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String saveConstraintKeyUsage() {

		this.log.debug("save key usage constraints");
		this.trustDomainService
				.saveKeyUsageConstraints(this.keyUsageConstraints);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String addConstraintDn() {

		this.log.debug("Add DN Statements constraint: #0", this.dn);
		this.dnConstraint = this.trustDomainService.addDNConstraint(
				this.selectedTrustDomain, this.dn);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String removeConstraintDn() {

		if (null != this.dnConstraint) {
			this.log.debug("Remove DN Statements constraint");
			this.trustDomainService.removeDNConstraint(this.dnConstraint);
			this.dnConstraint = null;
			this.dn = null;
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String saveConstraintDn() {

		this.log.debug("Save DN Statements constraint: #0", this.dn);
		this.dnConstraint.setDn(this.dn);
		this.trustDomainService.saveDNConstraint(this.dnConstraint);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(CONSTRAINTS_END_ENTITY_LIST)
	public void constraintsEndEntityFactory() {

		this.log.debug("end entity constraints factory");
		this.endEntityConstraints = new LinkedList<EndEntityConstraintEntity>();
		for (CertificateConstraintEntity certificateConstraint : this.selectedTrustDomain
				.getCertificateConstraints()) {
			if (certificateConstraint instanceof EndEntityConstraintEntity) {
				this.endEntityConstraints
						.add((EndEntityConstraintEntity) certificateConstraint);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String addConstraintEndEntity() {

		if (null != this.certificateBytes) {
			EndEntityConstraintEntity endEntityConstraint;
			try {
				endEntityConstraint = this.trustDomainService
						.addEndEntityConstraint(this.selectedTrustDomain,
								this.certificateBytes);
			} catch (CertificateException e) {
				this.facesMessages.addFromResourceBundle(
						StatusMessage.Severity.ERROR, "errorX509Encoding");
				return null;
			}
			this.selectedTrustDomain.getCertificateConstraints().add(
					endEntityConstraint);
			constraintsEndEntityFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String removeConstraintEndEntity() {

		if (null != this.selectedEndEntityConstraint) {
			this.log.debug("remove end entity constraint: #0 #1",
					this.selectedEndEntityConstraint.getIssuerName(),
					this.selectedEndEntityConstraint.getSerialNumber());
			this.trustDomainService
					.removeEndEntityConstraint(this.selectedEndEntityConstraint);
			this.selectedTrustDomain.getCertificateConstraints().remove(
					this.selectedEndEntityConstraint);
			constraintsEndEntityFactory();
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String addConstraintQc() {

		this.log.debug("Add QC Statements constraint: #0", this.qc);
		this.qcConstraint = this.trustDomainService.addQCConstraint(
				this.selectedTrustDomain, this.qc);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String removeConstraintQc() {

		if (null != this.qcConstraint) {
			this.log.debug("Remove QC Statements constraint");
			this.trustDomainService.removeQCConstraint(this.qcConstraint);
			this.qcConstraint = null;
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String saveConstraintQc() {

		this.log.debug("Save QC Statements constraint: #0", this.qc);
		this.qcConstraint.setQcComplianceFilter(this.qc);
		this.trustDomainService.saveQCConstraint(this.qcConstraint);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCertificatePolicy() {

		return this.certificatePolicy;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCertificatePolicy(String certificatePolicy) {

		this.certificatePolicy = certificatePolicy;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getKeyUsage() {

		return this.keyUsage;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setKeyUsage(String keyUsage) {

		this.keyUsage = keyUsage;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isAllowed() {

		return this.allowed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAllowed(boolean allowed) {

		this.allowed = allowed;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDn() {

		return this.dn;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDn(String dn) {

		this.dn = dn;
	}

	/**
	 * {@inheritDoc}
	 */
	public DNConstraintEntity getDnConstraint() {

		return this.dnConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isQc() {

		return this.qc;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setQc(boolean qc) {

		this.qc = qc;
	}

	/**
	 * {@inheritDoc}
	 */
	public QCStatementsConstraintEntity getQcConstraint() {

		return this.qcConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public void uploadListener(UploadEvent event) throws IOException {
		UploadItem item = event.getUploadItem();
		this.log.debug(item.getContentType());
		this.log.debug(item.getFileSize());
		this.log.debug(item.getFileName());
		if (null == item.getData()) {
			// meaning createTempFiles is set to true in the SeamFilter
			this.certificateBytes = FileUtils.readFileToByteArray(item
					.getFile());
		} else {
			this.certificateBytes = item.getData();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {

		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setName(String name) {

		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getNameVirtual() {

		return this.nameVirtual;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNameVirtual(String nameVirtual) {

		this.nameVirtual = nameVirtual;
	}
}
