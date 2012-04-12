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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.bouncycastle.crypto.io.MacOutputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.pircbotx.Channel;
import org.pircbotx.DccChat;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

public class ClientBot extends AbstractBot implements WorkListener {

	private final Main main;

	private final CertificateRepository certificateRepository;

	private User controlUser;

	private final List<TestResult> testResults;

	public ClientBot(String secret, Main main) throws Exception {
		super(secret, "bt");
		this.main = main;

		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.certificateRepository = new CertificateRepository();
		this.testResults = new LinkedList<TestResult>();
	}

	@Override
	public void onMessage(MessageEvent<PircBotX> event) {
		try {
			String message = event.getMessage();
			String sender = event.getUser().getNick();
			Channel channel = event.getChannel();
			System.out.println(sender + ": " + message);
			if (message.startsWith("HELLO ")) {
				String challenge = message.substring("HELLO ".length());
				System.out.println("challenge: " + challenge);

				checkNonce(challenge);

				String nonce = UUID.randomUUID().toString();
				String toBeSigned = "HI " + challenge + nonce;
				String signature = sign(toBeSigned);
				this.pircBotX.sendMessage(event.getUser(), "HI " + nonce + " "
						+ signature);
			} else if (message.startsWith("TEST ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				int requestsPerSecond = scanner.nextInt();
				int maxWorkers = scanner.nextInt();
				int totalTimeMillis = scanner.nextInt();
				boolean sameSerialNumber = scanner.nextBoolean();
				String nonce = scanner.next();
				String actualSignature = scanner.next();
				System.out.println("Request for testing");
				System.out.println("Requests per second: " + requestsPerSecond);
				System.out.println("Max workers: " + maxWorkers);
				System.out.println("Total time millis: " + totalTimeMillis);
				System.out.println("Same serial number: " + sameSerialNumber);

				checkNonce(nonce);

				String toBeSigned = "TEST " + requestsPerSecond + " "
						+ maxWorkers + " " + totalTimeMillis + " "
						+ sameSerialNumber + " " + nonce;

				checkSignature(toBeSigned, actualSignature, event.getUser());

				System.out.println("Ready to run test...");
				this.controlUser = event.getUser();
				this.pircBotX.sendMessage(channel, "STARTING");
				this.certificateRepository.init(sameSerialNumber);
				this.testResults.clear();
				this.main
						.runTest(requestsPerSecond, maxWorkers,
								totalTimeMillis, this.certificateRepository,
								null, this);
			} else if (message.startsWith("KILL ")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				String nonce = scanner.next();

				checkNonce(nonce);

				String actualSignature = scanner.next();
				String toBeSigned = "KILL " + nonce;
				checkSignature(toBeSigned, actualSignature, event.getUser());

				System.out.println("VALID SUICIDE MESSAGE RECEIVED!");
				this.pircBotX.sendMessage(channel, "SUICIDE");
				// TODO: more gentle way to stop?
				System.exit(1);
			} else if (message.startsWith("GET_RESULTS")) {
				Scanner scanner = new Scanner(message);
				scanner.useDelimiter(" ");
				scanner.next();
				String nonce = scanner.next();

				checkNonce(nonce);

				String actualSignature = scanner.next();
				String toBeSigned = "GET_RESULTS " + nonce;
				checkSignature(toBeSigned, actualSignature, event.getUser());

				File tmpFile = File.createTempFile("ocsp-perf-test-result-",
						".txt");
				tmpFile.deleteOnExit();
				FileOutputStream fileOutputStream = new FileOutputStream(
						tmpFile);
				HMac hMac = getHMac();
				MacOutputStream macOutputStream = new MacOutputStream(
						fileOutputStream, hMac);
				PrintWriter printWriter = new PrintWriter(macOutputStream);
				int idx = 0;
				for (TestResult testResult : this.testResults) {
					printWriter.println(idx + "," + testResult.getWorkerCount()
							+ "," + testResult.getCurrentRequestCount() + ","
							+ testResult.getCurrentRequestMillis());
					idx++;
				}
				printWriter.close();
				byte[] signatureData = new byte[20];
				macOutputStream.getMac().doFinal(signatureData, 0);
				String signature = new String(Hex.encode(signatureData));

				this.pircBotX.dccSendFile(tmpFile, event.getUser(),
						1000 * 60 * 2);
				tmpFile.delete();
				DccChat dccChat = this.pircBotX.dccSendChatRequest(
						event.getUser(), 1000 * 10);
				dccChat.sendLine("RESULTS_INTEGRITY " + signature);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void done() {
		this.pircBotX.sendMessage(Main.IRC_CHANNEL, "DONE");
	}

	@Override
	public void result(int intervalCounter, int workerCount,
			int currentRequestCount, int currentRequestMillis) {
		String message = "RESULT " + intervalCounter + " " + workerCount + " "
				+ currentRequestCount + " " + currentRequestMillis;
		TestResult testResult = new TestResult(workerCount,
				currentRequestCount, currentRequestMillis);
		this.testResults.add(testResult);
		// pircbot is queing, so minimal impact on timer here
		this.pircBotX.sendMessage(this.controlUser, message);
		// IRC server might be throttling this, so don't rely on it
	}
}
