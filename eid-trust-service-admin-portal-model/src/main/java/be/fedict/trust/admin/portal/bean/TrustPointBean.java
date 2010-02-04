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

import java.security.cert.CertificateException;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.FacesContext;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import be.fedict.trust.admin.portal.AdminWebappConstants;
import be.fedict.trust.admin.portal.TrustPoint;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;

@Stateful
@Name("trustPoint")
@LocalBinding(jndiBinding = "fedict/eid/trust/admin/portal/TrustPointBean")
public class TrustPointBean implements TrustPoint {

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In
	FacesMessages facesMessages;

	@In(value = TrustDomainBean.SELECTED_TRUST_DOMAIN)
	private TrustDomainEntity selectedTrustDomain;

	@In(value = AdminWebappConstants.TRUST_POINT_SESSION_ATTRIBUTE, required = false)
	private TrustPointEntity selectedTrustPoint;

	private TreeNode<TrustPointEntity> rootNode = null;

	private String crlRefreshCron;

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

	/**
	 * {@inheritDoc}
	 */
	public String remove() {

		this.log.debug("remove trust point: #0", selectedTrustPoint.getName());
		this.trustDomainService.remove(selectedTrustPoint);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String save() {

		this.log.debug("save trust point: #0", this.selectedTrustPoint
				.getName());
		try {
			this.trustDomainService.save(selectedTrustPoint);
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
	public String add() {

		this.log.debug("add trust point: crlRefreshCron=#0",
				this.crlRefreshCron);

		byte[] certificateBytes = (byte[]) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get(
						AdminWebappConstants.CERTIFICATE_SESSION_ATTRIBUTE);
		if (null == certificateBytes) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorNoCertificate");
			return null;
		}

		try {
			this.trustDomainService.addTrustPoint(this.crlRefreshCron,
					this.selectedTrustDomain, certificateBytes);
		} catch (CertificateException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorX509Encoding");
			return null;
		} catch (TrustPointAlreadyExistsException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR,
					"errorTrustPointAlreadyExists");
			return null;
		} catch (InvalidCronExpressionException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorCronExpressionInvalid");
			return null;
		} finally {
			FacesContext.getCurrentInstance().getExternalContext()
					.getSessionMap().remove(
							AdminWebappConstants.CERTIFICATE_SESSION_ATTRIBUTE);
		}

		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCrlRefreshCron() {

		return crlRefreshCron;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCrlRefreshCron(String crlRefreshCron) {

		this.crlRefreshCron = crlRefreshCron;
	}
}
