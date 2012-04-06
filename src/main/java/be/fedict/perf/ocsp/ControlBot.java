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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.io.MacInputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.DccChat;
import org.pircbotx.DccFileTransfer;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.IncomingChatRequestEvent;
import org.pircbotx.hooks.events.IncomingFileTransferEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;

public class ControlBot extends ListenerAdapter<PircBotX> {

	private String challenge;

	private final String secret;

	private final Set<String> usedNonces;

	private final Map<String, TestResult[]> testResults;

	private final Map<String, File> receivedFiles;

	private final PircBotX pircBotX;

	public ControlBot(String secret) throws Exception {
		this.secret = secret;
		this.usedNonces = new HashSet<String>();
		this.testResults = new HashMap<String, TestResult[]>();
		this.receivedFiles = new HashMap<String, File>();

		String name = "ctrl-" + UUID.randomUUID().toString();
		System.out.println("bot name: " + name);
		this.pircBotX = new PircBotX();
		// this.pircBotX.setVerbose(true);
		this.pircBotX.setName(name);
		this.pircBotX.getListenerManager().addListener(this);
		this.pircBotX.connect(Main.IRC_SERVER);
		this.pircBotX.joinChannel(Main.IRC_CHANNEL);
	}

	@Override
	public void onConnect(ConnectEvent<PircBotX> event) throws Exception {
		System.out.println("Connected to IRC");
	}

	@Override
	public void onJoin(JoinEvent<PircBotX> event) throws Exception {
		System.out.println("Joining: " + event.getUser().getNick());
	}

	@Override
	public void onQuit(QuitEvent<PircBotX> event) throws Exception {
		System.out.println("Quits: " + event.getUser().getNick());
	}

	@Override
	public void onNotice(NoticeEvent<PircBotX> event) throws Exception {
		String notice = event.getNotice();
		System.out.println("NOTICE: " + notice);
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) {
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		System.out.println(sender + ": " + message);
	}

	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> event)
			throws Exception {
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		System.out.println(sender + ": " + message);
		try {
			if (message.startsWith("HI ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				String nonce = scanner.next();
				String signature = scanner.next();

				if (this.usedNonces.contains(nonce)) {
					throw new RuntimeException("nonce already used");
				}
				this.usedNonces.add(nonce);

				String toBeSigned = "HI " + this.challenge + nonce;
				Mac mac = Mac.getInstance("HmacSHA1");
				Key key = new SecretKeySpec(this.secret.getBytes(), 0,
						this.secret.getBytes().length, "HmacSHA1");
				mac.init(key);
				byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
				String expectedSignature = new String(Hex.encode(signatureData));
				if (signature.equals(expectedSignature)) {
					System.out.println("Trusted bot: " + sender);
					this.testResults.put(sender, null);
					// we can accept test results from trusted senders
				} else {
					System.err.println("UNTRUSTED BOT: " + sender);
				}
			} else if (message.startsWith("RESULT ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				int intervalCount = scanner.nextInt();
				int workerCount = scanner.nextInt();
				int currentRequestCount = scanner.nextInt();
				int currentRequestMillis = scanner.nextInt();
				// cannot rely on this result because of IRC server throttling
				// TestResult[] botTestResults = this.testResults.get(sender);
				// botTestResults[intervalCount] = new TestResult(workerCount,
				// currentRequestCount, currentRequestMillis);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onIncomingFileTransfer(IncomingFileTransferEvent<PircBotX> event)
			throws Exception {
		DccFileTransfer dccFileTransfer = event.getTransfer();
		String nick = dccFileTransfer.getUser().getNick();
		System.out.println("Incoming file transfer from: " + nick);
		File tmpFile = File.createTempFile("ocsp-perf-test-result-", ".txt");
		tmpFile.deleteOnExit();
		dccFileTransfer.receive(tmpFile, true);
		System.out.println("File received from: " + nick);
		System.out.println("Temporary file: " + tmpFile.getAbsolutePath());
		this.receivedFiles.put(nick, tmpFile);
	}

	@Override
	public void onIncomingChatRequest(IncomingChatRequestEvent<PircBotX> event)
			throws Exception {
		DccChat dccChat = event.getChat();
		dccChat.accept();
		String nick = dccChat.getUser().getNick();
		String message = dccChat.readLine();
		System.out.println(nick + ": " + message);
		if (message.startsWith("RESULTS_INTEGRITY")) {
			Scanner scanner = new Scanner(message);
			scanner.useDelimiter(" ");
			scanner.next();
			String actualSignature = scanner.next();
			File receivedFile = this.receivedFiles.get(nick);
			FileInputStream fileInputStream = new FileInputStream(receivedFile);
			HMac hMac = new HMac(new SHA1Digest());
			hMac.init(new KeyParameter(this.secret.getBytes()));
			MacInputStream macInputStream = new MacInputStream(fileInputStream,
					hMac);
			NullOutputStream nullOutputStream = new NullOutputStream();
			IOUtils.copy(macInputStream, nullOutputStream);
			macInputStream.close();
			byte[] expectedSignature = new byte[20];
			macInputStream.getMac().doFinal(expectedSignature, 0);
			if (false == Arrays.equals(expectedSignature,
					Hex.decode(actualSignature))) {
				System.err.println("invalid file signature");
			} else {
				System.out.println("File signature valid: "
						+ receivedFile.getAbsolutePath());
				TestResult[] botTestResults = this.testResults.get(nick);
				InputStream resultInputStream = new FileInputStream(
						receivedFile);
				Scanner resultScanner = new Scanner(resultInputStream);
				resultScanner.useDelimiter(",|\n");
				while (resultScanner.hasNextLine()) {
					int idx = resultScanner.nextInt();
					int workerCount = resultScanner.nextInt();
					int currentRequestCount = resultScanner.nextInt();
					int currentRequestMillis = resultScanner.nextInt();
					TestResult testResult = new TestResult(workerCount,
							currentRequestCount, currentRequestMillis);
					botTestResults[idx] = testResult;
				}
			}
		}
	}

	public void listBots() {
		this.challenge = UUID.randomUUID().toString();
		this.testResults.clear();
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, "HELLO " + this.challenge);
	}

	public Map<String, TestResult[]> getTestResults() {
		return this.testResults;
	}

	public void runTest(int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, boolean sameSerialNumber) throws Exception {
		// reset test results
		for (String trustedBot : this.testResults.keySet()) {
			TestResult[] botTestResults = new TestResult[(int) totalTimeMillis / 1000 + 1];
			this.testResults.put(trustedBot, botTestResults);
		}
		for (File receivedFile : this.receivedFiles.values()) {
			receivedFile.delete();
		}
		this.receivedFiles.clear();

		String nonce = UUID.randomUUID().toString();
		String message = "TEST " + requestsPerSecond + " " + maxWorkers + " "
				+ totalTimeMillis + " " + sameSerialNumber + " " + nonce;
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(message.getBytes());
		String signature = new String(Hex.encode(signatureData));
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, message + " " + signature);
	}

	public void killAllBots() throws Exception {
		String nonce = UUID.randomUUID().toString();
		String toBeSigned = "KILL " + nonce;
		Mac mac = Mac.getInstance("HmacSHA1");
		Key key = new SecretKeySpec(this.secret.getBytes(), 0,
				this.secret.getBytes().length, "HmacSHA1");
		mac.init(key);
		byte[] signatureData = mac.doFinal(toBeSigned.getBytes());
		String signature = new String(Hex.encode(signatureData));
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, toBeSigned + " "
				+ signature);
	}

	public void retrieveTestResults() {
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, "GET_RESULTS");
	}
}
