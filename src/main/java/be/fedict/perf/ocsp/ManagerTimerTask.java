/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.entity.ByteArrayEntity;

public class ManagerTimerTask extends TimerTask {

	private final int requestsPerSecond;

	private final int maxWorkers;

	private final long endTimeMillis;

	private int currentRequestCount;

	private int currentRequestMillis;

	private int workerCount;

	private final CertificateRepository certificateRepository;

	private final NetworkConfig networkConfig;

	private ThreadGroup workerThreadGroup;

	private List<WorkerThread> workerThreads;

	private boolean running;

	private WorkListener workListener;

	private int intervalCounter;

	private final Timer timer;

	public ManagerTimerTask(Timer timer, int requestsPerSecond, int maxWorkers,
			long totalTimeMillis, CertificateRepository certificateRepository,
			NetworkConfig networkConfig) {
		this.timer = timer;
		this.requestsPerSecond = requestsPerSecond;
		this.maxWorkers = maxWorkers;
		long beginTimeMillis = System.currentTimeMillis();
		this.endTimeMillis = beginTimeMillis + totalTimeMillis;
		this.certificateRepository = certificateRepository;
		this.networkConfig = networkConfig;
		System.out.println("INTERVAL, WORKER COUNT, REQUEST COUNT, AVERAGE DT");
		System.out.println("-------------------------------------------------");
		this.workerThreads = new LinkedList<WorkerThread>();
		this.running = true;
		this.workerThreadGroup = new ThreadGroup("worker-thread-group");
	}

	public void registerWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	private void notifyWorkListenersDone() {
		if (null == this.workListener) {
			return;
		}
		this.workListener.done();
	}

	@Override
	public synchronized void run() {
		if (this.running) {
			System.out
					.println(this.intervalCounter
							+ ","
							+ this.workerCount
							+ ","
							+ this.currentRequestCount
							+ ","
							+ (this.currentRequestCount != 0 ? (double) this.currentRequestMillis
									/ this.currentRequestCount
									: 0));
			if (null != this.workListener) {
				this.workListener.result(intervalCounter, workerCount,
						currentRequestCount, currentRequestMillis);
			}
			this.intervalCounter++;
		}

		long currentTimeMillis = System.currentTimeMillis();
		if (this.endTimeMillis <= currentTimeMillis) {
			if (this.running) {
				System.out.println("Ending tests...");
			}
			this.running = false;

			this.currentRequestCount = 0;
			this.currentRequestMillis = 0;
			notifyAll();

			boolean oneAlive = false;
			for (WorkerThread workerThread : this.workerThreads) {
				oneAlive |= workerThread.isAlive();
			}
			if (false == oneAlive) {
				notifyWorkListenersDone();
				super.cancel();
				this.timer.cancel();
			}
			return;
		}

		if (this.currentRequestCount < this.requestsPerSecond) {
			if (this.workerCount < this.maxWorkers) {
				WorkerThread workerThread = new WorkerThread(
						this.workerThreadGroup, this.workerCount, this,
						this.networkConfig);
				workerThread.start();
				this.workerThreads.add(workerThread);
				this.workerCount++;
			}
		}

		this.currentRequestCount = 0;
		this.currentRequestMillis = 0;
		notifyAll();
	}

	public synchronized ByteArrayEntity reportWork(long millis)
			throws StopWorkException {
		if (false == this.running) {
			// expensive, but fired only once
			throw new StopWorkException();
		}
		if (this.currentRequestCount >= this.requestsPerSecond) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException("wait error: " + e.getMessage(), e);
			}
		}
		this.currentRequestCount++;
		this.currentRequestMillis += millis;
		return this.certificateRepository.getOCSPRequest();
	}

	public synchronized ByteArrayEntity getOCSPRequest() {
		return this.certificateRepository.getOCSPRequest();
	}
}
