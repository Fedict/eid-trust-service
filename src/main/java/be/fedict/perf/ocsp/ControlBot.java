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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.bouncycastle.crypto.io.MacInputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.DccChat;
import org.pircbotx.DccFileTransfer;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.IncomingChatRequestEvent;
import org.pircbotx.hooks.events.IncomingFileTransferEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;

public class ControlBot extends AbstractBot {

	private String challenge;

	private final Map<String, TestResult[]> testResults;

	private final Map<String, File> receivedFiles;

	public ControlBot(String secret) throws Exception {
		super(secret, "ctrl");
		this.testResults = new HashMap<String, TestResult[]>();
		this.receivedFiles = new HashMap<String, File>();
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
				String actualSignature = scanner.next();

				checkNonce(nonce);

				String toBeSigned = "HI " + this.challenge + nonce;
				checkSignature(toBeSigned, actualSignature, event.getUser());
				System.out.println("Trusted bot: " + sender);
				this.testResults.put(sender, null);
				// we can accept test results from trusted senders
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
			HMac hMac = getHMac();
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
		if (this.testResults.isEmpty()) {
			System.err.println("No trusted bots available.");
		}
		for (String trustedBot : this.testResults.keySet()) {
			TestResult[] botTestResults = new TestResult[(int) totalTimeMillis / 1000 + 1];
			this.testResults.put(trustedBot, botTestResults);
		}
		for (File receivedFile : this.receivedFiles.values()) {
			receivedFile.delete();
		}
		this.receivedFiles.clear();

		String nonce = UUID.randomUUID().toString();
		String toBeSigned = "TEST " + requestsPerSecond + " " + maxWorkers
				+ " " + totalTimeMillis + " " + sameSerialNumber + " " + nonce;
		String signature = sign(toBeSigned);
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, toBeSigned + " "
				+ signature);
	}

	public void killAllBots() throws Exception {
		String nonce = UUID.randomUUID().toString();
		String toBeSigned = "KILL " + nonce;
		String signature = sign(toBeSigned);
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, toBeSigned + " "
				+ signature);
	}

	public void retrieveTestResults() throws Exception {
		String nonce = UUID.randomUUID().toString();
		String toBeSigned = "GET_RESULTS " + nonce;
		String signature = sign(toBeSigned);
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, toBeSigned + " "
				+ signature);
	}
}
