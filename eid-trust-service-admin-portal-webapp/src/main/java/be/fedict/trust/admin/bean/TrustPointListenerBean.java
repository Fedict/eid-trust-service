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

package be.fedict.trust.admin.bean;

import java.io.IOException;

import javax.ejb.Remove;
import javax.faces.context.FacesContext;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.UploadItem;

import be.fedict.trust.admin.portal.AdminWebappConstants;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Have to create a seperate Seam POJO to resolve class loading issues with the
 * richfaces-api artifact when shared between the webapp in its seam backing
 * bean artifact.
 * 
 * http ://www.seamframework.org/Community/HowToConfigureClassLoadingInSEAMAPP
 * 
 * @author wvdhaute
 */
@Name("trustPointListener")
public class TrustPointListenerBean {

	@Logger
	private Log log;

	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
	}

	/**
	 * {@link TrustPointEntity} richfaces tree node listener.
	 * 
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void processNodeSelection(NodeSelectedEvent event) {

		HtmlTree tree = (HtmlTree) event.getComponent();
		TreeNode<TrustPointEntity> currentNode = tree.getModelTreeNode(tree
				.getRowKey());
		TrustPointEntity selectedTrustPoint = (TrustPointEntity) currentNode
				.getData();
		this.log.debug("view: " + selectedTrustPoint.getName());
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
				.put(AdminWebappConstants.TRUST_POINT_SESSION_ATTRIBUTE,
						selectedTrustPoint);
		try {
			FacesContext.getCurrentInstance().getExternalContext().redirect(
					"trust-point.seam");
		} catch (IOException e) {
			this.log.error("IO Exception: " + e.getMessage(), e);
			return;
		}
	}

	/**
	 * {@link TrustPointEntity} richfaces upload component listener
	 * 
	 * @param event
	 * @throws IOException
	 */
	public void uploadListener(UploadEvent event) throws IOException {
		UploadItem item = event.getUploadItem();
		this.log.debug(item.getContentType());
		this.log.debug(item.getFileSize());
		this.log.debug(item.getFileName());
		byte[] certBytes;
		if (null == item.getData()) {
			// meaning createTempFiles is set to true in the SeamFilter
			certBytes = FileUtils.readFileToByteArray(item.getFile());
		} else {
			certBytes = item.getData();
		}
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
				.put(AdminWebappConstants.CERTIFICATE_SESSION_ATTRIBUTE,
						certBytes);
	}
}