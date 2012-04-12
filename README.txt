OCSP Performance Test Project
=============================

Use tee to log while running a test:
java -jar ocsp-perf-test-1.0.0-SNAPSHOT.one-jar.jar 1 1 10 false | tee ocsp-perf.txt


=== Scenario 12/04/2012

Bots:
	e-contract.be
	isolde.eid.belgium.be
	eridu.apsu.be

Control:
	Frank Cornelis

TIME	REQ/SEC		DURATION(SECS)	REQ/SEC/BOT	THREADS/BOT
21h00	30		300		10		5
	45		300		15		5
21h15	75		600		25		6
21h30	90		600		30		7


=== Cheat sheet

scp target/ocsp-perf-test-0.2.0-SNAPSHOT.one-jar.jar e-contract.be:.
scp target/ocsp-perf-test-0.2.0-SNAPSHOT.one-jar.jar isolde.eid.belgium.be:.
scp target/ocsp-perf-test-0.2.0-SNAPSHOT.one-jar.jar eridu.apsu.be:.

ssh e-contract.be
screen
java -jar ocsp-perf-test-0.2.0-SNAPSHOT.one-jar.jar bot
Ctrl-A d

screen -ls
screen -r <screen process>

sudo modprobe nf_conntrack_irc
