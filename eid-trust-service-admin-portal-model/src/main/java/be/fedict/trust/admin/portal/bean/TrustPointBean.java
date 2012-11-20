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
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.jms.JMSException;

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
import org.jboss.seam.annotations.security.Admin;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.TrustPoint;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "trustPoint")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "TrustPointBean")
public class TrustPointBean implements TrustPoint {

	public static final String SELECTED_TRUST_POINT = "selectedTrustPoint";
	private static final String UPLOADED_CERTIFICATE = "uploadedCertificate";
	private static final String TRUST_POINT_LIST_NAME = AdminConstants.ADMIN_SEAM_PREFIX
			+ "trustPointList";
	private static final String TRUST_POINT_CA_LIST_NAME = AdminConstants.ADMIN_SEAM_PREFIX
			+ "trustPointCAList";

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@DataModel(TRUST_POINT_LIST_NAME)
	private List<TrustPointEntity> trustPointList;

	@DataModelSelection(TRUST_POINT_LIST_NAME)
	@In(value = SELECTED_TRUST_POINT, required = false)
	@Out(value = SELECTED_TRUST_POINT, required = false, scope = ScopeType.CONVERSATION)
	private TrustPointEntity selectedTrustPoint;

	@SuppressWarnings("unused")
	@DataModel(TRUST_POINT_CA_LIST_NAME)
	private List<CertificateAuthorityEntity> caList;

	@DataModelSelection(TRUST_POINT_CA_LIST_NAME)
	private CertificateAuthorityEntity selectedCA;

	@In(value = UPLOADED_CERTIFICATE, required = false)
	@Out(value = UPLOADED_CERTIFICATE, required = false, scope = ScopeType.CONVERSATION)
	private byte[] certificateBytes;

	@In(value = TrustDomainBean.SELECTED_TRUST_DOMAIN, required = false)
	private TrustDomainEntity selectedTrustDomain;

	private String crlRefreshCronSchedule;

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
	@Factory(TRUST_POINT_LIST_NAME)
	public void trustPointListFactory() {

		this.log.debug("trust point list factory");
		this.trustPointList = this.trustDomainService.listTrustPoints();
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(TRUST_POINT_CA_LIST_NAME)
	public void trustPointCAListFactory() {

		this.log.debug("trust point CA list factory");
		this.caList = this.trustDomainService
				.listTrustPointCAs(this.selectedTrustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	@Admin
	public String modify() {

		this.log.debug("modify: #0", this.selectedTrustPoint.getName());
		this.crlRefreshCronSchedule = this.selectedTrustPoint
				.getCrlRefreshCronSchedule();
		trustPointCAListFactory();
		return "modify";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public void select() {

		this.log.debug("selected trust point: #0", this.selectedTrustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String remove() {

		this.log.debug("remove trust point: #0",
				this.selectedTrustPoint.getName());
		this.trustDomainService.removeTrustPoint(this.selectedTrustPoint);
		this.selectedTrustPoint = null;
		trustPointListFactory();
		if (null != this.selectedTrustDomain) {
			return "success_trustdomain";
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String save() {

		if (null != this.selectedTrustPoint) {
			this.selectedTrustPoint
					.setCrlRefreshCronSchedule(this.crlRefreshCronSchedule);
			this.log.debug("save trust point: #0",
					this.selectedTrustPoint.getName());
			try {
				this.trustDomainService.save(this.selectedTrustPoint);
			} catch (InvalidCronExpressionException e) {
				this.facesMessages.addToControlFromResourceBundle(
						"cronSchedule", StatusMessage.Severity.ERROR,
						"errorCronExpressionInvalid");
				return null;
			}
			this.crlRefreshCronSchedule = null;
		}
		if (null != this.selectedTrustDomain) {
			return "success_trustdomain";
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	public String back() {

		if (null != this.selectedTrustDomain) {
			return "back_trustdomain";
		}
		return "back";

	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String add() {

		this.log.debug("add trust point: crlRefreshCronSchedule=#0",
				this.crlRefreshCronSchedule);

		if (null == this.certificateBytes) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorNoCertificate");
			return null;
		}

		try {
			this.trustDomainService.addTrustPoint(this.crlRefreshCronSchedule,
					this.certificateBytes);
			this.crlRefreshCronSchedule = null;
			this.certificateBytes = null;
			trustPointListFactory();
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
			this.facesMessages.addToControlFromResourceBundle("cronSchedule",
					StatusMessage.Severity.ERROR, "errorCronExpressionInvalid");
			return null;
		}

		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Admin
	public String refresh() {

		this.log.debug("refresh trust point: #0",
				this.selectedTrustPoint.getName());

		this.trustDomainService.refreshTrustPointCache(this.selectedTrustPoint);

		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Admin
	public String refreshCA() {

		this.log.debug("refresh CA: #0", this.selectedCA.getName());

		try {
			this.trustDomainService.refreshCACache(this.selectedCA);
		} catch (JMSException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorHarvesterNotification");
			return null;
		}

		return "refresh";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCrlRefreshCronSchedule() {

		return this.crlRefreshCronSchedule;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCrlRefreshCronSchedule(String crlRefreshCronSchedule) {

		this.crlRefreshCronSchedule = crlRefreshCronSchedule;
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
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
}
