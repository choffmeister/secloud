build:
	sbt pack

clean:
	sbt clean

test:
	sbt scct:test printCoverage
