/*
 * eID Trust Service Project.
 * Copyright (C) 2012 Frank Cornelis.
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

package be.fedict.perf.ocsp;

import java.security.Key;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.NoticeEvent;

public abstract class AbstractBot extends ListenerAdapter<PircBotX> {

	protected final PircBotX pircBotX;

	private final String secret;

	private final Set<String> usedNonces;

	public AbstractBot(String secret, String namePrefix) throws Exception {
		this.secret = secret;
		this.usedNonces = new HashSet<String>();
		this.pircBotX = new PircBotX();
		//this.pircBotX.setVerbose(true);
		String name = namePrefix + "-" + UUID.randomUUID().toString();
		System.out.println("bot name: " + name);
		this.pircBotX.setName(name);
		this.pircBotX.connect(Main.IRC_SERVER);
		this.pircBotX.joinChannel(Main.IRC_CHANNEL);
		this.pircBotX.getListenerManager().addListener(this);
	}

	@Override
	public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
		System.out.println("Connected to IRC");
	}
	
	@Override
	public void onNotice(NoticeEvent<PircBotX> event) throws Exception {
		String notice = event.getNotice();
		System.out.println("NOTICE: " + notice);
	}

	protected void checkNonce(String nonce) {
		if (this.usedNonces.contains(nonce)) {
			throw new RuntimeException("nonce already used");
		}
		this.usedNonces.add(nonce);
	}

	protected String sign(String toBeSigned) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
		String signature = new String(Hex.encode(signatureData));
		return signature;
	}

	protected void checkSignature(String toBeSigned, String actualSignature,
			User user) throws Exception {
		String expectedSignature = sign(toBeSigned);
		if (false == actualSignature.equals(expectedSignature)) {
			throw new RuntimeException("invalid signature by " + user.getNick());
		}
	}

	protected HMac getHMac() throws Exception {
		HMac hMac = new HMac(new SHA1Digest());
		hMac.init(new KeyParameter(this.secret.getBytes()));
		return hMac;
	}
}
