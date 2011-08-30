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

package be.fedict.trust.client;

import be.fedict.trust.CRLRevocationData;
import be.fedict.trust.OCSPRevocationData;
import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades132.*;
import be.fedict.trust.xkms2.XKMSConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TimeStampToken;

import javax.xml.ws.WebServiceException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.*;

/**
 * High Availability client component for the eID Trust Service XKMS2 web
 * service.
 * <p/>
 * In case the eID Trust Service is not available, it will fall back to the
 * specified jTrust {@link TrustValidator}.
 * 
 * @author wvdhaute
 */
public class HAXKMS2Client extends XKMS2Client {

	private static final Log LOG = LogFactory.getLog(HAXKMS2Client.class);

	private final TrustValidator trustValidator;

	/**
	 * Main constructor
	 * 
	 * @param location
	 *            location ( complete path ) of the XKMS2 web service
	 * @param trustValidator
	 *            Backup {@link TrustValidator} in case the XKMS2 service @
	 *            location is not available.
	 */
	public HAXKMS2Client(String location, TrustValidator trustValidator) {
		super(location);

		this.trustValidator = trustValidator;
	}

	@Override
	protected void validate(String trustDomain,
			List<X509Certificate> certificateChain,
			boolean returnRevocationData, Date validationDate,
			List<byte[]> ocspResponses, List<byte[]> crls,
			RevocationValuesType revocationValues,
			TimeStampToken timeStampToken,
			CertifiedRolesListType attributeCertificates)
			throws CertificateEncodingException, ValidationFailedException,
			TrustDomainNotFoundException, RevocationDataNotFoundException {

		try {
			super.validate(trustDomain, certificateChain, returnRevocationData,
					validationDate, ocspResponses, crls, revocationValues,
					timeStampToken, attributeCertificates);
		} catch (WebServiceException e) {

			// fallback to specified trust validator.
			fallbackValidate(certificateChain, validationDate, timeStampToken,
					attributeCertificates);
		}
	}

	@Override
	public RevocationValuesType getRevocationValues() {

		RevocationData revocationData = trustValidator.getRevocationData();
		if (null == revocationData) {
			return null;
		}

		be.fedict.trust.client.jaxb.xades132.ObjectFactory xadesObjectFactory = new be.fedict.trust.client.jaxb.xades132.ObjectFactory();
		RevocationValuesType revocationValues = xadesObjectFactory
				.createRevocationValuesType();

		/*
		 * Add OCSP responses
		 */
		OCSPValuesType ocspValues = xadesObjectFactory.createOCSPValuesType();
		for (OCSPRevocationData ocspRevocationData : revocationData
				.getOcspRevocationData()) {
			EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
					.createEncapsulatedPKIDataType();
			encapsulatedPKIData.setValue(ocspRevocationData.getData());
			ocspValues.getEncapsulatedOCSPValue().add(encapsulatedPKIData);
		}
		revocationValues.setOCSPValues(ocspValues);

		/*
		 * Add CRL's
		 */
		CRLValuesType crlValues = xadesObjectFactory.createCRLValuesType();
		for (CRLRevocationData crlRevocationData : revocationData
				.getCrlRevocationData()) {
			EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
					.createEncapsulatedPKIDataType();
			encapsulatedPKIData.setValue(crlRevocationData.getData());
			crlValues.getEncapsulatedCRLValue().add(encapsulatedPKIData);
		}
		revocationValues.setCRLValues(crlValues);

		return revocationValues;
	}

	private void fallbackValidate(List<X509Certificate> certificateChain,
			Date validationDate, TimeStampToken timeStampToken,
			CertifiedRolesListType attributeCertificates)
			throws ValidationFailedException, CertificateEncodingException {

		LOG.debug("eID Trust Service not available, falling back to specified Trust Validator");

		try {
			if (null != timeStampToken) {
				/*
				 * Parse embedded certificate chain
				 */
				List<X509Certificate> tsCertificateChain = new LinkedList<X509Certificate>();
				CertStore certStore = timeStampToken.getCertificatesAndCRLs(
						"Collection", "BC");
				Collection<? extends Certificate> certificates = certStore
						.getCertificates(null);
				for (Certificate certificate : certificates) {
					tsCertificateChain.add((X509Certificate) certificate);
				}

				if (TrustValidator.isSelfSigned(tsCertificateChain.get(0))) {
					Collections.reverse(tsCertificateChain);
				}
				trustValidator.isTrusted(certificateChain);

			} else if (null != attributeCertificates) {

				List<byte[]> encodedAttributeCertificates = new LinkedList<byte[]>();
				for (EncapsulatedPKIDataType attributeCertificate : attributeCertificates
						.getCertifiedRole()) {
					encodedAttributeCertificates.add(attributeCertificate
							.getValue());
				}

				if (null != validationDate) {
					trustValidator.isTrusted(encodedAttributeCertificates,
							certificateChain, validationDate);
				} else {
					trustValidator.isTrusted(encodedAttributeCertificates,
							certificateChain);
				}
			} else {
				if (null != validationDate) {
					trustValidator.isTrusted(certificateChain, validationDate);
				} else {
					trustValidator.isTrusted(certificateChain);
				}
			}

		} catch (CertPathValidatorException e) {
			invalidReasonURIs
					.add(XKMSConstants.KEY_BINDING_REASON_ISSUER_TRUST_URI);
			throw new ValidationFailedException(
					Collections
							.singletonList(XKMSConstants.KEY_BINDING_REASON_ISSUER_TRUST_URI));
		} catch (CertStoreException e) {
			throw new CertificateEncodingException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CertificateEncodingException(e);
		} catch (CMSException e) {
			throw new CertificateEncodingException(e);
		} catch (NoSuchProviderException e) {
			throw new CertificateEncodingException(e);
		}

	}
}
